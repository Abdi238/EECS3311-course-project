package ca.budgetbuddy.service;

/**
 * Observer notified when a budget crosses 80% or 100% for the first time in a month.
 */
public interface BudgetAlertListener {
    void onBudgetAlert(String message);
}
