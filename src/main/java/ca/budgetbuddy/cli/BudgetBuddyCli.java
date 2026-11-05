package ca.budgetbuddy.cli;

import ca.budgetbuddy.BudgetBuddyFacade;
import ca.budgetbuddy.llm.LlmConfigLoader;
import ca.budgetbuddy.model.Budget;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.MonthlySummary;
import ca.budgetbuddy.model.SavingsGoal;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.service.ImportResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.ScopeType;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Command line for Budget Buddy. Every command goes through {@link BudgetBuddyFacade}.
 */
@Command(
        name = "budgetbuddy",
        mixinStandardHelpOptions = true,
        version = "1.0-SNAPSHOT",
        description = "Budget Buddy command line.",
        subcommands = {
                BudgetBuddyCli.ImportCommand.class,
                BudgetBuddyCli.CategorizeCommand.class,
                BudgetBuddyCli.RecategorizeCommand.class,
                BudgetBuddyCli.BudgetCommand.class,
                BudgetBuddyCli.TrendsCommand.class,
                BudgetBuddyCli.GoalCommand.class,
                BudgetBuddyCli.AskCommand.class
        }
)
public class BudgetBuddyCli implements Callable<Integer> {
    @Option(names = "--db", defaultValue = "budgetbuddy.db", scope = ScopeType.INHERIT,
            description = "SQLite database file. Default: budgetbuddy.db")
    private Path db;

    private BudgetBuddyFacade facade;

    public static void main(String[] args) {
        System.exit(execute(args));
    }

    public static int execute(String[] args) {
        return new CommandLine(new BudgetBuddyCli()).execute(args);
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    private BudgetBuddyFacade facade() {
        if (facade == null) {
            Path databasePath = db;
            if (databasePath == null) {
                databasePath = Path.of("budgetbuddy.db");
            }
            facade = BudgetBuddyFacade.create(databasePath, configuredApiKey());
        }
        return facade;
    }

    private static String configuredApiKey() {
        try {
            return new LlmConfigLoader().load().getApiKey();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    private static Category parseCategory(String text) {
        try {
            return Category.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown category: " + text);
        }
    }

    private static BigDecimal parseAmount(String text) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Amount must be a number: " + text);
        }
    }

    private static YearMonth parseMonth(String text) {
        try {
            return YearMonth.parse(text);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Month must be yyyy-MM: " + text);
        }
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Date must be yyyy-MM-dd: " + text);
        }
    }

    private static int run(Callable<Integer> command) {
        try {
            return command.call();
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            return 2;
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return 1;
        }
    }

    @Command(name = "import", description = "Import a bank CSV. Bank is TD, RBC, or GENERIC.")
    public static class ImportCommand implements Callable<Integer> {
        @ParentCommand
        private BudgetBuddyCli parent;

        @Parameters(index = "0", paramLabel = "FILE")
        private Path file;

        @Option(names = "--bank", required = true, description = "TD, RBC, or GENERIC")
        private String bank;

        @Override
        public Integer call() {
            return run(() -> {
                ImportResult result = parent.facade().importTransactions(file, bank);
                System.out.println("Imported " + result.getTransactions().size()
                        + " transaction(s) from " + file + " (" + bank + ").");
                if (!result.getSkippedDuplicates().isEmpty()) {
                    System.out.println("Skipped duplicates: " + result.getSkippedDuplicates().size());
                }
                if (!result.getErrors().isEmpty()) {
                    System.out.println("Errors: " + result.getErrors().size());
                    for (String error : result.getErrors()) {
                        System.out.println(error);
                    }
                }
                return 0;
            });
        }
    }

    @Command(name = "categorize", description = "Apply saved merchant rules to uncategorized transactions.")
    public static class CategorizeCommand implements Callable<Integer> {
        @ParentCommand
        private BudgetBuddyCli parent;

        @Override
        public Integer call() {
            return run(() -> {
                int updated = parent.facade().categorizeUncategorized();
                System.out.println("Categorized " + updated + " transaction(s).");
                return 0;
            });
        }
    }

    @Command(name = "recategorize", description = "Set a transaction category. --remember saves a merchant rule.")
    public static class RecategorizeCommand implements Callable<Integer> {
        @ParentCommand
        private BudgetBuddyCli parent;

        @Parameters(index = "0", paramLabel = "ID")
        private long id;

        @Parameters(index = "1", paramLabel = "CATEGORY")
        private String category;

        @Option(names = "--remember", description = "Remember this merchant")
        private boolean remember;

        @Override
        public Integer call() {
            return run(() -> {
                Transaction tx = parent.facade().correctCategory(id, parseCategory(category), remember);
                System.out.println("Transaction " + tx.getId() + " set to " + tx.getCategory() + ".");
                if (remember) {
                    System.out.println("Remembered " + tx.getMerchant() + " as " + tx.getCategory() + ".");
                }
                return 0;
            });
        }
    }

