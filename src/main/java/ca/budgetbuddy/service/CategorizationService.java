package ca.budgetbuddy.service;

import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.CategoryRule;
import ca.budgetbuddy.model.CategorySource;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.persistence.CategoryRuleRepository;
import ca.budgetbuddy.persistence.TransactionRepository;

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

    /**
     * Sets a transaction's category as a user correction.
     * If {@code remember} is true, saves a merchant rule and applies it to
     * other transactions from that merchant that were not set by the user.
     */
    public Transaction correctCategory(long txId, Category category, boolean remember) {
        Transaction tx = transactionRepository.findById(txId);
        if (tx == null) {
            throw new IllegalArgumentException("Transaction not found: " + txId);
        }
        tx.setCategory(category, CategorySource.USER, 1.0);
        transactionRepository.update(tx);
        if (remember) {
            rememberMerchant(tx, category);
        }
        return tx;
    }

    private void rememberMerchant(Transaction corrected, Category category) {
        ruleRepository.save(new CategoryRule(corrected.getMerchant(), category));
        List<Transaction> sameMerchant = transactionRepository.findByMerchant(corrected.getMerchant());
        for (Transaction other : sameMerchant) {
            if (other.getId() == corrected.getId()) {
                continue;
            }
            if (other.getSource() == CategorySource.USER) {
                continue;
            }
            other.setCategory(category, CategorySource.RULE, 1.0);
            transactionRepository.update(other);
        }
    }
}
