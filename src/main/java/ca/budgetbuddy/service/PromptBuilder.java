package ca.budgetbuddy.service;

import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.CategoryRule;
import ca.budgetbuddy.model.Transaction;

import java.util.List;

/**
 * Builds fixed prompts. The model is asked for JSON and is not given tools here.
 */
public final class PromptBuilder {
    private PromptBuilder() {
    }

    public static String categorizePrompt(List<Transaction> transactions, List<CategoryRule> rules) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Categorize each transaction. Reply with a JSON array only. ");
        prompt.append("Each object has index, category, and confidence between 0 and 1. ");
        prompt.append("Allowed categories:");
        for (Category category : Category.values()) {
            if (category == Category.UNCATEGORIZED) {
                continue;
            }
            prompt.append(' ').append(category.name());
        }
        prompt.append(". Saved merchant rules as examples:");
        if (rules == null || rules.isEmpty()) {
            prompt.append(" none.");
        } else {
            for (CategoryRule rule : rules) {
                prompt.append(" \"").append(rule.getMerchant())
                        .append("\" -> ").append(rule.getCategory().name()).append('.');
            }
        }
        prompt.append(" Transactions:");
        int index = 0;
        for (Transaction tx : transactions) {
            prompt.append(' ').append(index)
                    .append(". ").append(tx.getDate())
                    .append(' ').append(tx.getMerchant())
                    .append(' ').append(tx.getAmount().toPlainString()).append('.');
            index++;
        }
        return prompt.toString();
    }
}
