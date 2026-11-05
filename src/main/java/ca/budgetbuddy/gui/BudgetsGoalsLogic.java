package ca.budgetbuddy.gui;

import ca.budgetbuddy.model.SavingsGoal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Form parsing and labels for the Budgets and Goals tab.
 */
public final class BudgetsGoalsLogic {
    private BudgetsGoalsLogic() {
    }

    public static YearMonth parseMonth(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Month is required (yyyy-MM).");
        }
        try {
            return YearMonth.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Month must be yyyy-MM.");
        }
    }

    public static BigDecimal parseAmount(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Amount is required.");
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Amount must be a number.");
        }
    }

    public static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Deadline is required (yyyy-MM-dd).");
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Deadline must be yyyy-MM-dd.");
        }
    }

    public static String goalLabel(SavingsGoal goal) {
        return goal.getId() + " " + goal.getName();
    }

    public static String savedBudgetMessage(YearMonth month, String category) {
        return "Saved budget for " + month + " " + category + ".";
    }

    public static String savedGoalMessage(SavingsGoal goal) {
        return "Saved goal " + goal.getId() + " " + goal.getName() + ".";
    }

    public static String contributionMessage(SavingsGoal goal) {
        return "Contribution saved. " + goal.getName() + " now has " + goal.getSaved().toPlainString() + ".";
    }
}
