package ca.budgetbuddy.llm;

import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Returns scripted replies in queue order. Tests use this instead of the network.
 */
public final class MockLlmClient implements LLMClient {
    private final Queue<LlmResponse> scriptedResponses;

    public MockLlmClient(Queue<LlmResponse> scriptedResponses) {
        this.scriptedResponses = Objects.requireNonNull(scriptedResponses, "scriptedResponses");
    }

    @Override
    public LlmResponse complete(String prompt, List<String> history, List<AgentTool> tools) {
        LlmResponse next = scriptedResponses.poll();
        if (next == null) {
            throw new IllegalStateException("MockLlmClient has no scripted responses left.");
        }
        return next;
    }
}
