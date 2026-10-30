package budgetbuddy.model;

import java.util.Objects;

/**
 * A transaction flagged as unusual. Java fills in {@code reason};
 * the LLM fills in {@code explanation}.
 */
public class Anomaly {
    private final Transaction transaction;
    private final String reason;
    private String explanation;

    public Anomaly(Transaction transaction, String reason) {
        this(transaction, reason, null);
    }

    public Anomaly(Transaction transaction, String reason, String explanation) {
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.explanation = explanation;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public String getReason() {
        return reason;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
