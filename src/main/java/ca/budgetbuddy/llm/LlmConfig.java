package ca.budgetbuddy.llm;

/**
 * API key and model name resolved by {@link LlmConfigLoader}. The key is never
 * stored in source.
 */
public final class LlmConfig {
    private final String apiKey;
    private final String model;

    public LlmConfig(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }
}
