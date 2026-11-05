package ca.budgetbuddy;

import ca.budgetbuddy.llm.AgentTool;
import ca.budgetbuddy.llm.LLMClient;
import ca.budgetbuddy.llm.LlmResponse;
import ca.budgetbuddy.llm.MockLlmClient;
import ca.budgetbuddy.model.Budget;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.CategoryRule;
import ca.budgetbuddy.model.CategorySource;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.persistence.Database;
import ca.budgetbuddy.persistence.SqliteCategoryRuleRepository;
import ca.budgetbuddy.persistence.SqliteTransactionRepository;
import ca.budgetbuddy.service.ImportResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetBuddyFacadeTest {
    private Database database;
    private BudgetBuddyFacade facade;

    @BeforeEach
    void setUp() {
        database = Database.inMemory();
        facade = BudgetBuddyFacade.create(database, new MockLlmClient(new ArrayDeque<>()));
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void importThenCategorizeAppliesSavedMerchantRules() {
        new SqliteCategoryRuleRepository(database).save(
                new CategoryRule("Sample Grocery Store", Category.GROCERIES));

        ImportResult imported = facade.importTransactions(Path.of("samples", "generic-sample.csv"), "GENERIC");
        int updated = facade.categorizeUncategorized();

        assertEquals(5, imported.getTransactions().size());
        assertEquals(2, updated);
        List<Transaction> stored = new SqliteTransactionRepository(database).findBetween(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        int groceries = 0;
        for (Transaction tx : stored) {
            if ("Sample Grocery Store".equals(tx.getMerchant())) {
                assertEquals(Category.GROCERIES, tx.getCategory());
                assertEquals(CategorySource.RULE, tx.getSource());
                groceries++;
            } else {
                assertEquals(Category.UNCATEGORIZED, tx.getCategory());
            }
        }
        assertEquals(2, groceries);
    }

    @Test
    void correctCategoryRemembersTheMerchant() {
        ImportResult imported = facade.importTransactions(Path.of("samples", "generic-sample.csv"), "GENERIC");
        Transaction firstCoffee = imported.getTransactions().get(1);

        Transaction corrected = facade.correctCategory(firstCoffee.getId(), Category.DINING, true);

        assertEquals(Category.DINING, corrected.getCategory());
        assertEquals(CategorySource.USER, corrected.getSource());
        List<Transaction> coffee = new SqliteTransactionRepository(database).findByMerchant("Campus Coffee Shop");
        assertEquals(2, coffee.size());
        int remembered = 0;
        for (Transaction tx : coffee) {
            assertEquals(Category.DINING, tx.getCategory());
            if (tx.getId() != firstCoffee.getId()) {
                assertEquals(CategorySource.RULE, tx.getSource());
                remembered++;
            }
        }
        assertEquals(1, remembered);
    }

    @Test
    void saveBudgetAndReadItBack() {
        Budget saved = facade.saveBudget(Category.DINING, new BigDecimal("100.00"), YearMonth.of(2026, 10));

        List<Budget> budgets = facade.getBudgets(YearMonth.of(2026, 10));

        assertEquals(1, budgets.size());
        assertEquals(saved.getId(), budgets.get(0).getId());
        assertEquals(Category.DINING, budgets.get(0).getCategory());
        assertEquals(0, new BigDecimal("100.00").compareTo(budgets.get(0).getLimit()));
        assertEquals(YearMonth.of(2026, 10), budgets.get(0).getMonth());
        assertEquals(0, BigDecimal.ZERO.compareTo(budgets.get(0).getSpent()));
    }

    @Test
    void askRunsSpendingToolThroughTheMockClient() {
        facade.importTransactions(Path.of("samples", "generic-sample.csv"), "GENERIC");
        ObjectNode args = new ObjectMapper().createObjectNode();
        args.put("query", "spendingByCategory");
        args.put("month", "2026-10");
        Queue<LlmResponse> script = new ArrayDeque<>();
        script.add(new LlmResponse(null, "SpendingTool", args));
        script.add(new LlmResponse(
                "Fake October spending for Sample Grocery Store and the other sample merchants is 90.51.",
                null,
                null));
        HistoryClient client = new HistoryClient(new MockLlmClient(script));
        BudgetBuddyFacade asking = BudgetBuddyFacade.create(database, client);

        String answer = asking.ask("How much did the fake October ledger spend?");

        assertEquals(
                "Fake October spending for Sample Grocery Store and the other sample merchants is 90.51.",
                answer);
        assertEquals(List.of("SpendingTool"), asking.getLastToolsUsed());
        assertEquals(2, client.histories.size());
        String toolTurn = String.join("\n", client.histories.get(1));
        assertTrue(toolTurn.contains("UNCATEGORIZED"));
        assertTrue(toolTurn.contains("90.51"));
        assertTrue(script.isEmpty());
    }

    @Test
    void blankApiKeyKeepsTheAssistantOffline(@TempDir Path tempDir) {
        Path dbFile = tempDir.resolve("offline-test.db");
        BudgetBuddyFacade offline = BudgetBuddyFacade.create(dbFile, "   ");
        try {
            assertEquals(BudgetBuddyFacade.ASSISTANT_OFFLINE, offline.ask("Any fake question"));
            assertEquals(BudgetBuddyFacade.AI_CATEGORIZATION_OFFLINE, offline.aiCategorizationNotice());
            assertTrue(offline.getLastToolsUsed().isEmpty());
        } finally {
            offline.close();
        }
    }

    @Test
    void mockClientEnablesAiCategorization() {
        assertEquals("", facade.aiCategorizationNotice());
    }

    @Test
    void savingPlanIsNotImplementedYet() {
        assertThrows(UnsupportedOperationException.class, () -> facade.generateSavingPlan(1L));
    }

    private static final class HistoryClient implements LLMClient {
        private final MockLlmClient delegate;
        private final List<List<String>> histories = new ArrayList<>();

        private HistoryClient(MockLlmClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public LlmResponse complete(String prompt, List<String> history, List<AgentTool> tools) {
            histories.add(List.copyOf(history));
            return delegate.complete(prompt, history, tools);
        }
    }
}
