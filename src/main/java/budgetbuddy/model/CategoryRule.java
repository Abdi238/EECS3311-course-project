package budgetbuddy.model;

import java.util.Objects;

/**
 * Maps a merchant name to a category. Used by rule-based categorization
 * and as examples for the LLM.
 */
public class CategoryRule {
    private final String merchant;
    private final Category category;

    public CategoryRule(String merchant, Category category) {
        this.merchant = Objects.requireNonNull(merchant, "merchant");
        this.category = Objects.requireNonNull(category, "category");
    }

    public boolean matches(Transaction tx) {
        return tx != null
                && tx.getMerchant() != null
                && tx.getMerchant().equalsIgnoreCase(merchant);
    }

    public String getMerchant() {
        return merchant;
    }

    public Category getCategory() {
        return category;
    }
}
