package ca.budgetbuddy.agent;

import ca.budgetbuddy.llm.AgentTool;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.RecurringPayment;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.persistence.TransactionRepository;
import ca.budgetbuddy.service.SpendingCalculator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Spending by category, biggest purchases, or repeated merchants.
 */
public final class SpendingTool implements AgentTool {
    private static final String QUERY_BY_CATEGORY = "spendingByCategory";
    private static final String QUERY_BIGGEST = "biggestPurchases";
    private static final String QUERY_RECURRING = "recurringPayments";

    private final SpendingCalculator calculator;
    private final ObjectMapper objectMapper;

    public SpendingTool(TransactionRepository transactions) {
        this.calculator = new SpendingCalculator(transactions);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "SpendingTool";
    }

    @Override
    public String getDescription() {
        return "Looks up spending. query is spendingByCategory, biggestPurchases, "
                + "or recurringPayments. month is yyyy-MM for the first two. "
                + "biggestPurchases accepts an optional limit.";
    }

    @Override
    public String execute(JsonNode args) {
        if (args == null || !args.hasNonNull("query")) {
            return error("Missing query. Use spendingByCategory, biggestPurchases, or recurringPayments.");
        }
        String query = args.get("query").asText();
        if (QUERY_RECURRING.equals(query)) {
            return recurring();
        }
        YearMonth month = readMonth(args);
        if (month == null) {
            return error("month is required as yyyy-MM.");
        }
        if (QUERY_BY_CATEGORY.equals(query)) {
            return byCategory(month);
        }
        if (QUERY_BIGGEST.equals(query)) {
            return biggest(month, readLimit(args));
        }
        return error("Unknown query: " + query
                + ". Use spendingByCategory, biggestPurchases, or recurringPayments.");
    }

    private String byCategory(YearMonth month) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("query", QUERY_BY_CATEGORY);
        root.put("month", month.toString());
        ArrayNode categories = root.putArray("categories");
        Map<Category, BigDecimal> totals = calculator.spendingByCategory(month);
        for (Category category : Category.values()) {
            BigDecimal total = totals.get(category);
            if (total == null) {
                continue;
            }
            ObjectNode row = categories.addObject();
            row.put("category", category.name());
            row.put("total", total.toPlainString());
        }
        return write(root);
    }

    private String biggest(YearMonth month, int limit) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("query", QUERY_BIGGEST);
        root.put("month", month.toString());
        ArrayNode purchases = root.putArray("purchases");
        for (Transaction tx : calculator.biggestPurchases(month, limit)) {
            ObjectNode row = purchases.addObject();
            row.put("date", tx.getDate().toString());
            row.put("merchant", tx.getMerchant());
            row.put("amount", tx.getAmount().toPlainString());
            row.put("category", tx.getCategory().name());
        }
        return write(root);
    }

    private String recurring() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("query", QUERY_RECURRING);
        ArrayNode payments = root.putArray("payments");
        List<RecurringPayment> found = calculator.recurringPayments();
        for (RecurringPayment payment : found) {
            ObjectNode row = payments.addObject();
            row.put("merchant", payment.getMerchant());
            row.put("amount", payment.getAmount().toPlainString());
        }
        return write(root);
    }

    private YearMonth readMonth(JsonNode args) {
        if (!args.hasNonNull("month")) {
            return null;
        }
        try {
            return YearMonth.parse(args.get("month").asText());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private int readLimit(JsonNode args) {
        int limit = 5;
        if (args.has("limit") && args.get("limit").isNumber()) {
            limit = args.get("limit").asInt();
        }
        if (limit < 1) {
            return 5;
        }
        return limit;
    }

    private String error(String message) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("error", message);
        return write(root);
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not write SpendingTool JSON.", ex);
        }
    }
}
