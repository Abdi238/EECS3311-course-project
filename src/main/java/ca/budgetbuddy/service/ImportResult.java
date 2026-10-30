package ca.budgetbuddy.service;

import budgetbuddy.model.Transaction;

import java.util.List;

/**
 * Valid transactions parsed from a CSV, plus error messages for skipped rows.
 */
public final class ImportResult {
    private final List<Transaction> transactions;
    private final List<String> errors;

    public ImportResult(List<Transaction> transactions, List<String> errors) {
        this.transactions = List.copyOf(transactions);
        this.errors = List.copyOf(errors);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<String> getErrors() {
        return errors;
    }
}
