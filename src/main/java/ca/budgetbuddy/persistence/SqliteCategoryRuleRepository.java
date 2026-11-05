package ca.budgetbuddy.persistence;

import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.CategoryRule;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqliteCategoryRuleRepository implements CategoryRuleRepository {
    private static final String FIND_ALL = "SELECT merchant, category FROM category_rules ORDER BY merchant";
    private static final String UPSERT = """
            INSERT INTO category_rules (merchant, category)
            VALUES (?, ?)
            ON CONFLICT(merchant) DO UPDATE SET category = excluded.category
            """;

    private final Database database;

    public SqliteCategoryRuleRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<CategoryRule> findAll() {
        try (PreparedStatement statement = database.connection().prepareStatement(FIND_ALL);
             ResultSet rs = statement.executeQuery()) {
            List<CategoryRule> rules = new ArrayList<>();
            while (rs.next()) {
                rules.add(new CategoryRule(
                        rs.getString("merchant"),
                        Category.valueOf(rs.getString("category"))
                ));
            }
            return rules;
        } catch (SQLException e) {
            throw new PersistenceException("Could not load category rules", e);
        }
    }

    @Override
    public void save(CategoryRule rule) {
        try (PreparedStatement statement = database.connection().prepareStatement(UPSERT)) {
            statement.setString(1, rule.getMerchant());
            statement.setString(2, rule.getCategory().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Could not save category rule for " + rule.getMerchant(), e);
        }
    }
}
