package ca.budgetbuddy.service;

import ca.budgetbuddy.llm.LlmResponse;
import ca.budgetbuddy.llm.MockLlmClient;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.CategoryRule;
import ca.budgetbuddy.model.CategorySource;
import ca.budgetbuddy.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiAssistedStrategyTest {
    @Test
    void appliesAValidMockReplyAndSkipsLowConfidence() {
        Transaction coffee = transaction(2, "Campus Coffee Shop", "6.25");
        Transaction transit = transaction(3, "City Transit", "3.30");
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse("""
                [
                  {"index": 0, "category": "DINING", "confidence": 0.91},
                  {"index": 1, "category": "TRANSPORT", "confidence": 0.40}
                ]
                """, null, null));

        new AiAssistedStrategy(new MockLlmClient(script)).categorize(
                List.of(coffee, transit),
                List.of(new CategoryRule("Sample Grocery Store", Category.GROCERIES)));

        assertEquals(Category.DINING, coffee.getCategory());
        assertEquals(CategorySource.AI, coffee.getSource());
        assertEquals(0.91, coffee.getConfidence(), 0.0001);
        assertEquals(Category.UNCATEGORIZED, transit.getCategory());
        assertEquals(CategorySource.NONE, transit.getSource());
        assertEquals(true, script.isEmpty());
    }

    @Test
    void invalidJsonLeavesTransactionsUncategorized() {
        Transaction coffee = transaction(2, "Campus Coffee Shop", "6.25");
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse("this is not json", null, null));

        new AiAssistedStrategy(new MockLlmClient(script)).categorize(List.of(coffee), List.of());

        assertEquals(Category.UNCATEGORIZED, coffee.getCategory());
        assertEquals(CategorySource.NONE, coffee.getSource());
    }

    @Test
    void unknownCategoryLeavesThatRowUncategorized() {
        Transaction coffee = transaction(2, "Campus Coffee Shop", "6.25");
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse(
                "[{\"index\": 0, \"category\": \"NOT_A_CATEGORY\", \"confidence\": 0.99}]",
                null,
                null));

        new AiAssistedStrategy(new MockLlmClient(script)).categorize(List.of(coffee), List.of());

        assertEquals(Category.UNCATEGORIZED, coffee.getCategory());
        assertEquals(CategorySource.NONE, coffee.getSource());
    }

    @Test
    void userSetCategoryIsNeverOverwritten() {
        Transaction groceries = transaction(1, "Sample Grocery Store", "54.21");
        groceries.setCategory(Category.GROCERIES, CategorySource.USER, 1.0);
        Transaction coffee = transaction(2, "Campus Coffee Shop", "6.25");
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse(
                "[{\"index\": 0, \"category\": \"DINING\", \"confidence\": 0.99}]",
                null,
                null));

        new AiAssistedStrategy(new MockLlmClient(script)).categorize(List.of(groceries, coffee), List.of());

        assertEquals(Category.GROCERIES, groceries.getCategory());
        assertEquals(CategorySource.USER, groceries.getSource());
        assertEquals(1.0, groceries.getConfidence(), 0.0001);
        assertEquals(Category.DINING, coffee.getCategory());
        assertEquals(CategorySource.AI, coffee.getSource());
    }

    private static Transaction transaction(int day, String merchant, String amount) {
        return new Transaction(LocalDate.of(2026, 10, day), merchant, new BigDecimal(amount));
    }
}
