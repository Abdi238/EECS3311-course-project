package ca.budgetbuddy.service;

import budgetbuddy.model.Category;
import budgetbuddy.model.CategoryRule;
import budgetbuddy.model.CategorySource;
import budgetbuddy.model.Transaction;
import budgetbuddy.persistence.Database;
import budgetbuddy.persistence.SqliteCategoryRuleRepository;
import budgetbuddy.persistence.SqliteTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
