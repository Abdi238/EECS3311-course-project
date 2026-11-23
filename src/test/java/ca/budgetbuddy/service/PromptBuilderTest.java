package ca.budgetbuddy.service;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    @Test
    void askPromptIncludesTodayAndMonthsWithData() {
        String prompt = PromptBuilder.askPrompt(
                YearMonth.of(2026, 11),
                List.of(YearMonth.of(2026, 8), YearMonth.of(2026, 10)));

        assertTrue(prompt.contains("Today's month is 2026-11."));
        assertTrue(prompt.contains("When the user names a month without a year, use that year."));
        assertTrue(prompt.contains("Months with spending data: 2026-08 2026-10."));
    }
}
