package ca.budgetbuddy;

import ca.budgetbuddy.cli.BudgetBuddyCli;

/**
 * Starts the command-line interface.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        System.exit(BudgetBuddyCli.execute(args));
    }
}
