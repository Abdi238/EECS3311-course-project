package ca.budgetbuddy.agent;

import ca.budgetbuddy.llm.LLMClient;
import ca.budgetbuddy.llm.LlmResponse;
import ca.budgetbuddy.model.Anomaly;
import ca.budgetbuddy.model.MonthlySummary;
import ca.budgetbuddy.model.RecurringPayment;
import ca.budgetbuddy.service.PromptBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs the question-and-tool loop. The model either answers in text or names a
 * tool; tool results go back into memory until the model answers or {@code maxSteps}
 * is reached.
 */
public final class BudgetBuddyAgent {
    public static final int DEFAULT_MAX_STEPS = 6;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PROMPT =
            "You are Budget Buddy. Answer the user's personal finance question. "
                    + "Use a tool when you need data, then answer in text.";

    private final LLMClient llm;
    private final ToolRegistry tools;
    private final ConversationMemory memory;
    private final int maxSteps;
    private final List<String> lastToolsUsed = new ArrayList<>();
    private boolean lastAnswerIncomplete;

    public BudgetBuddyAgent(LLMClient llm, ToolRegistry tools) {
        this(llm, tools, new ConversationMemory(), DEFAULT_MAX_STEPS);
    }

    public BudgetBuddyAgent(LLMClient llm, ToolRegistry tools, ConversationMemory memory, int maxSteps) {
        this.llm = Objects.requireNonNull(llm, "llm");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.memory = Objects.requireNonNull(memory, "memory");
        if (maxSteps < 1) {
            throw new IllegalArgumentException("maxSteps must be at least 1.");
        }
        this.maxSteps = maxSteps;
    }

    public String ask(String question) {
        Objects.requireNonNull(question, "question");
        lastToolsUsed.clear();
        lastAnswerIncomplete = false;
        memory.add("user", question);
        return runLoop();
    }

    public List<String> getLastToolsUsed() {
        return List.copyOf(lastToolsUsed);
    }

    public boolean isLastAnswerIncomplete() {
        return lastAnswerIncomplete;
    }

    private String runLoop() {
        String lastToolResult = null;
        for (int step = 0; step < maxSteps; step++) {
            LlmResponse response = llm.complete(PROMPT, memory.recent(), tools.getTools());
            if (response == null) {
                throw new IllegalStateException("LLMClient returned a null response.");
            }
            if (!response.isToolCall()) {
                String text = response.getText();
                if (text == null) {
                    text = "";
                }
                memory.add("assistant", text);
                lastAnswerIncomplete = false;
                return text;
            }
            String toolName = response.getToolName();
            lastToolsUsed.add(toolName);
            String result = tools.execute(toolName, response.getToolArgs());
            lastToolResult = result;
            memory.add("tool", result);
        }
        lastAnswerIncomplete = true;
        String partial = "Incomplete answer: stopped after " + maxSteps + " steps.";
        if (lastToolResult != null) {
            partial = partial + " Last tool result: " + lastToolResult;
        }
        memory.add("assistant", partial);
        return partial;
    }

    /**
     * Fills display name and subscription from the model. A failed call leaves the list unlabeled.
     */
    public void labelSubscriptions(List<RecurringPayment> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        JsonNode root;
        try {
            root = replyArray(PromptBuilder.subscriptionPrompt(items));
        } catch (RuntimeException ex) {
            return;
        }
        if (root == null) {
            return;
        }
        for (JsonNode item : root) {
            applyLabel(items, item);
        }
    }

    /**
     * Fills each explanation from the model. A failed or invalid reply uses the Java reason.
     */
    public void explainAnomalies(List<Anomaly> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        JsonNode root = null;
        try {
            root = replyArray(PromptBuilder.anomalyPrompt(items));
        } catch (RuntimeException ex) {
            root = null;
        }
        if (root != null) {
            for (JsonNode item : root) {
                applyExplanation(items, item);
            }
        }
        for (Anomaly anomaly : items) {
            if (anomaly.getExplanation() == null || anomaly.getExplanation().isBlank()) {
                anomaly.setExplanation(anomaly.getReason());
            }
        }
    }

    /**
     * @return the recap text, or null when the model is unavailable or the reply is empty
     */
    public String writeMonthlySummary(MonthlySummary summary) {
        Objects.requireNonNull(summary, "summary");
        try {
            LlmResponse response = llm.complete(PromptBuilder.summaryPrompt(summary), List.of(), List.of());
            if (response == null || response.isToolCall()) {
                return null;
            }
            if (response.getText() == null || response.getText().isBlank()) {
                return null;
            }
            return response.getText().trim();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private JsonNode replyArray(String prompt) {
        LlmResponse response = llm.complete(prompt, List.of(), List.of());
        if (response == null || response.isToolCall()) {
            return null;
        }
        return readArray(response.getText());
    }

    private static JsonNode readArray(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(text);
            if (root != null && root.isArray()) {
                return root;
            }
        } catch (JsonProcessingException ex) {
            return null;
        }
        return null;
    }

    private static void applyLabel(List<RecurringPayment> items, JsonNode item) {
        if (item == null || !item.isObject()) {
            return;
        }
        int index = readIndex(item, items.size());
        if (index < 0) {
            return;
        }
        RecurringPayment payment = items.get(index);
        if (item.hasNonNull("displayName")) {
            String name = item.get("displayName").asText();
            if (!name.isBlank()) {
                payment.setDisplayName(name);
            }
        }
        if (item.has("subscription")) {
            payment.setSubscription(readFlag(item.get("subscription")));
        }
    }

    private static void applyExplanation(List<Anomaly> items, JsonNode item) {
        if (item == null || !item.isObject()) {
            return;
        }
        int index = readIndex(item, items.size());
        if (index < 0) {
            return;
        }
        if (!item.hasNonNull("explanation")) {
            return;
        }
        String explanation = item.get("explanation").asText();
        if (explanation.isBlank()) {
            return;
        }
        items.get(index).setExplanation(explanation);
    }

    private static int readIndex(JsonNode item, int size) {
        if (!item.hasNonNull("index") || !item.get("index").canConvertToInt()) {
            return -1;
        }
        int index = item.get("index").asInt();
        if (index < 0 || index >= size) {
            return -1;
        }
        return index;
    }

    private static boolean readFlag(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return "true".equalsIgnoreCase(node.asText());
    }
}
