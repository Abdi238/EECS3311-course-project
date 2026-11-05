package ca.budgetbuddy.gui;

import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.MonthlySummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightsLogicTest {

    @Test
    void trendTextShowsIncomeAndSpending() {
        MonthlySummary summary = new MonthlySummary(
                YearMonth.of(2026, 10),
                new BigDecimal("0.00"),
                new BigDecimal("90.51"),
                Map.of(Category.UNCATEGORIZED, new BigDecimal("90.51")));

        assertEquals("2026-10 income 0.00 spent 90.51", InsightsLogic.trendText(summary));
    }

    @Test
    void summaryTextIncludesStatsBudgetsAndRecap() {
        MonthlySummary summary = new MonthlySummary(
                YearMonth.of(2026, 10),
                new BigDecimal("200.00"),
                new BigDecimal("50.00"),
                Map.of(Category.GROCERIES, new BigDecimal("50.00")));
        summary.setBudgetResults("GROCERIES limit 100.00 spent 50.00");
        summary.setRecap("Three tips for the fake grocery ledger.");

        String text = InsightsLogic.summaryText(summary);

        assertTrue(text.contains("2026-10 income 200.00 spent 50.00"));
        assertTrue(text.contains("GROCERIES 50.00"));
        assertTrue(text.contains("GROCERIES limit 100.00 spent 50.00"));
        assertTrue(text.contains("Three tips for the fake grocery ledger."));
    }
}
