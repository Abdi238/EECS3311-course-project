package ca.budgetbuddy.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmResponseTest {

    @Test
    void textReplyIsNotAToolCall() {
        LlmResponse response = new LlmResponse(
                "Sample Grocery Store spent 54.21 in the fake October budget.",
                null,
                null);

        assertFalse(response.isToolCall());
        assertEquals("Sample Grocery Store spent 54.21 in the fake October budget.", response.getText());
        assertNull(response.getToolName());
        assertNull(response.getToolArgs());
    }

    @Test
    void toolNameMakesTheReplyAToolCall() throws Exception {
        ObjectNode args = new ObjectMapper().createObjectNode();
        args.put("merchant", "Sample Grocery Store");
        args.put("month", "2026-10");
        LlmResponse response = new LlmResponse(null, "spending", args);

        assertTrue(response.isToolCall());
        assertNull(response.getText());
        assertEquals("spending", response.getToolName());
        assertEquals("Sample Grocery Store", response.getToolArgs().get("merchant").asText());
        assertEquals("2026-10", response.getToolArgs().get("month").asText());
    }
}
