package ca.budgetbuddy.gui;

import ca.budgetbuddy.model.SavingsGoal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BudgetsGoalsLogicTest {

    @Test
    void parsesBudgetAndGoalFormValues() {
        assertEquals(YearMonth.of(2026, 10), BudgetsGoalsLogic.parseMonth("2026-10"));
        assertEquals(0, new BigDecimal("100.00").compareTo(BudgetsGoalsLogic.parseAmount(" 100.00 ")));
        assertEquals(LocalDate.of(2026, 12, 31), BudgetsGoalsLogic.parseDate("2026-12-31"));
        assertThrows(IllegalArgumentException.class, () -> BudgetsGoalsLogic.parseMonth("October"));
    }

    @Test
    void labelsAndConfirmationsUseTheFakeGoal() {
        SavingsGoal goal = new SavingsGoal(3, "Emergency fund", new BigDecimal("500.00"),
                new BigDecimal("25.00"), LocalDate.of(2026, 12, 31));

        assertEquals("3 Emergency fund", BudgetsGoalsLogic.goalLabel(goal));
        assertEquals("Saved budget for 2026-10 DINING.", BudgetsGoalsLogic.savedBudgetMessage(YearMonth.of(2026, 10), "DINING"));
        assertEquals("Saved goal 3 Emergency fund.", BudgetsGoalsLogic.savedGoalMessage(goal));
        assertEquals("Contribution saved. Emergency fund now has 25.00.", BudgetsGoalsLogic.contributionMessage(goal));
    }
}
