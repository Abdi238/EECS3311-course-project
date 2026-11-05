package ca.budgetbuddy.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockLlmClientTest {

    @Test
    void returnsScriptedResponsesInQueueOrder() {
        LlmResponse first = new LlmResponse("First fake reply about Sample Grocery Store.", null, null);
        LlmResponse second = new LlmResponse(
                null,
                "budget",
                JsonNodeFactory.instance.objectNode().put("category", "Groceries"));
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(first);
        script.add(second);
        MockLlmClient client = new MockLlmClient(script);
        AgentTool placeholder = new AgentTool() {
            @Override
            public String getName() {
                return "budget";
            }

            @Override
            public String getDescription() {
                return "Looks up a fake grocery budget.";
            }

            @Override
            public String execute(JsonNode args) {
                return "{\"category\":\"Groceries\"}";
            }
        };

        LlmResponse firstReply = client.complete(
                "How much did the fake grocery budget use?",
                List.of("Earlier fake turn"),
                List.of(placeholder));
        LlmResponse secondReply = client.complete("And the category?", List.of(), List.of());

        assertSame(first, firstReply);
        assertFalse(firstReply.isToolCall());
        assertEquals("First fake reply about Sample Grocery Store.", firstReply.getText());
        assertSame(second, secondReply);
        assertTrue(secondReply.isToolCall());
        assertEquals("budget", secondReply.getToolName());
        assertEquals("Groceries", secondReply.getToolArgs().get("category").asText());
    }

    @Test
    void throwsWhenTheScriptIsEmpty() {
        MockLlmClient client = new MockLlmClient(new ArrayDeque<>());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> client.complete("Any fake prompt", List.of(), List.of()));

        assertEquals("MockLlmClient has no scripted responses left.", error.getMessage());
    }
}
