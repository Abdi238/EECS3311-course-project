package ca.budgetbuddy.service;

import ca.budgetbuddy.model.Anomaly;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.CategoryRule;
import ca.budgetbuddy.model.MonthlySummary;
import ca.budgetbuddy.model.RecurringPayment;
import ca.budgetbuddy.model.SavingsGoal;
import ca.budgetbuddy.model.Transaction;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Builds fixed prompts. The model is asked for JSON and is not given tools here.
 */
public final class PromptBuilder {
    private PromptBuilder() {
    }

    public static String categorizePrompt(List<Transaction> transactions, List<CategoryRule> rules) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Categorize each transaction. Reply with a JSON array only. ");
        prompt.append("Each object has index, category, and confidence between 0 and 1. ");
        prompt.append("Allowed categories:");
        for (Category category : Category.values()) {
            if (category == Category.UNCATEGORIZED) {
                continue;
            }
            prompt.append(' ').append(category.name());
        }
        prompt.append(". Saved merchant rules as examples:");
        if (rules == null || rules.isEmpty()) {
            prompt.append(" none.");
        } else {
            for (CategoryRule rule : rules) {
                prompt.append(" \"").append(rule.getMerchant())
                        .append("\" -> ").append(rule.getCategory().name()).append('.');
            }
        }
        prompt.append(" Transactions:");
        int index = 0;
        for (Transaction tx : transactions) {
            prompt.append(' ').append(index)
                    .append(". ").append(tx.getDate())
                    .append(' ').append(tx.getMerchant())
                    .append(' ').append(tx.getAmount().toPlainString()).append('.');
            index++;
        }
        return prompt.toString();
    }

    public static String subscriptionPrompt(List<RecurringPayment> payments) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Label each recurring charge. Reply with a JSON array only. ");
        prompt.append("Each object has index, displayName, and subscription (true or false). ");
        prompt.append("Charges:");
        int index = 0;
        for (RecurringPayment payment : payments) {
            prompt.append(' ').append(index)
                    .append(". merchant ").append(payment.getMerchant())
                    .append(" amount ").append(payment.getAmount().toPlainString())
                    .append(" frequency ").append(payment.getFrequency()).append('.');
            index++;
        }
        return prompt.toString();
    }

    public static String anomalyPrompt(List<Anomaly> anomalies) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Explain each flagged expense in one sentence. Reply with a JSON array only. ");
        prompt.append("Each object has index and explanation. Use only the reason and the amount. ");
        prompt.append("Charges:");
        int index = 0;
        for (Anomaly anomaly : anomalies) {
            Transaction tx = anomaly.getTransaction();
            prompt.append(' ').append(index)
                    .append(". ").append(tx.getDate())
                    .append(' ').append(tx.getMerchant())
                    .append(' ').append(tx.getAmount().toPlainString())
                    .append(' ').append(tx.getCategory().name())
                    .append(" reason ").append(anomaly.getReason()).append('.');
            index++;
        }
        return prompt.toString();
    }

    public static String summaryPrompt(MonthlySummary summary) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Write a short monthly recap and exactly three saving tips. ");
        prompt.append("Use only the numbers below. Do not invent transactions. ");
        prompt.append("Month ").append(summary.getMonth()).append(". ");
        prompt.append("Income ").append(summary.getIncome().toPlainString()).append(". ");
        prompt.append("Spent ").append(summary.getSpent().toPlainString()).append(". ");
        prompt.append("Category totals:");
        if (summary.getCategoryTotals().isEmpty()) {
            prompt.append(" none.");
        } else {
            for (Map.Entry<Category, BigDecimal> entry : summary.getCategoryTotals().entrySet()) {
                prompt.append(' ').append(entry.getKey().name())
                        .append(' ').append(entry.getValue().toPlainString()).append('.');
            }
        }
        prompt.append(" Budget results:");
        if (summary.getBudgetResults().isBlank()) {
            prompt.append(" none.");
        } else {
            prompt.append(' ').append(summary.getBudgetResults().replace('\n', ' ')).append('.');
        }
        return prompt.toString();
    }

    public static String askPrompt(YearMonth today, List<YearMonth> monthsWithData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are Budget Buddy. Answer the user's personal finance question. ");
        prompt.append("Use a tool when you need data, then answer in text. ");
        appendDateContext(prompt, today, monthsWithData);
        return prompt.toString();
    }

    public static String savingPlanPrompt(SavingsGoal goal, YearMonth today, List<YearMonth> monthsWithData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Write a saving plan for this goal. ");
        prompt.append("Call SpendingTool at most twice: once for spendingByCategory on a month that has data, ");
        prompt.append("and once for recurringPayments if needed. ");
        prompt.append("If a tool returns empty categories or lists monthsWithData, use one of those months ");
        prompt.append("or stop calling tools and answer from the goal numbers. ");
        prompt.append("Do not keep trying random months. After the tool results, reply in plain text only ");
        prompt.append("with a monthly target, 3 to 5 suggestions, and whether the goal looks realistic. ");
        prompt.append("Use only numbers from the tools and this goal. Do not invent transactions. ");
        prompt.append("The plan is informational and not financial advice. ");
        appendDateContext(prompt, today, monthsWithData);
        prompt.append("Goal ").append(goal.getName());
        prompt.append(". Target ").append(goal.getTarget().toPlainString());
        prompt.append(". Saved ").append(goal.getSaved().toPlainString());
        prompt.append(". Deadline ").append(goal.getDeadline());
        prompt.append(". Required per month ").append(goal.requiredPerMonth().toPlainString());
        prompt.append('.');
        return prompt.toString();
    }

    private static void appendDateContext(
            StringBuilder prompt, YearMonth today, List<YearMonth> monthsWithData) {
        YearMonth current = today == null ? YearMonth.now() : today;
        prompt.append("Today's month is ").append(current).append(". ");
        prompt.append("When the user names a month without a year, use that year. ");
        prompt.append("Tool month arguments must be yyyy-MM. ");
        if (monthsWithData == null || monthsWithData.isEmpty()) {
            prompt.append("No months currently have spending data. ");
            return;
        }
        prompt.append("Months with spending data:");
        for (YearMonth month : monthsWithData) {
            prompt.append(' ').append(month);
        }
        prompt.append(". Prefer those months when looking up spending. ");
    }
}
