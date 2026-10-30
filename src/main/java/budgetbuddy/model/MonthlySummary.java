package budgetbuddy.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Totals for one month. {@code recap} is written by the LLM from these numbers.
 */
public class MonthlySummary {
    private final YearMonth month;
    private final BigDecimal income;
    private final BigDecimal spent;
    private final Map<Category, BigDecimal> categoryTotals;
    private String recap;

    public MonthlySummary(YearMonth month, BigDecimal income, BigDecimal spent,
                          Map<Category, BigDecimal> categoryTotals) {
        this.month = Objects.requireNonNull(month, "month");
        this.income = income == null ? BigDecimal.ZERO : income;
        this.spent = spent == null ? BigDecimal.ZERO : spent;
        this.categoryTotals = new EnumMap<>(Category.class);
        if (categoryTotals != null) {
            this.categoryTotals.putAll(categoryTotals);
        }
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public BigDecimal getSpent() {
        return spent;
    }

    public Map<Category, BigDecimal> getCategoryTotals() {
        return Map.copyOf(categoryTotals);
    }

    public String getRecap() {
        return recap;
    }

    public void setRecap(String recap) {
        this.recap = recap;
    }
}
