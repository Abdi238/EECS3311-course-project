package ca.budgetbuddy.service;

import budgetbuddy.model.Transaction;

import java.util.List;

/**
 * Valid transactions parsed from a CSV, plus error messages for skipped rows
 * and duplicate rows that were not imported.
 */
public final class ImportResult {
    private final List<Transaction> transactions;
    private final List<String> errors;
    private final List<String> skippedDuplicates;

    public ImportResult(List<Transaction> transactions, List<String> errors) {
        this(transactions, errors, List.of());
    }

    public ImportResult(List<Transaction> transactions, List<String> errors,
                        List<String> skippedDuplicates) {
        this.transactions = List.copyOf(transactions);
        this.errors = List.copyOf(errors);
        this.skippedDuplicates = List.copyOf(skippedDuplicates);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getSkippedDuplicates() {
        return skippedDuplicates;
    }
}
