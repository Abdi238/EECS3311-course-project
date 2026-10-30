package budgetbuddy.persistence;

import budgetbuddy.model.Budget;
import budgetbuddy.model.Category;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class SqliteBudgetRepository implements BudgetRepository {
    private static final String INSERT = """
            INSERT INTO budgets (category, limit_amount, month, last_alert_percent)
            VALUES (?, ?, ?, ?)
            """;
    private static final String UPDATE = """
            UPDATE budgets
            SET category = ?, limit_amount = ?, month = ?, last_alert_percent = ?
            WHERE id = ?
            """;
    private static final String DELETE = "DELETE FROM budgets WHERE id = ?";
    private static final String FIND_BY_MONTH = """
            SELECT id, category, limit_amount, month, last_alert_percent
            FROM budgets
            WHERE month = ?
            ORDER BY category
            """;

    private final Database database;

    public SqliteBudgetRepository(Database database) {
        this.database = database;
    }

    @Override
    public Budget save(Budget budget) {
        if (budget.getId() > 0) {
            update(budget);
            return budget;
        }
        try (PreparedStatement statement = database.connection()
                .prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bindBudget(statement, budget);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    budget.setId(keys.getLong(1));
                }
            }
            return budget;
        } catch (SQLException e) {
            throw new PersistenceException("Could not save budget", e);
        }
    }

    @Override
    public void delete(long budgetId) {
        try (PreparedStatement statement = database.connection().prepareStatement(DELETE)) {
            statement.setLong(1, budgetId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Could not delete budget " + budgetId, e);
        }
    }

    @Override
    public List<Budget> findByMonth(YearMonth month) {
        try (PreparedStatement statement = database.connection().prepareStatement(FIND_BY_MONTH)) {
            statement.setString(1, month.toString());
            List<Budget> budgets = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    budgets.add(map(rs));
                }
            }
            return budgets;
        } catch (SQLException e) {
            throw new PersistenceException("Could not load budgets for " + month, e);
        }
    }

    private void update(Budget budget) {
        try (PreparedStatement statement = database.connection().prepareStatement(UPDATE)) {
            bindBudget(statement, budget);
            statement.setLong(5, budget.getId());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new PersistenceException("No budget with id " + budget.getId());
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not update budget " + budget.getId(), e);
        }
    }

    private static void bindBudget(PreparedStatement statement, Budget budget) throws SQLException {
        statement.setString(1, budget.getCategory().name());
        statement.setString(2, budget.getLimit().toPlainString());
        statement.setString(3, budget.getMonth().toString());
        statement.setInt(4, budget.getLastAlertPercent());
    }

    private static Budget map(ResultSet rs) throws SQLException {
        return new Budget(
                rs.getLong("id"),
                Category.valueOf(rs.getString("category")),
                new BigDecimal(rs.getString("limit_amount")),
                YearMonth.parse(rs.getString("month")),
                BigDecimal.ZERO,
                rs.getInt("last_alert_percent")
        );
    }
}
