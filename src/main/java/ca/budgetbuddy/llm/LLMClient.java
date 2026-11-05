package ca.budgetbuddy.llm;

import java.util.List;

/**
 * One completion call to a language model. The rest of the app depends on this
 * interface, not on a vendor SDK.
 */
public interface LLMClient {
    LlmResponse complete(String prompt, List<String> history, List<AgentTool> tools);
}
