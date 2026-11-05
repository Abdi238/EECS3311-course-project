package ca.budgetbuddy.service;

import ca.budgetbuddy.model.Budget;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.persistence.BudgetRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Saves monthly limits and tells listeners when spending reaches a new alert level.
 * Listeners are observers. This class does not know whether the CLI or the GUI is running.
 */
public class BudgetService {
    private final BudgetRepository budgets;
    private final SpendingCalculator calculator;
    private final List<BudgetAlertListener> listeners = new ArrayList<>();

    public BudgetService(BudgetRepository budgets, SpendingCalculator calculator) {
        this.budgets = Objects.requireNonNull(budgets, "budgets");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
    }

    public void addListener(BudgetAlertListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public Budget saveBudget(Category category, BigDecimal limit, YearMonth month) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(month, "month");
        if (limit == null || limit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        Budget saved = null;
        for (Budget existing : budgets.findByMonth(month)) {
            if (existing.getCategory() == category) {
                existing.setLimit(limit);
                saved = budgets.save(existing);
            }
        }
        if (saved == null) {
            saved = budgets.save(new Budget(category, limit, month));
        }
        recalculate(month);
        for (Budget budget : getBudgets(month)) {
            if (budget.getId() == saved.getId()) {
                return budget;
            }
        }
        return saved;
    }

    public List<Budget> getBudgets(YearMonth month) {
        Objects.requireNonNull(month, "month");
        List<Budget> found = budgets.findByMonth(month);
        for (Budget budget : found) {
            budget.setSpent(calculator.totalFor(month, budget.getCategory()));
        }
        return found;
    }

    /**
     * Recomputes spent for each budget in the month and alerts if 80% or 100% is new.
     */
    public void recalculate(YearMonth month) {
        Objects.requireNonNull(month, "month");
        for (Budget budget : budgets.findByMonth(month)) {
            budget.setSpent(calculator.totalFor(month, budget.getCategory()));
            checkAlert(budget);
        }
    }

    public void recalculateFor(List<Transaction> txs) {
        if (txs == null) {
            return;
        }
        List<YearMonth> months = new ArrayList<>();
        for (Transaction tx : txs) {
            YearMonth month = YearMonth.from(tx.getDate());
            if (!months.contains(month)) {
                months.add(month);
            }
        }
        for (YearMonth month : months) {
            recalculate(month);
        }
    }

    private void checkAlert(Budget budget) {
        int level = alertLevel(budget.percentUsed());
        if (level <= budget.getLastAlertPercent()) {
            return;
        }
        budget.setLastAlertPercent(level);
        budgets.save(budget);
        notifyListeners(alertMessage(budget));
    }

    private void notifyListeners(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        List<BudgetAlertListener> snapshot = new ArrayList<>(listeners);
        for (BudgetAlertListener listener : snapshot) {
            listener.onBudgetAlert(message);
        }
    }

    private static int alertLevel(double percent) {
        if (percent >= 100.0) {
            return 100;
        }
        if (percent >= 80.0) {
            return 80;
        }
        return 0;
    }

    private static String alertMessage(Budget budget) {
        return categoryLabel(budget.getCategory()) + " budget at " + formatPercent(budget.percentUsed()) + "%";
    }

    private static String categoryLabel(Category category) {
        String lower = category.name().toLowerCase(Locale.ROOT);
        return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
    }

    private static String formatPercent(double percent) {
        long rounded = Math.round(percent);
        if (Math.abs(percent - rounded) < 0.05) {
            return Long.toString(rounded);
        }
        return String.format(Locale.ROOT, "%.1f", percent);
    }
}
