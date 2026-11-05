package ca.budgetbuddy.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves the API key and model name. The key is read from the {@code LLM_API_KEY}
 * environment variable, then a {@code .env} file, then {@code config.local.json}.
 */
public final class LlmConfigLoader {
    public static final String API_KEY_ENV = "LLM_API_KEY";
    public static final String MODEL_ENV = "LLM_MODEL";
    public static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";
    public static final String MISSING_API_KEY_MESSAGE =
            "No LLM API key found. Set the LLM_API_KEY environment variable, "
                    + "or provide it in .env or config.local.json.";

    private final ObjectMapper objectMapper;

    public LlmConfigLoader() {
        this.objectMapper = new ObjectMapper();
    }

    public LlmConfig load() {
        return load(System.getenv(), Path.of(".env"), Path.of("config.local.json"));
    }

    public LlmConfig load(Map<String, String> environment, Path dotenvFile, Path jsonFile) {
        Objects.requireNonNull(environment, "environment");
        Map<String, String> dotenv = readDotEnv(dotenvFile);
        JsonNode json = readJson(jsonFile);

        String apiKey = firstNonBlank(
                environment.get(API_KEY_ENV),
                dotenv.get(API_KEY_ENV),
                dotenv.get("ANTHROPIC_API_KEY"),
                jsonText(json, API_KEY_ENV),
                jsonText(json, "apiKey"),
                jsonText(json, "ANTHROPIC_API_KEY")
        );
        if (apiKey == null) {
            throw new IllegalStateException(MISSING_API_KEY_MESSAGE);
        }

        String model = firstNonBlank(
                environment.get(MODEL_ENV),
                dotenv.get(MODEL_ENV),
                dotenv.get("ANTHROPIC_MODEL"),
                jsonText(json, MODEL_ENV),
                jsonText(json, "model"),
                jsonText(json, "ANTHROPIC_MODEL"),
                DEFAULT_MODEL
        );
        return new LlmConfig(apiKey, model);
    }

    private Map<String, String> readDotEnv(Path dotenvFile) {
        Map<String, String> values = new HashMap<>();
        if (dotenvFile == null || !Files.isRegularFile(dotenvFile)) {
            return values;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(dotenvFile, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read .env file at " + dotenvFile, exception);
        }
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).trim();
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = stripQuotes(line.substring(separator + 1).trim());
            values.put(key, value);
        }
        return values;
    }

    private JsonNode readJson(Path jsonFile) {
        if (jsonFile == null || !Files.isRegularFile(jsonFile)) {
            return null;
        }
        try {
            return objectMapper.readTree(jsonFile.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read config.local.json at " + jsonFile, exception);
        }
    }

    private static String jsonText(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private static String stripQuotes(String value) {
        if (value.length() < 2) {
            return value;
        }
        boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
        boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
        if (doubleQuoted || singleQuoted) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
