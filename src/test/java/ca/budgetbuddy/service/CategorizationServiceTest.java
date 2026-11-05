package ca.budgetbuddy.service;

import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.CategoryRule;
import ca.budgetbuddy.model.CategorySource;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.persistence.Database;
import ca.budgetbuddy.persistence.SqliteCategoryRuleRepository;
import ca.budgetbuddy.persistence.SqliteTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategorizationServiceTest {

    private Database database;
    private SqliteTransactionRepository transactions;
    private SqliteCategoryRuleRepository rules;
    private CategorizationService service;

    @BeforeEach
    void setUp() {
        database = Database.inMemory();
        transactions = new SqliteTransactionRepository(database);
        rules = new SqliteCategoryRuleRepository(database);
        service = new CategorizationService(rules, transactions);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void ruleBasedStrategyAppliesMatchingMerchantRule() {
        RuleBasedStrategy strategy = new RuleBasedStrategy();
        Transaction groceries = new Transaction(LocalDate.of(2026, 10, 1), "Sample Grocery Store",
                new BigDecimal("54.21"));
        Transaction coffee = new Transaction(LocalDate.of(2026, 10, 2), "Campus Coffee Shop",
                new BigDecimal("6.25"));
        List<CategoryRule> savedRules = List.of(
                new CategoryRule("Sample Grocery Store", Category.GROCERIES)
        );

        strategy.categorize(List.of(groceries, coffee), savedRules);

        assertEquals(Category.GROCERIES, groceries.getCategory());
        assertEquals(CategorySource.RULE, groceries.getSource());
        assertEquals(1.0, groceries.getConfidence());
        assertEquals(Category.UNCATEGORIZED, coffee.getCategory());
        assertEquals(CategorySource.NONE, coffee.getSource());
    }

    @Test
    void ruleBasedStrategyMatchesMerchantIgnoringCase() {
        RuleBasedStrategy strategy = new RuleBasedStrategy();
        Transaction tx = new Transaction(LocalDate.of(2026, 10, 2), "CAMPUS COFFEE SHOP",
                new BigDecimal("6.25"));

        strategy.categorize(List.of(tx), List.of(new CategoryRule("Campus Coffee Shop", Category.DINING)));

        assertEquals(Category.DINING, tx.getCategory());
        assertEquals(CategorySource.RULE, tx.getSource());
    }

    @Test
    void ruleBasedStrategyDoesNotOverwriteUserCategory() {
        RuleBasedStrategy strategy = new RuleBasedStrategy();
        Transaction tx = new Transaction(LocalDate.of(2026, 10, 3), "City Transit", new BigDecimal("3.30"));
        tx.setCategory(Category.OTHER, CategorySource.USER, 1.0);

        strategy.categorize(List.of(tx), List.of(new CategoryRule("City Transit", Category.TRANSPORT)));

        assertEquals(Category.OTHER, tx.getCategory());
        assertEquals(CategorySource.USER, tx.getSource());
    }

    @Test
    void categorizationServiceLoadsRulesFromTheRepository() {
        rules.save(new CategoryRule("Sample Grocery Store", Category.GROCERIES));
        Transaction tx = new Transaction(LocalDate.of(2026, 10, 1), "Sample Grocery Store",
                new BigDecimal("54.21"));

        service.categorize(List.of(tx));

        assertEquals(Category.GROCERIES, tx.getCategory());
        assertEquals(CategorySource.RULE, tx.getSource());
    }

    @Test
    void categorizeUncategorizedUpdatesSavedTransactions() {
        rules.save(new CategoryRule("Campus Coffee Shop", Category.DINING));
        Transaction uncategorized = new Transaction(LocalDate.of(2026, 10, 2), "Campus Coffee Shop",
                new BigDecimal("6.25"));
        Transaction alreadySet = new Transaction(LocalDate.of(2026, 10, 3), "City Transit",
                new BigDecimal("3.30"));
        alreadySet.setCategory(Category.TRANSPORT, CategorySource.USER, 1.0);
        transactions.saveAll(List.of(uncategorized, alreadySet));

        int updated = service.categorizeUncategorized();

        assertEquals(1, updated);
        Transaction loadedCoffee = transactions.findById(uncategorized.getId());
        assertEquals(Category.DINING, loadedCoffee.getCategory());
        assertEquals(CategorySource.RULE, loadedCoffee.getSource());
        Transaction loadedTransit = transactions.findById(alreadySet.getId());
        assertEquals(Category.TRANSPORT, loadedTransit.getCategory());
        assertEquals(CategorySource.USER, loadedTransit.getSource());
    }

    @Test
    void correctCategorySetsUserSourceWithoutSavingARule() {
        Transaction tx = new Transaction(LocalDate.of(2026, 10, 1), "Sample Grocery Store",
                new BigDecimal("54.21"));
        tx.setCategory(Category.UNCATEGORIZED, CategorySource.NONE, 0.0);
        transactions.saveAll(List.of(tx));

        Transaction updated = service.correctCategory(tx.getId(), Category.GROCERIES, false);

        assertEquals(Category.GROCERIES, updated.getCategory());
        assertEquals(CategorySource.USER, updated.getSource());
        assertEquals(1.0, updated.getConfidence());
        assertTrue(rules.findAll().isEmpty());
        Transaction loaded = transactions.findById(tx.getId());
        assertEquals(Category.GROCERIES, loaded.getCategory());
        assertEquals(CategorySource.USER, loaded.getSource());
    }

    @Test
    void correctCategoryRememberSavesRuleAndAppliesToOtherMerchantRows() {
        Transaction first = new Transaction(LocalDate.of(2026, 10, 1), "Campus Coffee Shop",
                new BigDecimal("6.25"));
        Transaction second = new Transaction(LocalDate.of(2026, 10, 8), "Campus Coffee Shop",
                new BigDecimal("7.10"));
        Transaction userLocked = new Transaction(LocalDate.of(2026, 10, 15), "Campus Coffee Shop",
                new BigDecimal("4.50"));
        Transaction otherMerchant = new Transaction(LocalDate.of(2026, 10, 2), "Sample Grocery Store",
                new BigDecimal("54.21"));
        second.setCategory(Category.OTHER, CategorySource.AI, 0.6);
        userLocked.setCategory(Category.SHOPPING, CategorySource.USER, 1.0);
        transactions.saveAll(List.of(first, second, userLocked, otherMerchant));

        service.correctCategory(first.getId(), Category.DINING, true);

        List<CategoryRule> savedRules = rules.findAll();
        assertEquals(1, savedRules.size());
        assertEquals("Campus Coffee Shop", savedRules.get(0).getMerchant());
        assertEquals(Category.DINING, savedRules.get(0).getCategory());

        Transaction loadedFirst = transactions.findById(first.getId());
        assertEquals(Category.DINING, loadedFirst.getCategory());
        assertEquals(CategorySource.USER, loadedFirst.getSource());

        Transaction loadedSecond = transactions.findById(second.getId());
        assertEquals(Category.DINING, loadedSecond.getCategory());
        assertEquals(CategorySource.RULE, loadedSecond.getSource());

        Transaction loadedUser = transactions.findById(userLocked.getId());
        assertEquals(Category.SHOPPING, loadedUser.getCategory());
        assertEquals(CategorySource.USER, loadedUser.getSource());

        Transaction loadedGrocery = transactions.findById(otherMerchant.getId());
        assertEquals(Category.UNCATEGORIZED, loadedGrocery.getCategory());
    }

    @Test
    void correctCategoryRememberReplacesExistingRuleForMerchant() {
        rules.save(new CategoryRule("City Transit", Category.OTHER));
        Transaction tx = new Transaction(LocalDate.of(2026, 10, 3), "City Transit", new BigDecimal("3.30"));
        transactions.saveAll(List.of(tx));

        service.correctCategory(tx.getId(), Category.TRANSPORT, true);

        List<CategoryRule> savedRules = rules.findAll();
        assertEquals(1, savedRules.size());
        assertEquals(Category.TRANSPORT, savedRules.get(0).getCategory());
    }

    @Test
    void correctCategoryThrowsWhenTransactionIsMissing() {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> service.correctCategory(999, Category.DINING, false)
        );
        assertTrue(thrown.getMessage().contains("Transaction not found"));
    }

    @Test
    void setStrategyReplacesTheCurrentStrategy() {
        RecordingStrategy recording = new RecordingStrategy();
        service.setStrategy(recording);
        List<Transaction> txs = new ArrayList<>();
        txs.add(new Transaction(LocalDate.of(2026, 10, 1), "Sample Grocery Store", new BigDecimal("54.21")));

        service.categorize(txs);

        assertSame(txs, recording.lastTransactions);
        assertEquals(0, recording.lastRules.size());
    }

    private static final class RecordingStrategy implements CategorizationStrategy {
        private List<Transaction> lastTransactions;
        private List<CategoryRule> lastRules;

        @Override
        public void categorize(List<Transaction> txs, List<CategoryRule> rules) {
            lastTransactions = txs;
            lastRules = rules;
        }
    }
}
