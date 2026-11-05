package ca.budgetbuddy.service;

import ca.budgetbuddy.model.Anomaly;
import ca.budgetbuddy.model.Budget;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.MonthlySummary;
import ca.budgetbuddy.model.RecurringPayment;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.persistence.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Detects recurring charges and unusual expenses, and builds the numbers for a monthly summary.
 * The LLM is not called here.
 */
public final class AnalyticsService {
    public static final String WEEKLY = "WEEKLY";
    public static final String MONTHLY = "MONTHLY";
    public static final String YEARLY = "YEARLY";
    public static final String SHORT_HISTORY_WARNING =
            "Fewer than 3 months of data. Results may be incomplete.";
    public static final String NEW_MERCHANT_REASON = "New merchant over $200";

    private static final BigDecimal NEW_MERCHANT_AMOUNT = new BigDecimal("200");
    private static final BigDecimal THREE = new BigDecimal("3");
    private static final BigDecimal SIMILAR_FRACTION = new BigDecimal("0.20");

    private final TransactionRepository transactions;
    private final SpendingCalculator calculator;
    private final BudgetService budgets;
    private final LocalDate asOf;
    private String recurringWarning = "";

    public AnalyticsService(TransactionRepository transactions, SpendingCalculator calculator, BudgetService budgets) {
        this(transactions, calculator, budgets, LocalDate.now());
    }

