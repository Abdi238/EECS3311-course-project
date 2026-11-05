package ca.budgetbuddy;

import ca.budgetbuddy.llm.LlmResponse;
import ca.budgetbuddy.llm.MockLlmClient;
import ca.budgetbuddy.model.SavingsGoal;
import ca.budgetbuddy.persistence.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SavingPlanTest {
    private static final String PLAN_TEXT =
            "Monthly target 50.00. Cut Campus Coffee Shop. Skip one City Transit ride. "
                    + "Cook instead of dining out. The goal looks realistic.";

    private Database database;
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
    void planCallsSpendingToolAndReturnsTheMockText() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode spending = mapper.createObjectNode();
        spending.put("query", "spendingByCategory");
        spending.put("month", YearMonth.now().toString());
        ObjectNode recurring = mapper.createObjectNode();
        recurring.put("query", "recurringPayments");
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse(null, "SpendingTool", spending));
        script.add(new LlmResponse(null, "SpendingTool", recurring));
        script.add(new LlmResponse(PLAN_TEXT, null, null));
        open(new MockLlmClient(script));
        SavingsGoal goal = facade.createGoal(
                "Emergency fund", new BigDecimal("500.00"), LocalDate.now().plusMonths(10));

        String plan = facade.generateSavingPlan(goal.getId());

        assertEquals(PLAN_TEXT, plan);
        assertEquals(List.of("SpendingTool", "SpendingTool"), facade.getLastToolsUsed());
        assertEquals(true, script.isEmpty());
    }

    @Test
    void completedGoalReturnsTheReachedMessage() {
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse("This reply must not be used.", null, null));
        open(new MockLlmClient(script));
        SavingsGoal goal = facade.createGoal(
                "Emergency fund", new BigDecimal("100.00"), LocalDate.now().plusMonths(4));
        facade.addContribution(goal.getId(), new BigDecimal("100.00"));

        assertEquals(BudgetBuddyFacade.GOAL_ALREADY_REACHED, facade.generateSavingPlan(goal.getId()));
        assertFalse(script.isEmpty());
    }

    @Test
    void offlinePlanShowsOnlyTheRequiredMonthlyAmount() {
        open(null);
        SavingsGoal created = facade.createGoal(
                "Emergency fund", new BigDecimal("500.00"), LocalDate.now().plusMonths(10));
        SavingsGoal stored = facade.getGoals().get(0);

        String plan = facade.generateSavingPlan(created.getId());

        assertEquals("Required per month: " + stored.requiredPerMonth().toPlainString(), plan);
    }

    private void open(ca.budgetbuddy.llm.LLMClient client) {
        database = Database.inMemory();
        facade = BudgetBuddyFacade.create(database, client);
    }
}
