package ca.budgetbuddy;

import ca.budgetbuddy.agent.BudgetBuddyAgent;
import ca.budgetbuddy.agent.BudgetTool;
import ca.budgetbuddy.agent.GoalsTool;
import ca.budgetbuddy.agent.SpendingTool;
import ca.budgetbuddy.agent.ToolRegistry;
import ca.budgetbuddy.llm.ClaudeAdapter;
import ca.budgetbuddy.llm.LLMClient;
import ca.budgetbuddy.llm.LlmConfig;
import ca.budgetbuddy.llm.LlmConfigLoader;
import ca.budgetbuddy.model.Anomaly;
import ca.budgetbuddy.model.Budget;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.MonthlySummary;
import ca.budgetbuddy.model.RecurringPayment;
import ca.budgetbuddy.model.SavingsGoal;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.persistence.BudgetRepository;
import ca.budgetbuddy.persistence.CategoryRuleRepository;
import ca.budgetbuddy.persistence.Database;
import ca.budgetbuddy.persistence.GoalRepository;
import ca.budgetbuddy.persistence.SqliteBudgetRepository;
import ca.budgetbuddy.persistence.SqliteCategoryRuleRepository;
import ca.budgetbuddy.persistence.SqliteGoalRepository;
import ca.budgetbuddy.persistence.SqliteTransactionRepository;
import ca.budgetbuddy.persistence.TransactionRepository;
import ca.budgetbuddy.service.AiAssistedStrategy;
import ca.budgetbuddy.service.BudgetAlertListener;
import ca.budgetbuddy.service.BudgetService;
import ca.budgetbuddy.service.CategorizationService;
import ca.budgetbuddy.service.ImportResult;
import ca.budgetbuddy.service.ImportService;
import ca.budgetbuddy.service.RuleBasedStrategy;
import ca.budgetbuddy.service.SpendingCalculator;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One entry point for the CLI and, later, the GUI. A blank API key keeps
 * categorization on rules and makes AI features report that the assistant is offline.
 */
public final class BudgetBuddyFacade implements AutoCloseable {
    public static final String ASSISTANT_OFFLINE = "assistant offline";
    public static final String AI_CATEGORIZATION_OFFLINE = "AI categorization is offline";

    private final Database database;
    private final TransactionRepository transactions;
    private final GoalRepository goals;
    private final ImportService importService;
    private final CategorizationService categorization;
    private final SpendingCalculator calculator;
    private final BudgetService budgetService;
    private final BudgetBuddyAgent agent;
    private final boolean aiCategorization;

    public static BudgetBuddyFacade create(Path dbPath, String apiKey) {
        Objects.requireNonNull(dbPath, "dbPath");
        Database database = Database.file(dbPath);
        try {
            if (apiKey == null || apiKey.isBlank()) {
                return new BudgetBuddyFacade(database, null);
            }
            Map<String, String> environment = new HashMap<>();
            environment.put(LlmConfigLoader.API_KEY_ENV, apiKey);
            LlmConfig config = new LlmConfigLoader().load(
                    environment, Path.of(".env"), Path.of("config.local.json"));
            return new BudgetBuddyFacade(database, new ClaudeAdapter(config));
        } catch (RuntimeException ex) {
            database.close();
            throw ex;
        }
    }

    /**
     * Test factory. A null client runs with the assistant offline and does not call a model.
     */
    public static BudgetBuddyFacade create(Database database, LLMClient llm) {
        Objects.requireNonNull(database, "database");
        return new BudgetBuddyFacade(database, llm);
    }

    private BudgetBuddyFacade(Database database, LLMClient llm) {
        this.database = database;
        this.transactions = new SqliteTransactionRepository(database);
        BudgetRepository budgetRepository = new SqliteBudgetRepository(database);
        this.goals = new SqliteGoalRepository(database);
        CategoryRuleRepository rules = new SqliteCategoryRuleRepository(database);
        this.importService = new ImportService(transactions);
        this.categorization = new CategorizationService(rules, transactions, new RuleBasedStrategy());
        this.calculator = new SpendingCalculator(transactions);
        this.budgetService = new BudgetService(budgetRepository, calculator);
        if (llm == null) {
            this.agent = null;
            this.aiCategorization = false;
        } else {
            this.categorization.setStrategy(new AiAssistedStrategy(llm));
            this.aiCategorization = true;
            ToolRegistry registry = new ToolRegistry(List.of(
                    new SpendingTool(transactions),
                    new BudgetTool(budgetRepository, transactions),
                    new GoalsTool(goals)
            ));
            this.agent = new BudgetBuddyAgent(llm, registry);
        }
    }

    public void addBudgetAlertListener(BudgetAlertListener listener) {
        budgetService.addListener(listener);
    }

    /**
     * Blank when an API key was provided. Otherwise the fixed offline sentence.
     */
    public String aiCategorizationNotice() {
        if (aiCategorization) {
            return "";
        }
        return AI_CATEGORIZATION_OFFLINE;
    }

    public ImportResult importTransactions(Path file, String bank) {
        ImportResult result = importService.importFile(file, bank);
        transactions.saveAll(result.getTransactions());
        budgetService.recalculateFor(result.getTransactions());
        return result;
    }

    public int categorizeUncategorized() {
        int updated = categorization.categorizeUncategorized();
        budgetService.recalculateFor(getTransactions());
        return updated;
    }

    public Transaction correctCategory(long txId, Category category, boolean remember) {
        Transaction updated = categorization.correctCategory(txId, category, remember);
        budgetService.recalculate(YearMonth.from(updated.getDate()));
        return updated;
    }

    public Budget saveBudget(Category category, BigDecimal limit, YearMonth month) {
        return budgetService.saveBudget(category, limit, month);
    }

    public List<Transaction> getTransactions() {
        return transactions.findBetween(LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31));
    }

    public List<SavingsGoal> getGoals() {
        return goals.findAll();
    }

    public List<Budget> getBudgets(YearMonth month) {
        return budgetService.getBudgets(month);
    }

    public List<MonthlySummary> getSpendingTrends(YearMonth from, YearMonth to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        return calculator.trends(from, to);
    }

    public String ask(String question) {
        Objects.requireNonNull(question, "question");
        if (agent == null) {
            return ASSISTANT_OFFLINE;
        }
        return agent.ask(question);
    }

    public List<String> getLastToolsUsed() {
        if (agent == null) {
            return List.of();
        }
        return agent.getLastToolsUsed();
    }

    public SavingsGoal createGoal(String name, BigDecimal target, LocalDate deadline) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("target must be positive");
        }
        Objects.requireNonNull(deadline, "deadline");
        return goals.save(new SavingsGoal(name, target, deadline));
    }

    public SavingsGoal addContribution(long goalId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        SavingsGoal goal = goals.findById(goalId);
        if (goal == null) {
            throw new IllegalArgumentException("Savings goal not found: " + goalId);
        }
        goal.setSaved(goal.getSaved().add(amount));
        return goals.save(goal);
    }

    public List<RecurringPayment> findRecurringPayments() {
        throw new UnsupportedOperationException("Recurring payment detection is not implemented yet.");
    }

    public List<Anomaly> findUnusualExpenses(YearMonth month) {
        throw new UnsupportedOperationException("Unusual expense detection is not implemented yet.");
    }

    public MonthlySummary generateMonthlySummary(YearMonth month) {
        throw new UnsupportedOperationException("Monthly summary is not implemented yet.");
    }

    public String generateSavingPlan(long goalId) {
        throw new UnsupportedOperationException("Saving plan is not implemented yet.");
    }

    @Override
    public void close() {
        database.close();
    }
}
