package ca.budgetbuddy.agent;

import ca.budgetbuddy.llm.AgentTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ToolRegistryTest {

    @Test
    void unknownToolNameReturnsJsonErrorInsteadOfThrowing() throws Exception {
        ToolRegistry registry = new ToolRegistry(List.of(new QuietTool()));

        String result = registry.execute("missingFakeTool", new ObjectMapper().createObjectNode());

        JsonNode error = new ObjectMapper().readTree(result);
        assertEquals("Unknown tool: missingFakeTool", error.get("error").asText());
        assertFalse(error.has("stackTrace"));
    }

    @Test
    void toolExceptionReturnsJsonErrorInsteadOfThrowing() throws Exception {
        ToolRegistry registry = new ToolRegistry(List.of(new QuietTool()));

        String result = registry.execute("quietTool", new ObjectMapper().createObjectNode());

        JsonNode error = new ObjectMapper().readTree(result);
        assertEquals("fake ledger failed", error.get("error").asText());
    }

    private static final class QuietTool implements AgentTool {
        @Override
        public String getName() {
            return "quietTool";
        }

        @Override
        public String getDescription() {
            return "Always fails with a fake message. Test double only.";
        }

        @Override
        public String execute(JsonNode args) {
            throw new IllegalStateException("fake ledger failed");
        }
    }
}
