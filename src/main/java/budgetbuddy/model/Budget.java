package budgetbuddy.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Objects;

/**
 * A monthly spending limit for one category.
 * {@code spent} is calculated by BudgetService and is not persisted.
 */
public class Budget {
    private long id;
    private Category category;
    private BigDecimal limit;
    private YearMonth month;
    private BigDecimal spent;
    private int lastAlertPercent;

    public Budget(Category category, BigDecimal limit, YearMonth month) {
        this(0, category, limit, month, BigDecimal.ZERO, 0);
    }

    public Budget(long id, Category category, BigDecimal limit, YearMonth month,
                  BigDecimal spent, int lastAlertPercent) {
        this.id = id;
        this.category = Objects.requireNonNull(category, "category");
        this.limit = Objects.requireNonNull(limit, "limit");
        this.month = Objects.requireNonNull(month, "month");
        this.spent = spent == null ? BigDecimal.ZERO : spent;
        this.lastAlertPercent = lastAlertPercent;
    }

    /**
     * Percent of the limit that has been used. Returns 0 if the limit is not positive.
     */
    public double percentUsed() {
        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return spent.divide(limit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public void setLimit(BigDecimal limit) {
        this.limit = Objects.requireNonNull(limit, "limit");
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getSpent() {
        return spent;
    }

    public void setSpent(BigDecimal spent) {
        this.spent = spent == null ? BigDecimal.ZERO : spent;
    }

    public int getLastAlertPercent() {
        return lastAlertPercent;
    }

    public void setLastAlertPercent(int lastAlertPercent) {
        this.lastAlertPercent = lastAlertPercent;
    }
}
