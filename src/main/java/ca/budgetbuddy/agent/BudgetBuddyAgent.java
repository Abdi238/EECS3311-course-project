package ca.budgetbuddy.agent;

import ca.budgetbuddy.llm.LLMClient;
import ca.budgetbuddy.llm.LlmResponse;

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
}
