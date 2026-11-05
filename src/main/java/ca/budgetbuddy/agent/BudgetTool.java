package ca.budgetbuddy.agent;

import ca.budgetbuddy.llm.AgentTool;
import ca.budgetbuddy.model.Budget;
import ca.budgetbuddy.persistence.BudgetRepository;
import ca.budgetbuddy.persistence.TransactionRepository;
import ca.budgetbuddy.service.SpendingCalculator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Budget progress for one month: limit, amount spent, and percent used.
 */
public final class BudgetTool implements AgentTool {
    private final BudgetRepository budgets;
    private final SpendingCalculator calculator;
    private final ObjectMapper objectMapper;

    public BudgetTool(BudgetRepository budgets, TransactionRepository transactions) {
        this.budgets = budgets;
        this.calculator = new SpendingCalculator(transactions);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "BudgetTool";
    }

    @Override
    public String getDescription() {
        return "Budget progress for a month. Argument month is yyyy-MM.";
    }

    @Override
    public String execute(JsonNode args) {
        if (args == null || !args.hasNonNull("month")) {
            return error("month is required as yyyy-MM.");
        }
        YearMonth month;
        try {
            month = YearMonth.parse(args.get("month").asText());
        } catch (DateTimeParseException ex) {
            return error("month must be yyyy-MM.");
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("month", month.toString());
        ArrayNode rows = root.putArray("budgets");
        List<Budget> found = budgets.findByMonth(month);
        for (Budget budget : found) {
            budget.setSpent(calculator.totalFor(month, budget.getCategory()));
            ObjectNode row = rows.addObject();
            row.put("category", budget.getCategory().name());
            row.put("limit", budget.getLimit().toPlainString());
            row.put("spent", budget.getSpent().toPlainString());
            row.put("percentUsed", budget.percentUsed());
        }
        return write(root);
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
            throw new IllegalStateException("Could not write BudgetTool JSON.", ex);
        }
    }
}
