package ca.budgetbuddy.llm;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A named operation the model can request. The registry runs {@link #execute(JsonNode)}
 * when the model sends that name.
 */
public interface AgentTool {
    String getName();

    String getDescription();

    String execute(JsonNode args);
}
