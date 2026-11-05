package ca.budgetbuddy;

import ca.budgetbuddy.llm.LlmResponse;
import ca.budgetbuddy.llm.MockLlmClient;
import ca.budgetbuddy.model.Anomaly;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.CategorySource;
import ca.budgetbuddy.model.MonthlySummary;
import ca.budgetbuddy.model.RecurringPayment;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.persistence.Database;
import ca.budgetbuddy.persistence.SqliteTransactionRepository;
import ca.budgetbuddy.persistence.TransactionRepository;
import ca.budgetbuddy.service.AnalyticsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsFeaturesTest {
    private Database database;
    private TransactionRepository transactions;
    private BudgetBuddyFacade facade;

    @AfterEach
    void tearDown() {
        if (facade != null) {
            facade.close();
            return;
        }
        if (database != null) {
            database.close();
        }
    }

    @Test
    void recurringFindsMonthlyMerchantAndIgnoresOneOff() {
        open(null);
        LocalDate latest = LocalDate.now().withDayOfMonth(1);
        save(latest.minusMonths(2), "Stream Service", "15.99", Category.SUBSCRIPTIONS);
        save(latest.minusMonths(1), "Stream Service", "15.99", Category.SUBSCRIPTIONS);
        save(latest, "Stream Service", "15.99", Category.SUBSCRIPTIONS);
        save(LocalDate.now().minusDays(1), "Campus Coffee Shop", "6.25", Category.DINING);

        List<RecurringPayment> found = facade.findRecurringPayments();

        assertEquals(1, found.size());
        RecurringPayment payment = found.get(0);
        assertEquals("Stream Service", payment.getMerchant());
        assertEquals("Stream Service", payment.getDisplayName());
        assertEquals(0, new BigDecimal("15.99").compareTo(payment.getAmount()));
        assertEquals(AnalyticsService.MONTHLY, payment.getFrequency());
        assertEquals(latest.plusMonths(1), payment.getNextExpectedDate());
        assertEquals(false, payment.isSubscription());
        assertEquals("", facade.recurringDataWarning());
    }

    @Test
    void unusualFlagsFourTimesChargeAndUsesMockExplanation() {
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse("""
                [{"index": 0, "explanation": "This fake Campus Coffee Shop charge is about 4 times the usual amount."}]
                """, null, null));
        open(new MockLlmClient(script));
        save(LocalDate.of(2026, 7, 4), "Campus Coffee Shop", "10.00", Category.DINING);
        save(LocalDate.of(2026, 8, 4), "Campus Coffee Shop", "10.00", Category.DINING);
        save(LocalDate.of(2026, 9, 4), "Campus Coffee Shop", "10.00", Category.DINING);
        save(LocalDate.of(2026, 10, 2), "Campus Coffee Shop", "10.00", Category.DINING);
        save(LocalDate.of(2026, 10, 20), "Campus Coffee Shop", "40.00", Category.DINING);

        List<Anomaly> found = facade.findUnusualExpenses(YearMonth.of(2026, 10));

        assertEquals(1, found.size());
        assertEquals("Campus Coffee Shop", found.get(0).getTransaction().getMerchant());
        assertEquals(0, new BigDecimal("40.00").compareTo(found.get(0).getTransaction().getAmount()));
        assertEquals("4x category average", found.get(0).getReason());
        assertEquals(
                "This fake Campus Coffee Shop charge is about 4 times the usual amount.",
                found.get(0).getExplanation());
        assertTrue(script.isEmpty());
    }

    @Test
    void summaryReturnsStatsWhenLlmIsOffline() {
        open(null);
        save(LocalDate.of(2026, 10, 1), "Payroll Deposit", "200.00", Category.INCOME);
        save(LocalDate.of(2026, 10, 4), "Sample Grocery Store", "50.00", Category.GROCERIES);
        facade.saveBudget(Category.GROCERIES, new BigDecimal("100.00"), YearMonth.of(2026, 10));

        MonthlySummary summary = facade.generateMonthlySummary(YearMonth.of(2026, 10));

        assertEquals(YearMonth.of(2026, 10), summary.getMonth());
        assertEquals(0, new BigDecimal("200.00").compareTo(summary.getIncome()));
        assertEquals(0, new BigDecimal("50.00").compareTo(summary.getSpent()));
        assertEquals(0, new BigDecimal("50.00").compareTo(summary.getCategoryTotals().get(Category.GROCERIES)));
        assertNull(summary.getCategoryTotals().get(Category.INCOME));
        assertEquals("GROCERIES limit 100.00 spent 50.00", summary.getBudgetResults());
        assertNull(summary.getRecap());
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> facade.generateMonthlySummary(YearMonth.of(2026, 1)));
        assertEquals("No transactions for 2026-01.", error.getMessage());
    }

    @Test
    void offlineFallbacksLeaveRecurringUnlabeledUnusualReasonAndSummaryWithoutRecap() {
        open(null);
        YearMonth selected = YearMonth.from(LocalDate.now());
        save(selected.minusMonths(2).atDay(1), "Stream Service", "15.99", Category.SUBSCRIPTIONS);
        save(selected.minusMonths(1).atDay(1), "Stream Service", "15.99", Category.SUBSCRIPTIONS);
        save(selected.atDay(1), "Stream Service", "15.99", Category.SUBSCRIPTIONS);
        save(selected.minusMonths(1).atDay(1), "Sample Grocery Store", "10.00", Category.SHOPPING);
        save(selected.minusMonths(1).atDay(2), "Sample Grocery Store", "10.00", Category.SHOPPING);
        save(selected.minusMonths(1).atDay(3), "Sample Grocery Store", "10.00", Category.SHOPPING);
        save(LocalDate.now(), "City Electronics", "40.00", Category.SHOPPING);

        List<RecurringPayment> payments = facade.findRecurringPayments();
        assertEquals(1, payments.size());
        assertEquals("Stream Service", payments.get(0).getDisplayName());
        assertEquals(false, payments.get(0).isSubscription());

        List<Anomaly> anomalies = facade.findUnusualExpenses(selected);
        assertEquals(1, anomalies.size());
        assertEquals("4x category average", anomalies.get(0).getReason());
        assertEquals(anomalies.get(0).getReason(), anomalies.get(0).getExplanation());

        MonthlySummary summary = facade.generateMonthlySummary(selected);
        assertEquals(0, new BigDecimal("55.99").compareTo(summary.getSpent()));
        assertNull(summary.getRecap());
    }

    @Test
    void failedLlmCallUsesTheSameFallbacks() {
        open(new MockLlmClient(new ArrayDeque<>()));
        YearMonth selected = YearMonth.from(LocalDate.now());
        save(selected.minusMonths(2).atDay(1), "Stream Service", "15.99", Category.SUBSCRIPTIONS);
        save(selected.minusMonths(1).atDay(1), "Stream Service", "15.99", Category.SUBSCRIPTIONS);
        save(selected.atDay(1), "Stream Service", "15.99", Category.SUBSCRIPTIONS);
        save(selected.minusMonths(1).atDay(4), "Campus Coffee Shop", "10.00", Category.DINING);
        save(selected.minusMonths(1).atDay(5), "Campus Coffee Shop", "10.00", Category.DINING);
        save(selected.minusMonths(1).atDay(6), "Campus Coffee Shop", "10.00", Category.DINING);
        save(LocalDate.now(), "Campus Coffee Shop", "40.00", Category.DINING);

        RecurringPayment payment = facade.findRecurringPayments().get(0);
        assertEquals("Stream Service", payment.getDisplayName());
        assertEquals(false, payment.isSubscription());

        Anomaly anomaly = facade.findUnusualExpenses(selected).get(0);
        assertEquals(anomaly.getReason(), anomaly.getExplanation());

        assertNull(facade.generateMonthlySummary(selected).getRecap());
    }

    @Test
    void warnsWhenHistoryCoversFewerThanThreeMonths() {
        open(null);
        save(LocalDate.now(), "City Transit", "3.30", Category.TRANSPORT);

        assertTrue(facade.findRecurringPayments().isEmpty());
        assertEquals(AnalyticsService.SHORT_HISTORY_WARNING, facade.recurringDataWarning());
    }

    private void open(ca.budgetbuddy.llm.LLMClient client) {
        database = Database.inMemory();
        transactions = new SqliteTransactionRepository(database);
        facade = BudgetBuddyFacade.create(database, client);
    }

    private void save(LocalDate date, String merchant, String amount, Category category) {
        Transaction tx = new Transaction(date, merchant, new BigDecimal(amount));
        tx.setCategory(category, CategorySource.USER, 1.0);
        transactions.saveAll(List.of(tx));
    }
}
