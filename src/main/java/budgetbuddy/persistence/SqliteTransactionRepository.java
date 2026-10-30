package budgetbuddy.persistence;

import budgetbuddy.model.Category;
import budgetbuddy.model.CategorySource;
import budgetbuddy.model.Transaction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class SqliteTransactionRepository implements TransactionRepository {
    private static final String INSERT = """
            INSERT INTO transactions (date, merchant, amount, category, source, confidence)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE = """
            UPDATE transactions
            SET date = ?, merchant = ?, amount = ?, category = ?, source = ?, confidence = ?
            WHERE id = ?
            """;
    private static final String FIND_BY_ID = "SELECT * FROM transactions WHERE id = ?";
    private static final String FIND_BETWEEN = """
            SELECT * FROM transactions
            WHERE date >= ? AND date <= ?
            ORDER BY date, id
            """;
    private static final String FIND_BY_MERCHANT = """
            SELECT * FROM transactions
            WHERE LOWER(merchant) = LOWER(?)
            ORDER BY date, id
            """;
    private static final String EXISTS = """
            SELECT 1 FROM transactions
            WHERE date = ? AND merchant = ? AND amount = ?
            LIMIT 1
            """;

    private final Database database;

    public SqliteTransactionRepository(Database database) {
        this.database = database;
    }

    @Override
    public void saveAll(List<Transaction> txs) {
        if (txs == null || txs.isEmpty()) {
            return;
        }
        try {
            var connection = database.connection();
            boolean previous = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                for (Transaction tx : txs) {
                    bindTransaction(statement, tx);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            tx.setId(keys.getLong(1));
                        }
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previous);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not save transactions", e);
        }
    }

    @Override
    public void update(Transaction tx) {
        try (PreparedStatement statement = database.connection().prepareStatement(UPDATE)) {
            bindTransaction(statement, tx);
            statement.setLong(7, tx.getId());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new PersistenceException("No transaction with id " + tx.getId());
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not update transaction " + tx.getId(), e);
        }
    }

    @Override
    public Transaction findById(long id) {
        try (PreparedStatement statement = database.connection().prepareStatement(FIND_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not find transaction " + id, e);
        }
    }

    @Override
    public List<Transaction> findBetween(LocalDate from, LocalDate to) {
        try (PreparedStatement statement = database.connection().prepareStatement(FIND_BETWEEN)) {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
            return list(statement);
        } catch (SQLException e) {
            throw new PersistenceException("Could not find transactions between " + from + " and " + to, e);
        }
    }

    @Override
    public List<Transaction> findByMerchant(String merchant) {
        try (PreparedStatement statement = database.connection().prepareStatement(FIND_BY_MERCHANT)) {
            statement.setString(1, merchant);
            return list(statement);
        } catch (SQLException e) {
            throw new PersistenceException("Could not find transactions for merchant " + merchant, e);
        }
    }

    @Override
    public boolean exists(Transaction tx) {
        try (PreparedStatement statement = database.connection().prepareStatement(EXISTS)) {
            statement.setString(1, tx.getDate().toString());
            statement.setString(2, tx.getMerchant());
            statement.setString(3, tx.getAmount().toPlainString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not check whether transaction exists", e);
        }
    }

    private static void bindTransaction(PreparedStatement statement, Transaction tx) throws SQLException {
        statement.setString(1, tx.getDate().toString());
        statement.setString(2, tx.getMerchant());
        statement.setString(3, tx.getAmount().toPlainString());
        statement.setString(4, tx.getCategory().name());
        statement.setString(5, tx.getSource().name());
        statement.setDouble(6, tx.getConfidence());
    }

    private static List<Transaction> list(PreparedStatement statement) throws SQLException {
        List<Transaction> result = new ArrayList<>();
        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    private static Transaction map(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getLong("id"),
                LocalDate.parse(rs.getString("date")),
                rs.getString("merchant"),
                new BigDecimal(rs.getString("amount")),
                Category.valueOf(rs.getString("category")),
                CategorySource.valueOf(rs.getString("source")),
                rs.getDouble("confidence")
        );
    }
}
