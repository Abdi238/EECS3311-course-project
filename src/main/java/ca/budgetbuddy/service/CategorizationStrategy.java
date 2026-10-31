package ca.budgetbuddy.service;

import budgetbuddy.model.CategoryRule;
import budgetbuddy.model.Transaction;

import java.util.List;

/**
 * A way to assign categories to transactions.
 */
public interface CategorizationStrategy {
    void categorize(List<Transaction> txs, List<CategoryRule> rules);
}
