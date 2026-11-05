package ca.budgetbuddy.gui;

import ca.budgetbuddy.service.BudgetAlertListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Observer that shows budget alerts on the window banner, including the Budgets and Goals tab.
 */
public final class GuiBudgetAlertListener implements BudgetAlertListener {
    private final Consumer<String> banner;
    private final List<String> pending = new ArrayList<>();

    public GuiBudgetAlertListener(Consumer<String> banner) {
        this.banner = banner;
    }

    @Override
    public void onBudgetAlert(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        pending.add(message);
        banner.accept(String.join(" ", pending));
    }

    /**
     * Keeps alerts that fired during a facade call on the same banner as the status text.
     */
    public String combine(String message) {
        String alerts = String.join(" ", pending);
        pending.clear();
        if (alerts.isBlank()) {
            return message;
        }
        if (message == null || message.isBlank()) {
            return alerts;
        }
        return message + " " + alerts;
    }
}
