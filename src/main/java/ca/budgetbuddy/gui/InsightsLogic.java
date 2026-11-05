package ca.budgetbuddy.gui;

import ca.budgetbuddy.model.MonthlySummary;

/**
 * Display text for one month of trends.
 */
public final class InsightsLogic {
    private InsightsLogic() {
    }

    public static String trendText(MonthlySummary summary) {
        return summary.getMonth()
                + " income " + summary.getIncome().toPlainString()
                + " spent " + summary.getSpent().toPlainString();
    }
}
