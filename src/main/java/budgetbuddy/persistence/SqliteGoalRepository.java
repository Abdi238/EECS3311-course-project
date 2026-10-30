package budgetbuddy.persistence;

import budgetbuddy.model.SavingsGoal;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class SqliteGoalRepository implements GoalRepository {
    private static final String INSERT = """
            INSERT INTO savings_goals (name, target, saved, deadline)
            VALUES (?, ?, ?, ?)
            """;
    private static final String UPDATE = """
            UPDATE savings_goals
            SET name = ?, target = ?, saved = ?, deadline = ?
            WHERE id = ?
            """;
    private static final String FIND_BY_ID = """
            SELECT id, name, target, saved, deadline
            FROM savings_goals
            WHERE id = ?
            """;

    private final Database database;

    public SqliteGoalRepository(Database database) {
        this.database = database;
    }

    @Override
    public SavingsGoal save(SavingsGoal goal) {
        if (goal.getId() > 0) {
            update(goal);
            return goal;
        }
        try (PreparedStatement statement = database.connection()
                .prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bindGoal(statement, goal);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    goal.setId(keys.getLong(1));
                }
            }
            return goal;
        } catch (SQLException e) {
            throw new PersistenceException("Could not save savings goal", e);
        }
    }

    @Override
    public SavingsGoal findById(long goalId) {
        try (PreparedStatement statement = database.connection().prepareStatement(FIND_BY_ID)) {
            statement.setLong(1, goalId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not find savings goal " + goalId, e);
        }
    }

    private void update(SavingsGoal goal) {
        try (PreparedStatement statement = database.connection().prepareStatement(UPDATE)) {
            bindGoal(statement, goal);
            statement.setLong(5, goal.getId());
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new PersistenceException("No savings goal with id " + goal.getId());
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not update savings goal " + goal.getId(), e);
        }
    }

    private static void bindGoal(PreparedStatement statement, SavingsGoal goal) throws SQLException {
        statement.setString(1, goal.getName());
        statement.setString(2, goal.getTarget().toPlainString());
        statement.setString(3, goal.getSaved().toPlainString());
        statement.setString(4, goal.getDeadline().toString());
    }

    private static SavingsGoal map(ResultSet rs) throws SQLException {
        return new SavingsGoal(
                rs.getLong("id"),
                rs.getString("name"),
                new BigDecimal(rs.getString("target")),
                new BigDecimal(rs.getString("saved")),
                LocalDate.parse(rs.getString("deadline"))
        );
    }
}
