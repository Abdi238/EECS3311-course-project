package ca.budgetbuddy.llm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void environmentVariableWinsOverDotEnvAndJson() throws Exception {
        Path dotenv = writeDotEnv("fake-key-from-dotenv", "fake-model-from-dotenv");
        Path json = writeJson("fake-key-from-json", "fake-model-from-json");
        Map<String, String> environment = new HashMap<>();
        environment.put(LlmConfigLoader.API_KEY_ENV, "fake-key-from-env");
        environment.put(LlmConfigLoader.MODEL_ENV, "fake-model-from-env");

        LlmConfig config = new LlmConfigLoader().load(environment, dotenv, json);

        assertEquals("fake-key-from-env", config.getApiKey());
        assertEquals("fake-model-from-env", config.getModel());
    }

    @Test
    void blankEnvironmentFallsThroughToDotEnvFile() throws Exception {
        Path dotenv = writeDotEnv("fake-key-from-dotenv", "fake-model-from-dotenv");
        Path json = writeJson("fake-key-from-json", "fake-model-from-json");
        Map<String, String> environment = new HashMap<>();
        environment.put(LlmConfigLoader.API_KEY_ENV, "   ");
        environment.put(LlmConfigLoader.MODEL_ENV, "");

        LlmConfig config = new LlmConfigLoader().load(environment, dotenv, json);

        assertEquals("fake-key-from-dotenv", config.getApiKey());
        assertEquals("fake-model-from-dotenv", config.getModel());
    }

    @Test
    void jsonFileIsUsedWhenEnvironmentAndDotEnvAreMissing() throws Exception {
        Path missingDotEnv = tempDir.resolve(".env");
        Path json = writeJson("fake-key-from-json", "fake-model-from-json");

        LlmConfig config = new LlmConfigLoader().load(Map.of(), missingDotEnv, json);

        assertEquals("fake-key-from-json", config.getApiKey());
        assertEquals("fake-model-from-json", config.getModel());
    }

    @Test
    void dotenvAnthropicKeyIsAcceptedBeforeJson() throws Exception {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv,
                "ANTHROPIC_API_KEY=fake-key-from-dotenv-alias\nANTHROPIC_MODEL=fake-model-from-dotenv-alias\n",
                StandardCharsets.UTF_8);
        Path json = writeJson("fake-key-from-json", "fake-model-from-json");

        LlmConfig config = new LlmConfigLoader().load(Map.of(), dotenv, json);

        assertEquals("fake-key-from-dotenv-alias", config.getApiKey());
        assertEquals("fake-model-from-dotenv-alias", config.getModel());
    }

    @Test
    void missingKeyThrowsAndNamesEveryLookupLocation() {
        Path missingDotEnv = tempDir.resolve(".env");
        Path missingJson = tempDir.resolve("config.local.json");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new LlmConfigLoader().load(Map.of(), missingDotEnv, missingJson));

        assertTrue(error.getMessage().contains("LLM_API_KEY"));
        assertTrue(error.getMessage().contains(".env"));
        assertTrue(error.getMessage().contains("config.local.json"));
    }

    private Path writeDotEnv(String apiKey, String model) throws Exception {
        Path dotenv = tempDir.resolve(".env");
        String contents = "LLM_API_KEY=" + apiKey + "\nLLM_MODEL=" + model + "\n";
        Files.writeString(dotenv, contents, StandardCharsets.UTF_8);
        return dotenv;
    }

    private Path writeJson(String apiKey, String model) throws Exception {
        Path json = tempDir.resolve("config.local.json");
        String contents = "{\n"
                + "  \"LLM_API_KEY\": \"" + apiKey + "\",\n"
                + "  \"LLM_MODEL\": \"" + model + "\"\n"
                + "}\n";
        Files.writeString(json, contents, StandardCharsets.UTF_8);
        return json;
    }
}
