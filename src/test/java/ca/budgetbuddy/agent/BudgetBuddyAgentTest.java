package ca.budgetbuddy.agent;

import ca.budgetbuddy.llm.AgentTool;
import ca.budgetbuddy.llm.LlmResponse;
import ca.budgetbuddy.llm.MockLlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetBuddyAgentTest {

    @Test
    void directTextAnswerDoesNotCallTools() {
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse(
                "Sample Grocery Store spent 12.50 in the fake October ledger.",
                null,
                null));
        ConversationMemory memory = new ConversationMemory();
        BudgetBuddyAgent agent = new BudgetBuddyAgent(
                new MockLlmClient(script),
                new ToolRegistry(List.of(new FakeLedgerTool())),
                memory,
                BudgetBuddyAgent.DEFAULT_MAX_STEPS);

        String answer = agent.ask("What did Sample Grocery Store cost in the fake ledger?");

        assertEquals("Sample Grocery Store spent 12.50 in the fake October ledger.", answer);
        assertFalse(agent.isLastAnswerIncomplete());
        assertTrue(agent.getLastToolsUsed().isEmpty());
        assertEquals(List.of(
                "user: What did Sample Grocery Store cost in the fake ledger?",
                "assistant: Sample Grocery Store spent 12.50 in the fake October ledger."
        ), memory.recent());
    }

    @Test
    void toolCallIsFollowedByTheFinalAnswer() throws Exception {
        ObjectNode args = new ObjectMapper().createObjectNode();
        args.put("merchant", "Sample Grocery Store");
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse(null, "fakeLedger", args));
        script.add(new LlmResponse(
                "Sample Grocery Store total in the fake ledger is 12.50.",
                null,
                null));
        ConversationMemory memory = new ConversationMemory();
        BudgetBuddyAgent agent = new BudgetBuddyAgent(
                new MockLlmClient(script),
                new ToolRegistry(List.of(new FakeLedgerTool())),
                memory,
                BudgetBuddyAgent.DEFAULT_MAX_STEPS);

        String answer = agent.ask("Total for Sample Grocery Store?");

        assertEquals("Sample Grocery Store total in the fake ledger is 12.50.", answer);
        assertFalse(agent.isLastAnswerIncomplete());
        assertTrue(memory.recent().contains(
                "tool: {\"merchant\":\"Sample Grocery Store\",\"total\":\"12.50\"}"));
        assertTrue(script.isEmpty());
    }

    @Test
    void unknownToolNameIsReturnedAsJsonAndTheModelRecovers() throws Exception {
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse(null, "missingFakeTool", new ObjectMapper().createObjectNode()));
        script.add(new LlmResponse(
                "The fake tool was missing, so there is no grocery total.",
                null,
                null));
        ConversationMemory memory = new ConversationMemory();
        BudgetBuddyAgent agent = new BudgetBuddyAgent(
                new MockLlmClient(script),
                new ToolRegistry(List.of(new FakeLedgerTool())),
                memory,
                BudgetBuddyAgent.DEFAULT_MAX_STEPS);

        String answer = agent.ask("Look up a tool that does not exist.");

        assertEquals("The fake tool was missing, so there is no grocery total.", answer);
        assertFalse(agent.isLastAnswerIncomplete());
        JsonNode error = new ObjectMapper().readTree(
                memory.recent().get(1).substring("tool: ".length()));
        assertEquals("Unknown tool: missingFakeTool", error.get("error").asText());
        assertTrue(script.isEmpty());
    }

    @Test
    void stepLimitStopsAfterSixToolCallsAndMarksTheAnswerIncomplete() {
        Queue<LlmResponse> script = new ArrayDeque<>();
        ObjectNode args = new ObjectMapper().createObjectNode();
        args.put("merchant", "Sample Grocery Store");
        for (int step = 0; step < BudgetBuddyAgent.DEFAULT_MAX_STEPS; step++) {
            script.add(new LlmResponse(null, "fakeLedger", args));
        }
        script.add(new LlmResponse("This seventh fake reply must not be used.", null, null));
        BudgetBuddyAgent agent = new BudgetBuddyAgent(
                new MockLlmClient(script),
                new ToolRegistry(List.of(new FakeLedgerTool())));

        String answer = agent.ask("Keep looking up Sample Grocery Store.");

        assertTrue(agent.isLastAnswerIncomplete());
        assertTrue(answer.startsWith("Incomplete answer: stopped after 6 steps."));
        assertTrue(answer.contains("{\"merchant\":\"Sample Grocery Store\",\"total\":\"12.50\"}"));
        assertEquals(1, script.size());
        assertEquals(BudgetBuddyAgent.DEFAULT_MAX_STEPS, agent.getLastToolsUsed().size());
    }

    @Test
    void toolsUsedListRecordsEachCallInOrder() {
        ObjectNode args = new ObjectMapper().createObjectNode();
        args.put("merchant", "Campus Coffee Shop");
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse(null, "fakeLedger", args));
        script.add(new LlmResponse(null, "fakeLedger", args));
        script.add(new LlmResponse("Two fake lookups are enough.", null, null));
        BudgetBuddyAgent agent = new BudgetBuddyAgent(
                new MockLlmClient(script),
                new ToolRegistry(List.of(new FakeLedgerTool())));

        String answer = agent.ask("Check the fake coffee total twice.");

        assertEquals("Two fake lookups are enough.", answer);
        assertEquals(List.of("fakeLedger", "fakeLedger"), agent.getLastToolsUsed());
    }

    private static final class FakeLedgerTool implements AgentTool {
        @Override
        public String getName() {
            return "fakeLedger";
        }

        @Override
        public String getDescription() {
            return "Returns a fake grocery total. Test double only.";
        }

        @Override
        public String execute(JsonNode args) {
            String merchant = "Sample Grocery Store";
            if (args != null && args.hasNonNull("merchant")) {
                merchant = args.get("merchant").asText();
            }
            ObjectNode result = new ObjectMapper().createObjectNode();
            result.put("merchant", merchant);
            result.put("total", "12.50");
            return result.toString();
        }
    }
}