    @Command(name = "budget", description = "Set or show a monthly budget.",
            subcommands = {BudgetSetCommand.class, BudgetStatusCommand.class})
    public static class BudgetCommand implements Callable<Integer> {
        @ParentCommand
        private BudgetBuddyCli parent;

        @Override
        public Integer call() {
            CommandLine.usage(this, System.out);
            return 0;
        }
    }

    @Command(name = "set", description = "Save a category limit for a month.")
    public static class BudgetSetCommand implements Callable<Integer> {
        @ParentCommand
        private BudgetCommand budget;

        @Parameters(index = "0", paramLabel = "CATEGORY")
        private String category;

        @Parameters(index = "1", paramLabel = "AMOUNT")
        private String amount;

        @Option(names = "--month", required = true, paramLabel = "yyyy-MM")
        private String month;

        @Override
        public Integer call() {
            return run(() -> {
                Budget saved = budget.parent.facade().saveBudget(
                        parseCategory(category), parseAmount(amount), parseMonth(month));
                System.out.println("Saved " + saved.getMonth() + " " + saved.getCategory()
                        + " limit " + saved.getLimit().toPlainString() + ".");
                return 0;
            });
        }
    }

    @Command(name = "status", description = "Show budgets for a month.")
    public static class BudgetStatusCommand implements Callable<Integer> {
        @ParentCommand
        private BudgetCommand budget;

        @Option(names = "--month", required = true, paramLabel = "yyyy-MM")
        private String month;

        @Override
        public Integer call() {
            return run(() -> {
                YearMonth parsed = parseMonth(month);
                List<Budget> found = budget.parent.facade().getBudgets(parsed);
                if (found.isEmpty()) {
                    System.out.println("No budgets for " + parsed + ".");
                    return 0;
                }
                for (Budget item : found) {
                    System.out.println(item.getMonth() + " " + item.getCategory()
                            + " limit " + item.getLimit().toPlainString()
                            + " spent " + item.getSpent().toPlainString());
                }
                return 0;
            });
        }
    }

    @Command(name = "trends", description = "Show income and spending for each month in a range.")
    public static class TrendsCommand implements Callable<Integer> {
        @ParentCommand
        private BudgetBuddyCli parent;

        @Option(names = "--from", required = true, paramLabel = "yyyy-MM")
        private String from;

        @Option(names = "--to", required = true, paramLabel = "yyyy-MM")
        private String to;

        @Override
        public Integer call() {
            return run(() -> {
                List<MonthlySummary> summaries = parent.facade().getSpendingTrends(
                        parseMonth(from), parseMonth(to));
                for (MonthlySummary summary : summaries) {
                    System.out.println(summary.getMonth()
                            + " income " + summary.getIncome().toPlainString()
                            + " spent " + summary.getSpent().toPlainString());
                }
                return 0;
            });
        }
    }

    @Command(name = "goal", description = "Savings goals.", subcommands = GoalAddCommand.class)
    public static class GoalCommand implements Callable<Integer> {
        @ParentCommand
        private BudgetBuddyCli parent;

        @Override
        public Integer call() {
            CommandLine.usage(this, System.out);
            return 0;
        }
    }

    @Command(name = "add", description = "Add a savings goal.")
    public static class GoalAddCommand implements Callable<Integer> {
        @ParentCommand
        private GoalCommand goal;

        @Parameters(index = "0", paramLabel = "NAME")
        private String name;

        @Parameters(index = "1", paramLabel = "TARGET")
        private String target;

        @Option(names = "--deadline", required = true, paramLabel = "yyyy-MM-dd")
        private String deadline;

        @Override
        public Integer call() {
            return run(() -> {
                SavingsGoal saved = goal.parent.facade().createGoal(
                        name, parseAmount(target), parseDate(deadline));
                System.out.println("Saved goal " + saved.getId() + " " + saved.getName()
                        + " target " + saved.getTarget().toPlainString()
                        + " deadline " + saved.getDeadline() + ".");
                return 0;
            });
        }
    }

    @Command(name = "ask", description = "Ask Budget Buddy a question.")
    public static class AskCommand implements Callable<Integer> {
        @ParentCommand
        private BudgetBuddyCli parent;

        @Parameters(index = "0", paramLabel = "QUESTION")
        private String question;

        @Override
        public Integer call() {
            return run(() -> {
                String answer = parent.facade().ask(question);
                System.out.println(answer);
                List<String> tools = parent.facade().getLastToolsUsed();
                if (!tools.isEmpty()) {
                    System.out.println("Tools used: " + String.join(", ", tools));
                }
                return 0;
            });
        }
    }
}
