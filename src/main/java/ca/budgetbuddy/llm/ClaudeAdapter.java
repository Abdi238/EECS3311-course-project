package ca.budgetbuddy.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;

/**
 * Adapts the Anthropic Java SDK to {@link LLMClient}. The API key comes from
 * {@link LlmConfigLoader} and is never hard-coded.
 */
public final class ClaudeAdapter implements LLMClient {
    private static final long MAX_TOKENS = 1024L;

    private final AnthropicClient client;
    private final String model;

    public ClaudeAdapter() {
        this(new LlmConfigLoader().load());
    }

    public ClaudeAdapter(LlmConfig config) {
        Objects.requireNonNull(config, "config");
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(LlmConfigLoader.MISSING_API_KEY_MESSAGE);
        }
        String configuredModel = config.getModel();
        if (configuredModel == null || configuredModel.isBlank()) {
            throw new IllegalStateException(
                    "No LLM model name found. Set LLM_MODEL, or add it to .env or config.local.json.");
        }
        this.model = configuredModel;
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }

    @Override
    public LlmResponse complete(String prompt, List<String> history, List<AgentTool> tools) {
        Objects.requireNonNull(prompt, "prompt");
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .addUserMessage(combinePrompt(prompt, history));
        addTools(params, tools);
        Message message = client.messages().create(params.build());
        return toLlmResponse(message);
    }

    private String combinePrompt(String prompt, List<String> history) {
        StringBuilder combined = new StringBuilder();
        if (history != null) {
            for (String turn : history) {
                if (turn == null || turn.isBlank()) {
                    continue;
                }
                combined.append(turn);
                combined.append('\n');
            }
        }
        combined.append(prompt);
        return combined.toString();
    }

    private void addTools(MessageCreateParams.Builder params, List<AgentTool> tools) {
        if (tools == null) {
            return;
        }
        for (AgentTool tool : tools) {
            Objects.requireNonNull(tool, "tool");
            String name = tool.getName();
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("AgentTool name is required.");
            }
            Tool.Builder toolBuilder = Tool.builder()
                    .name(name)
                    .inputSchema(Tool.InputSchema.builder().build());
            String description = tool.getDescription();
            if (description != null) {
                toolBuilder.description(description);
            }
            params.addTool(toolBuilder.build());
        }
    }

    private LlmResponse toLlmResponse(Message message) {
        List<ContentBlock> blocks = message.content();
        for (ContentBlock block : blocks) {
            if (block.isToolUse()) {
                ToolUseBlock toolUse = block.asToolUse();
                return new LlmResponse(null, toolUse.name(), toJsonNode(toolUse._input()));
            }
        }
        StringBuilder text = new StringBuilder();
        for (ContentBlock block : blocks) {
            if (block.isText()) {
                text.append(block.asText().text());
            }
        }
        return new LlmResponse(text.toString(), null, null);
    }

    private JsonNode toJsonNode(JsonValue input) {
        if (input == null) {
            return null;
        }
        return input.convert(JsonNode.class);
    }
}
