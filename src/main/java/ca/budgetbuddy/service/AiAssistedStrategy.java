package ca.budgetbuddy.service;

import ca.budgetbuddy.llm.LLMClient;
import ca.budgetbuddy.llm.LlmResponse;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.CategoryRule;
import ca.budgetbuddy.model.CategorySource;
import ca.budgetbuddy.model.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Applies saved merchant rules, then asks the model to categorize what is left.
 * A reply is used only when it is valid JSON, names a known category, and has
 * confidence of {@value #MIN_CONFIDENCE} or higher. User-set categories are kept.
 */
public class AiAssistedStrategy implements CategorizationStrategy {
    public static final double MIN_CONFIDENCE = 0.6;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LLMClient client;

    public AiAssistedStrategy(LLMClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public void categorize(List<Transaction> txs, List<CategoryRule> rules) {
        if (txs == null) {
            return;
        }
        new RuleBasedStrategy().categorize(txs, rules);
        List<Transaction> remaining = new ArrayList<>();
        for (Transaction tx : txs) {
            if (tx.getSource() == CategorySource.USER) {
                continue;
            }
            if (tx.getCategory() == Category.UNCATEGORIZED) {
                remaining.add(tx);
            }
        }
        if (remaining.isEmpty()) {
            return;
        }
        LlmResponse response;
        try {
            response = client.complete(
                    PromptBuilder.categorizePrompt(remaining, rules), List.of(), List.of());
        } catch (RuntimeException ex) {
            return;
        }
        if (response == null || response.getText() == null || response.getText().isBlank()) {
            return;
        }
        applyReply(remaining, response.getText());
    }

    private void applyReply(List<Transaction> remaining, String text) {
        JsonNode root;
        try {
            root = MAPPER.readTree(text);
        } catch (JsonProcessingException ex) {
            return;
        }
        if (root == null || !root.isArray()) {
            return;
        }
        for (JsonNode item : root) {
            applyItem(remaining, item);
        }
    }

    private void applyItem(List<Transaction> remaining, JsonNode item) {
        if (item == null || !item.isObject()) {
            return;
        }
        if (!item.hasNonNull("index") || !item.get("index").canConvertToInt()) {
            return;
        }
        int index = item.get("index").asInt();
        if (index < 0 || index >= remaining.size()) {
            return;
        }
        Transaction tx = remaining.get(index);
        if (tx.getSource() == CategorySource.USER) {
            return;
        }
        if (!item.hasNonNull("category") || !item.hasNonNull("confidence")) {
            return;
        }
        if (!item.get("confidence").isNumber()) {
            return;
        }
        double confidence = item.get("confidence").asDouble();
        if (confidence < MIN_CONFIDENCE) {
            return;
        }
        Category category = parseCategory(item.get("category").asText());
        if (category == null || category == Category.UNCATEGORIZED) {
            return;
        }
        tx.setCategory(category, CategorySource.AI, confidence);
    }

    private static Category parseCategory(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Category.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
