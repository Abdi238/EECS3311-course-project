package ca.budgetbuddy.gui;

import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.MonthlySummary;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Display text for insights. No detection rules and no JavaFX.
 */
public final class InsightsLogic {
    private InsightsLogic() {
    }

    public static String trendText(MonthlySummary summary) {
        return summary.getMonth()
                + " income " + summary.getIncome().toPlainString()
                + " spent " + summary.getSpent().toPlainString();
    }

    public static String summaryText(MonthlySummary summary) {
        StringBuilder text = new StringBuilder();
        text.append(summary.getMonth())
                .append(" income ").append(summary.getIncome().toPlainString())
                .append(" spent ").append(summary.getSpent().toPlainString());
        for (Map.Entry<Category, BigDecimal> entry : summary.getCategoryTotals().entrySet()) {
            text.append('\n').append(entry.getKey().name()).append(' ').append(entry.getValue().toPlainString());
        }
        if (!summary.getBudgetResults().isBlank()) {
            text.append("\nBudgets:\n").append(summary.getBudgetResults());
        }
        if (summary.getRecap() != null && !summary.getRecap().isBlank()) {
            text.append('\n').append(summary.getRecap());
        }
        return text.toString();
    }
}
