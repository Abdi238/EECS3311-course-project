package ca.budgetbuddy.persistence;

import ca.budgetbuddy.model.Budget;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.CategoryRule;
import ca.budgetbuddy.model.CategorySource;
import ca.budgetbuddy.model.SavingsGoal;
import ca.budgetbuddy.model.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteRepositoryTest {

    private Database database;
    private SqliteTransactionRepository transactions;
    private SqliteCategoryRuleRepository rules;
    private SqliteBudgetRepository budgets;
    private SqliteGoalRepository goals;

    @BeforeEach
    void setUp() {
        database = Database.inMemory();
        transactions = new SqliteTransactionRepository(database);
        rules = new SqliteCategoryRuleRepository(database);
        budgets = new SqliteBudgetRepository(database);
        goals = new SqliteGoalRepository(database);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void saveAllAssignsIdsAndFindsByRangeAndMerchant() {
        Transaction groceries = new Transaction(LocalDate.of(2026, 10, 3), "Loblaws", new BigDecimal("54.21"));
        groceries.setCategory(Category.GROCERIES, CategorySource.RULE, 1.0);
        Transaction coffee = new Transaction(LocalDate.of(2026, 10, 4), "Starbucks", new BigDecimal("6.25"));
        Transaction outside = new Transaction(LocalDate.of(2026, 9, 1), "Loblaws", new BigDecimal("12.00"));

        transactions.saveAll(List.of(groceries, coffee, outside));

        assertTrue(groceries.getId() > 0);
        assertTrue(coffee.getId() > 0);

        List<Transaction> october = transactions.findBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertEquals(2, october.size());
        assertEquals("Loblaws", october.get(0).getMerchant());
        assertEquals(Category.GROCERIES, october.get(0).getCategory());

        List<Transaction> loblaws = transactions.findByMerchant("loblaws");
        assertEquals(2, loblaws.size());
    }

    @Test
    void existsMatchesDateMerchantAndAmount() {
        Transaction original = new Transaction(LocalDate.of(2026, 10, 3), "Loblaws", new BigDecimal("54.21"));
        transactions.saveAll(List.of(original));

        Transaction duplicate = new Transaction(LocalDate.of(2026, 10, 3), "Loblaws", new BigDecimal("54.21"));
        Transaction differentAmount = new Transaction(LocalDate.of(2026, 10, 3), "Loblaws", new BigDecimal("10.00"));

        assertTrue(transactions.exists(duplicate));
        assertFalse(transactions.exists(differentAmount));
    }

    @Test
    void updatePersistsCategoryChange() {
        Transaction tx = new Transaction(LocalDate.of(2026, 10, 8), "Uber", new BigDecimal("18.40"));
        transactions.saveAll(List.of(tx));
        tx.setCategory(Category.TRANSPORT, CategorySource.USER, 1.0);

        transactions.update(tx);

        Transaction loaded = transactions.findById(tx.getId());
        assertNotNull(loaded);
        assertEquals(Category.TRANSPORT, loaded.getCategory());
        assertEquals(CategorySource.USER, loaded.getSource());
        assertEquals(1.0, loaded.getConfidence());
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        assertNull(transactions.findById(999));
    }

    @Test
    void categoryRulesCanBeSavedAndReplaced() {
        rules.save(new CategoryRule("Starbucks", Category.DINING));
        rules.save(new CategoryRule("Loblaws", Category.GROCERIES));
        rules.save(new CategoryRule("Starbucks", Category.OTHER));

        List<CategoryRule> all = rules.findAll();
        assertEquals(2, all.size());
        CategoryRule starbucks = all.stream()
                .filter(rule -> rule.getMerchant().equals("Starbucks"))
                .findFirst()
                .orElseThrow();
        assertEquals(Category.OTHER, starbucks.getCategory());
    }

    @Test
    void budgetSaveDoesNotPersistSpentButDoesPersistAlertPercent() {
        Budget budget = new Budget(Category.DINING, new BigDecimal("300.00"), YearMonth.of(2026, 10));
        budget.setSpent(new BigDecimal("250.00"));
        budget.setLastAlertPercent(80);

        Budget saved = budgets.save(budget);
        assertTrue(saved.getId() > 0);

        Budget loaded = budgets.findByMonth(YearMonth.of(2026, 10)).get(0);
        assertEquals(new BigDecimal("300.00"), loaded.getLimit());
        assertEquals(0, loaded.getSpent().compareTo(BigDecimal.ZERO));
        assertEquals(80, loaded.getLastAlertPercent());
        assertEquals(Category.DINING, loaded.getCategory());
    }

    @Test
    void budgetUpdateAndDelete() {
        Budget budget = budgets.save(new Budget(Category.RENT, new BigDecimal("1400.00"), YearMonth.of(2026, 10)));
        budget.setLimit(new BigDecimal("1500.00"));
        budgets.save(budget);

        Budget updated = budgets.findByMonth(YearMonth.of(2026, 10)).get(0);
        assertEquals(new BigDecimal("1500.00"), updated.getLimit());

        budgets.delete(updated.getId());
        assertTrue(budgets.findByMonth(YearMonth.of(2026, 10)).isEmpty());
    }

    @Test
    void findByMonthDoesNotReturnOtherMonths() {
        budgets.save(new Budget(Category.UTILITIES, new BigDecimal("120.00"), YearMonth.of(2026, 10)));
        budgets.save(new Budget(Category.UTILITIES, new BigDecimal("120.00"), YearMonth.of(2026, 11)));

        assertEquals(1, budgets.findByMonth(YearMonth.of(2026, 10)).size());
        assertEquals(YearMonth.of(2026, 10), budgets.findByMonth(YearMonth.of(2026, 10)).get(0).getMonth());
    }

    @Test
    void goalSaveAssignsIdAndRoundTripsFields() {
        SavingsGoal goal = new SavingsGoal("Trip", new BigDecimal("2000.00"), LocalDate.of(2027, 4, 30));
        goal.setSaved(new BigDecimal("250.00"));

        SavingsGoal saved = goals.save(goal);
        assertTrue(saved.getId() > 0);

        SavingsGoal loaded = goals.findById(saved.getId());
        assertNotNull(loaded);
        assertEquals("Trip", loaded.getName());
        assertEquals(new BigDecimal("2000.00"), loaded.getTarget());
        assertEquals(new BigDecimal("250.00"), loaded.getSaved());
        assertEquals(LocalDate.of(2027, 4, 30), loaded.getDeadline());
    }

    @Test
    void goalUpdatePersistsContribution() {
        SavingsGoal goal = goals.save(new SavingsGoal("Emergency", new BigDecimal("1000.00"),
                LocalDate.of(2027, 12, 31)));
        goal.setSaved(new BigDecimal("100.00"));
        goals.save(goal);

        assertEquals(new BigDecimal("100.00"), goals.findById(goal.getId()).getSaved());
    }

    @Test
    void findGoalByIdReturnsNullWhenMissing() {
        assertNull(goals.findById(42));
    }
}
