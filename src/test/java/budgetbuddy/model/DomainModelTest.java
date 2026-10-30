package budgetbuddy.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelTest {

    @Test
    void setCategoryUpdatesSourceAndConfidence() {
        Transaction tx = new Transaction(LocalDate.of(2026, 10, 1), "Loblaws", new BigDecimal("42.50"));
        tx.setCategory(Category.GROCERIES, CategorySource.RULE, 1.0);

        assertEquals(Category.GROCERIES, tx.getCategory());
        assertEquals(CategorySource.RULE, tx.getSource());
        assertEquals(1.0, tx.getConfidence());
    }

    @Test
    void categoryRuleMatchesMerchantIgnoringCase() {
        CategoryRule rule = new CategoryRule("Starbucks", Category.DINING);
        Transaction match = new Transaction(LocalDate.of(2026, 10, 2), "STARBUCKS", new BigDecimal("6.25"));
        Transaction other = new Transaction(LocalDate.of(2026, 10, 2), "Tim Hortons", new BigDecimal("4.15"));

        assertTrue(rule.matches(match));
        assertFalse(rule.matches(other));
    }

    @Test
    void budgetPercentUsedIsSpentOverLimit() {
        Budget budget = new Budget(Category.DINING, new BigDecimal("200.00"), YearMonth.of(2026, 10));
        budget.setSpent(new BigDecimal("50.00"));

        assertEquals(25.0, budget.percentUsed(), 0.0001);
    }

    @Test
    void budgetPercentUsedIsZeroWhenLimitIsZero() {
        Budget budget = new Budget(Category.DINING, BigDecimal.ZERO, YearMonth.of(2026, 10));
        budget.setSpent(new BigDecimal("50.00"));

        assertEquals(0.0, budget.percentUsed(), 0.0001);
    }

    @Test
    void savingsGoalRequiredPerMonthDividesRemainingByMonthsLeft() {
        LocalDate deadline = LocalDate.now().plusMonths(4);
        SavingsGoal goal = new SavingsGoal("Trip", new BigDecimal("1200.00"), deadline);

        assertEquals(new BigDecimal("300.00"), goal.requiredPerMonth());
        assertFalse(goal.isComplete());
    }

    @Test
    void savingsGoalIsCompleteWhenSavedMeetsTarget() {
        SavingsGoal goal = new SavingsGoal(1, "Emergency", new BigDecimal("500.00"),
                new BigDecimal("500.00"), LocalDate.now().plusMonths(6));

        assertTrue(goal.isComplete());
        assertEquals(new BigDecimal("0.00"), goal.requiredPerMonth());
    }

    @Test
    void monthlySummaryKeepsCategoryTotals() {
        MonthlySummary summary = new MonthlySummary(
                YearMonth.of(2026, 10),
                new BigDecimal("3000.00"),
                new BigDecimal("400.00"),
                Map.of(Category.GROCERIES, new BigDecimal("250.00"), Category.DINING, new BigDecimal("150.00"))
        );
        summary.setRecap("You spent most on groceries.");

        assertEquals(new BigDecimal("250.00"), summary.getCategoryTotals().get(Category.GROCERIES));
        assertEquals("You spent most on groceries.", summary.getRecap());
    }

    @Test
    void recurringPaymentAndAnomalyHoldMutableLlmFields() {
        RecurringPayment payment = new RecurringPayment("NETFLIX.COM", new BigDecimal("18.99"));
        payment.setDisplayName("Netflix");
        payment.setSubscription(true);

        Transaction tx = new Transaction(LocalDate.of(2026, 10, 5), "Best Buy", new BigDecimal("640.00"));
        Anomaly anomaly = new Anomaly(tx, "4x category average");
        anomaly.setExplanation("This $640 at Best Buy is about 4 times your usual Shopping purchase.");

        assertTrue(payment.isSubscription());
        assertEquals("Netflix", payment.getDisplayName());
        assertEquals(tx, anomaly.getTransaction());
        assertEquals("4x category average", anomaly.getReason());
    }
}
