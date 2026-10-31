package ca.budgetbuddy.service;

import budgetbuddy.model.Category;
import budgetbuddy.model.CategoryRule;
import budgetbuddy.model.Transaction;
import budgetbuddy.persistence.CategoryRuleRepository;
import budgetbuddy.persistence.TransactionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loads saved rules and runs the current categorization strategy.
 */
public class CategorizationService {
    private static final LocalDate RANGE_START = LocalDate.of(1900, 1, 1);
    private static final LocalDate RANGE_END = LocalDate.of(9999, 12, 31);

    private final CategoryRuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private CategorizationStrategy strategy;

    public CategorizationService(CategoryRuleRepository ruleRepository,
                                 TransactionRepository transactionRepository) {
        this(ruleRepository, transactionRepository, new RuleBasedStrategy());
    }

    public CategorizationService(CategoryRuleRepository ruleRepository,
                                 TransactionRepository transactionRepository,
                                 CategorizationStrategy strategy) {
        this.ruleRepository = Objects.requireNonNull(ruleRepository, "ruleRepository");
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository");
        setStrategy(strategy);
    }

    public void setStrategy(CategorizationStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public void categorize(List<Transaction> txs) {
        strategy.categorize(txs, ruleRepository.findAll());
    }

    /**
     * Recategorizes uncategorized transactions already in the database.
     * Returns how many received a category.
     */
    public int categorizeUncategorized() {
        List<Transaction> all = transactionRepository.findBetween(RANGE_START, RANGE_END);
        List<Transaction> uncategorized = new ArrayList<>();
        for (Transaction tx : all) {
            if (tx.getCategory() == Category.UNCATEGORIZED) {
                uncategorized.add(tx);
            }
        }
        categorize(uncategorized);
        int updated = 0;
        for (Transaction tx : uncategorized) {
            if (tx.getCategory() != Category.UNCATEGORIZED) {
                transactionRepository.update(tx);
                updated++;
            }
        }
        return updated;
    }
}
