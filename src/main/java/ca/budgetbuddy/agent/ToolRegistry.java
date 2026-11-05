package ca.budgetbuddy.agent;

import ca.budgetbuddy.llm.AgentTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Invokes an {@link AgentTool} by the name the model sent. Failures become a JSON
 * error string so the model can try again.
 */
public final class ToolRegistry {
    private final Map<String, AgentTool> tools;
    private final ObjectMapper objectMapper;

    public ToolRegistry(List<AgentTool> tools) {
        Objects.requireNonNull(tools, "tools");
        this.tools = new LinkedHashMap<>();
        this.objectMapper = new ObjectMapper();
        for (AgentTool tool : tools) {
            Objects.requireNonNull(tool, "tool");
            String name = tool.getName();
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("AgentTool name is required.");
            }
            if (this.tools.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate tool name: " + name);
            }
            this.tools.put(name, tool);
        }
    }

    public List<AgentTool> getTools() {
        return List.copyOf(tools.values());
    }

    public String execute(String toolName, JsonNode args) {
        if (toolName == null || !tools.containsKey(toolName)) {
            String reportedName = "";
            if (toolName != null) {
                reportedName = toolName;
            }
            return errorJson("Unknown tool: " + reportedName);
        }
        try {
            String result = tools.get(toolName).execute(args);
            if (result == null) {
                return "";
            }
            return result;
        } catch (Exception exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = exception.getClass().getSimpleName();
            }
            return errorJson(message);
        }
    }

    private String errorJson(String message) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", message);
        try {
            return objectMapper.writeValueAsString(error);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write tool error JSON.", exception);
        }
    }
}