    public AnalyticsService(TransactionRepository transactions, SpendingCalculator calculator,
                            BudgetService budgets, LocalDate asOf) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.budgets = Objects.requireNonNull(budgets, "budgets");
        this.asOf = Objects.requireNonNull(asOf, "asOf");
    }

    public String getRecurringWarning() {
        return recurringWarning;
    }

    /**
     * Merchants in the last six months with at least three similar charges on a weekly,
     * monthly, or yearly interval.
     */
    public List<RecurringPayment> detectRecurring() {
        LocalDate start = asOf.minusMonths(6);
        List<Transaction> window = transactions.findBetween(start, asOf);
        if (distinctMonths(window) < 3) {
            recurringWarning = SHORT_HISTORY_WARNING;
        } else {
            recurringWarning = "";
        }
        Map<String, List<Transaction>> grouped = new TreeMap<>();
        for (Transaction tx : window) {
            if (tx.getCategory() == Category.INCOME) {
                continue;
            }
            grouped.computeIfAbsent(tx.getMerchant(), key -> new ArrayList<>()).add(tx);
        }
        List<RecurringPayment> found = new ArrayList<>();
        for (List<Transaction> group : grouped.values()) {
            if (group.size() < 3) {
                continue;
            }
            List<BigDecimal> amounts = new ArrayList<>();
            for (Transaction tx : group) {
                amounts.add(tx.getAmount());
            }
            if (!similarAmounts(amounts)) {
                continue;
            }
            String frequency = frequencyOf(group);
            if (frequency == null) {
                continue;
            }
            Transaction latest = latest(group);
            LocalDate next = nextDate(latest.getDate(), frequency);
            found.add(new RecurringPayment(latest.getMerchant(), latest.getAmount(), frequency, next));
        }
        return found;
    }

    /**
     * Flags selected-month charges that are over three times the prior three-month category
     * average, or a new merchant over $200. With no baseline, only the new-merchant rule is used.
     */
    public List<Anomaly> detectUnusual(YearMonth month) {
        Objects.requireNonNull(month, "month");
        List<Transaction> baseline = new ArrayList<>();
        YearMonth cursor = month.minusMonths(3);
        YearMonth lastBaseline = month.minusMonths(1);
        while (!cursor.isAfter(lastBaseline)) {
            baseline.addAll(transactions.findBetween(cursor.atDay(1), cursor.atEndOfMonth()));
            cursor = cursor.plusMonths(1);
        }
        Map<Category, List<BigDecimal>> byCategory = new EnumMap<>(Category.class);
        for (Transaction tx : baseline) {
            if (tx.getCategory() == Category.INCOME) {
                continue;
            }
            byCategory.computeIfAbsent(tx.getCategory(), key -> new ArrayList<>()).add(tx.getAmount());
        }
        List<Anomaly> anomalies = new ArrayList<>();
        for (Transaction tx : transactions.findBetween(month.atDay(1), month.atEndOfMonth())) {
            if (tx.getCategory() == Category.INCOME) {
                continue;
            }
            String reason = unusualReason(tx, byCategory.get(tx.getCategory()), baseline);
            if (reason == null) {
                continue;
            }
            anomalies.add(new Anomaly(tx, reason));
        }
        return anomalies;
    }

    /**
     * @return null when the month has no transactions
     */
    public MonthlySummary buildMonthlyStats(YearMonth month) {
        Objects.requireNonNull(month, "month");
        List<Transaction> monthTx = transactions.findBetween(month.atDay(1), month.atEndOfMonth());
        if (monthTx.isEmpty()) {
            return null;
        }
        MonthlySummary summary = calculator.trends(month, month).get(0);
        summary.setBudgetResults(budgetText(month));
        return summary;
    }

    private String budgetText(YearMonth month) {
        StringBuilder text = new StringBuilder();
        for (Budget budget : budgets.getBudgets(month)) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(budget.getCategory().name())
                    .append(" limit ").append(budget.getLimit().toPlainString())
                    .append(" spent ").append(budget.getSpent().toPlainString());
        }
        return text.toString();
    }

    private static String unusualReason(Transaction tx, List<BigDecimal> baselineAmounts, List<Transaction> baseline) {
        if (baselineAmounts != null && !baselineAmounts.isEmpty()) {
            BigDecimal average = average(baselineAmounts);
            if (average.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cutoff = average.multiply(THREE);
                if (tx.getAmount().compareTo(cutoff) > 0) {
                    return multipleReason(tx.getAmount(), average);
                }
            }
        }
        if (tx.getAmount().compareTo(NEW_MERCHANT_AMOUNT) > 0 && !seenBefore(tx.getMerchant(), baseline)) {
            return NEW_MERCHANT_REASON;
        }
        return null;
    }

    private static String multipleReason(BigDecimal amount, BigDecimal average) {
        BigDecimal ratio = amount.divide(average, 1, RoundingMode.HALF_UP).stripTrailingZeros();
        return ratio.toPlainString() + "x category average";
    }

    private static boolean seenBefore(String merchant, List<Transaction> baseline) {
        for (Transaction tx : baseline) {
            if (tx.getMerchant() != null && tx.getMerchant().equalsIgnoreCase(merchant)) {
                return true;
            }
        }
        return false;
    }

    private static int distinctMonths(List<Transaction> txs) {
        List<YearMonth> months = new ArrayList<>();
        for (Transaction tx : txs) {
            YearMonth month = YearMonth.from(tx.getDate());
            if (!months.contains(month)) {
                months.add(month);
            }
        }
        return months.size();
    }

    private static boolean similarAmounts(List<BigDecimal> amounts) {
        BigDecimal average = average(amounts);
        if (average.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal tolerance = average.multiply(SIMILAR_FRACTION);
        for (BigDecimal amount : amounts) {
            if (amount.subtract(average).abs().compareTo(tolerance) > 0) {
                return false;
            }
        }
        return true;
    }

    private static BigDecimal average(List<BigDecimal> amounts) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal amount : amounts) {
            sum = sum.add(amount);
        }
        return sum.divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
    }

    private static String frequencyOf(List<Transaction> group) {
        List<LocalDate> dates = new ArrayList<>();
        for (Transaction tx : group) {
            dates.add(tx.getDate());
        }
        dates.sort(LocalDate::compareTo);
        String found = null;
        for (int i = 1; i < dates.size(); i++) {
            long days = ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i));
            String gap = classifyGap(days);
            if (gap == null) {
                return null;
            }
            if (found == null) {
                found = gap;
            } else if (!found.equals(gap)) {
                return null;
            }
        }
        return found;
    }

    private static String classifyGap(long days) {
        if (days >= 6 && days <= 8) {
            return WEEKLY;
        }
        if (days >= 27 && days <= 32) {
            return MONTHLY;
        }
        if (days >= 350 && days <= 380) {
            return YEARLY;
        }
        return null;
    }

    private static Transaction latest(List<Transaction> group) {
        Transaction latest = group.get(0);
        for (Transaction tx : group) {
            if (tx.getDate().isAfter(latest.getDate())) {
                latest = tx;
            } else if (tx.getDate().equals(latest.getDate()) && tx.getId() > latest.getId()) {
                latest = tx;
            }
        }
        return latest;
    }

    private static LocalDate nextDate(LocalDate latest, String frequency) {
        if (WEEKLY.equals(frequency)) {
            return latest.plusDays(7);
        }
        if (MONTHLY.equals(frequency)) {
            return latest.plusMonths(1);
        }
        if (YEARLY.equals(frequency)) {
            return latest.plusYears(1);
        }
        return latest;
    }
}
