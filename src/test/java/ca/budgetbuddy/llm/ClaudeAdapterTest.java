package ca.budgetbuddy.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClaudeAdapterTest {

    @Test
    void missingApiKeyThrowsBeforeAnyRequest() {
        LlmConfig config = new LlmConfig("   ", "fake-model");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new ClaudeAdapter(config));

        assertEquals(LlmConfigLoader.MISSING_API_KEY_MESSAGE, error.getMessage());
    }
}
