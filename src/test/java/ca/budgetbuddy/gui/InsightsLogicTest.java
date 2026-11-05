package ca.budgetbuddy.gui;

import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.MonthlySummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
