package budgetbuddy.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A savings target with an optional deadline.
 */
public class SavingsGoal {
    private long id;
    private String name;
    private BigDecimal target;
    private BigDecimal saved;
    private LocalDate deadline;

    public SavingsGoal(String name, BigDecimal target, LocalDate deadline) {
        this(0, name, target, BigDecimal.ZERO, deadline);
    }

    public SavingsGoal(long id, String name, BigDecimal target, BigDecimal saved, LocalDate deadline) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.target = Objects.requireNonNull(target, "target");
        this.saved = saved == null ? BigDecimal.ZERO : saved;
        this.deadline = Objects.requireNonNull(deadline, "deadline");
    }

    /**
     * Amount still needed each month to hit the target by the deadline.
     * Uses at least one month so a due-this-month goal is not divided by zero.
     */
    public BigDecimal requiredPerMonth() {
        BigDecimal remaining = target.subtract(saved);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long months = ChronoUnit.MONTHS.between(YearMonth.now(), YearMonth.from(deadline));
        if (months <= 0) {
            months = 1;
        }
        return remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }

    public boolean isComplete() {
        return saved.compareTo(target) >= 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTarget() {
        return target;
    }

    public BigDecimal getSaved() {
        return saved;
    }

    public void setSaved(BigDecimal saved) {
        this.saved = saved == null ? BigDecimal.ZERO : saved;
    }

    public LocalDate getDeadline() {
        return deadline;
    }
}
