package ca.budgetbuddy.llm;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One model reply. A text answer leaves {@code toolName} null. A tool call sets
 * {@code toolName} and {@code toolArgs}.
 */
public final class LlmResponse {
    private final String text;
    private final String toolName;
    private final JsonNode toolArgs;

    public LlmResponse(String text, String toolName, JsonNode toolArgs) {
        this.text = text;
        this.toolName = toolName;
        this.toolArgs = toolArgs;
    }

    public String getText() {
        return text;
    }

    public String getToolName() {
        return toolName;
    }

    public JsonNode getToolArgs() {
        return toolArgs;
    }

    /**
     * @return true when the model asked to run a tool
     */
    public boolean isToolCall() {
        return toolName != null;
    }
}
