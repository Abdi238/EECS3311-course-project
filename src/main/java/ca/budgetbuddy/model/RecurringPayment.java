package ca.budgetbuddy.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A charge that repeats on a regular schedule. Detected in Java; the LLM
 * fills in {@code displayName} and {@code subscription}.
 */
public class RecurringPayment {
    private final String merchant;
    private String displayName;
    private final BigDecimal amount;
    private boolean subscription;

    public RecurringPayment(String merchant, BigDecimal amount) {
        this(merchant, merchant, amount, false);
    }

    public RecurringPayment(String merchant, String displayName, BigDecimal amount, boolean subscription) {
        this.merchant = Objects.requireNonNull(merchant, "merchant");
        this.displayName = displayName == null ? merchant : displayName;
        this.amount = Objects.requireNonNull(amount, "amount");
        this.subscription = subscription;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isSubscription() {
        return subscription;
    }

    public void setSubscription(boolean subscription) {
        this.subscription = subscription;
    }
}
