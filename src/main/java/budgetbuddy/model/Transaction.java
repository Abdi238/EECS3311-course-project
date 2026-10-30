package budgetbuddy.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A single imported bank transaction.
 */
public class Transaction {
    private long id;
    private LocalDate date;
    private String merchant;
    private BigDecimal amount;
    private Category category;
    private CategorySource source;
    private double confidence;

    public Transaction(LocalDate date, String merchant, BigDecimal amount) {
        this(0, date, merchant, amount, Category.UNCATEGORIZED, CategorySource.NONE, 0.0);
    }

    public Transaction(long id, LocalDate date, String merchant, BigDecimal amount,
                       Category category, CategorySource source, double confidence) {
        this.id = id;
        this.date = Objects.requireNonNull(date, "date");
        this.merchant = Objects.requireNonNull(merchant, "merchant");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.category = category == null ? Category.UNCATEGORIZED : category;
        this.source = source == null ? CategorySource.NONE : source;
        this.confidence = confidence;
    }

    public void setCategory(Category category, CategorySource source, double confidence) {
        this.category = Objects.requireNonNull(category, "category");
        this.source = Objects.requireNonNull(source, "source");
        this.confidence = confidence;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getMerchant() {
        return merchant;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Category getCategory() {
        return category;
    }

    public CategorySource getSource() {
        return source;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "Transaction{id=" + id + ", date=" + date + ", merchant='" + merchant
                + "', amount=" + amount + ", category=" + category + "}";
    }
}
