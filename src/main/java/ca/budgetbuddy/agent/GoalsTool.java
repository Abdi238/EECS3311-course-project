package ca.budgetbuddy.agent;

import ca.budgetbuddy.llm.AgentTool;
import ca.budgetbuddy.model.SavingsGoal;
import ca.budgetbuddy.persistence.GoalRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Lists saved savings goals.
 */
public final class GoalsTool implements AgentTool {
    private final GoalRepository goals;
    private final ObjectMapper objectMapper;

    public GoalsTool(GoalRepository goals) {
        this.goals = goals;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "GoalsTool";
    }

    @Override
    public String getDescription() {
        return "Lists savings goals with target, amount saved, deadline, and required amount per month.";
    }

    @Override
    public String execute(JsonNode args) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode rows = root.putArray("goals");
        List<SavingsGoal> found = goals.findAll();
        for (SavingsGoal goal : found) {
            ObjectNode row = rows.addObject();
            row.put("id", goal.getId());
            row.put("name", goal.getName());
            row.put("target", goal.getTarget().toPlainString());
            row.put("saved", goal.getSaved().toPlainString());
            row.put("deadline", goal.getDeadline().toString());
            row.put("requiredPerMonth", goal.requiredPerMonth().toPlainString());
        }
        return write(root);
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not write GoalsTool JSON.", ex);
        }
    }
}
