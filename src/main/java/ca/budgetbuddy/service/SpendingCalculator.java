package ca.budgetbuddy.service;

import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.MonthlySummary;
import ca.budgetbuddy.model.RecurringPayment;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.persistence.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Totals and groupings computed from stored transactions. Money math stays in Java.
 */
public final class SpendingCalculator {
    private static final LocalDate RANGE_START = LocalDate.of(1900, 1, 1);
    private static final LocalDate RANGE_END = LocalDate.of(9999, 12, 31);

    private final TransactionRepository transactions;

    public SpendingCalculator(TransactionRepository transactions) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    public Map<Category, BigDecimal> spendingByCategory(YearMonth month) {
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        for (Transaction tx : inMonth(month)) {
            if (tx.getCategory() == Category.INCOME) {
                continue;
            }
            BigDecimal current = totals.get(tx.getCategory());
            if (current == null) {
                current = BigDecimal.ZERO;
            }
            totals.put(tx.getCategory(), current.add(tx.getAmount()));
        }
        return totals;
    }

    public List<Transaction> biggestPurchases(YearMonth month, int limit) {
        List<Transaction> purchases = new ArrayList<>();
        for (Transaction tx : inMonth(month)) {
            if (tx.getCategory() != Category.INCOME) {
                purchases.add(tx);
            }
        }
        purchases.sort((left, right) -> {
            int byAmount = right.getAmount().compareTo(left.getAmount());
            if (byAmount != 0) {
                return byAmount;
            }
            return left.getDate().compareTo(right.getDate());
        });
        if (limit >= purchases.size()) {
            return purchases;
        }
        return new ArrayList<>(purchases.subList(0, limit));
    }

    public List<RecurringPayment> recurringPayments() {
        Map<String, List<Transaction>> grouped = new TreeMap<>();
        for (Transaction tx : transactions.findBetween(RANGE_START, RANGE_END)) {
            grouped.computeIfAbsent(tx.getMerchant(), key -> new ArrayList<>()).add(tx);
        }
        List<RecurringPayment> payments = new ArrayList<>();
        for (List<Transaction> group : grouped.values()) {
            if (group.size() < 2) {
                continue;
            }
            Transaction latest = group.get(0);
            for (Transaction tx : group) {
                if (tx.getDate().isAfter(latest.getDate())) {
                    latest = tx;
                } else if (tx.getDate().equals(latest.getDate()) && tx.getId() > latest.getId()) {
                    latest = tx;
                }
            }
            payments.add(new RecurringPayment(latest.getMerchant(), latest.getAmount()));
        }
        return payments;
    }

    public BigDecimal totalFor(YearMonth month, Category category) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction tx : inMonth(month)) {
            if (tx.getCategory() == category) {
                total = total.add(tx.getAmount());
            }
        }
        return total;
    }

    public List<MonthlySummary> trends(YearMonth from, YearMonth to) {
        List<MonthlySummary> summaries = new ArrayList<>();
        YearMonth cursor = from;
        while (!cursor.isAfter(to)) {
            summaries.add(summary(cursor));
            cursor = cursor.plusMonths(1);
        }
        return summaries;
    }

    private MonthlySummary summary(YearMonth month) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal spent = BigDecimal.ZERO;
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        for (Transaction tx : inMonth(month)) {
            if (tx.getCategory() == Category.INCOME) {
                income = income.add(tx.getAmount());
            } else {
                spent = spent.add(tx.getAmount());
                BigDecimal current = totals.get(tx.getCategory());
                if (current == null) {
                    current = BigDecimal.ZERO;
                }
                totals.put(tx.getCategory(), current.add(tx.getAmount()));
            }
        }
        return new MonthlySummary(month, income, spent, totals);
    }

    private List<Transaction> inMonth(YearMonth month) {
        return transactions.findBetween(month.atDay(1), month.atEndOfMonth());
    }
}
