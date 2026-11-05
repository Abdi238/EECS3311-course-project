package ca.budgetbuddy.service;

import ca.budgetbuddy.model.CategoryRule;
import ca.budgetbuddy.model.CategorySource;
import ca.budgetbuddy.model.Transaction;

import java.util.List;

/**
 * Applies saved merchant rules. Never overwrites a category the user set.
 */
public class RuleBasedStrategy implements CategorizationStrategy {
    @Override
    public void categorize(List<Transaction> txs, List<CategoryRule> rules) {
        if (txs == null || rules == null) {
            return;
        }
        for (Transaction tx : txs) {
            if (tx.getSource() == CategorySource.USER) {
                continue;
            }
            CategoryRule match = matchingRule(tx, rules);
            if (match != null) {
                tx.setCategory(match.getCategory(), CategorySource.RULE, 1.0);
            }
        }
    }

    private static CategoryRule matchingRule(Transaction tx, List<CategoryRule> rules) {
        for (CategoryRule rule : rules) {
            if (rule.matches(tx)) {
                return rule;
            }
        }
        return null;
    }
}
