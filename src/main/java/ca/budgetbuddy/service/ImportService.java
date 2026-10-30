package ca.budgetbuddy.service;

import budgetbuddy.model.Transaction;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads bank CSV exports into Transaction objects.
 * Commit 2 supports the generic layout: date, description, amount.
 */
public class ImportService {
    private static final String GENERIC = "generic";
    private static final DateTimeFormatter GENERIC_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Imports a CSV file. {@code bank} selects the column layout.
     * Only {@code generic} is supported in this commit.
     */
    public ImportResult importFile(Path file, String bank) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        if (!isGeneric(bank)) {
            throw new IllegalArgumentException("Unsupported bank format: " + bank);
        }
        return readGeneric(file);
    }

    private boolean isGeneric(String bank) {
        if (bank == null) {
            return false;
        }
        return GENERIC.equalsIgnoreCase(bank);
    }

    private ImportResult readGeneric(Path file) {
        List<Transaction> transactions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVReaderHeaderAware csv = new CSVReaderHeaderAware(reader)) {
            int rowNumber = 1;
            Map<String, String> row = csv.readMap();
            while (row != null) {
                rowNumber++;
                parseGenericRow(row, rowNumber, transactions, errors);
                row = csv.readMap();
            }
        } catch (IOException | CsvValidationException e) {
            throw new IllegalArgumentException("Could not read CSV file: " + file, e);
        }
        return new ImportResult(transactions, errors);
    }

    private void parseGenericRow(Map<String, String> row, int rowNumber,
                                 List<Transaction> transactions, List<String> errors) {
        String dateText = cell(row, "date");
        String description = cell(row, "description");
        String amountText = cell(row, "amount");

        LocalDate date = parseDate(dateText);
        if (date == null) {
            errors.add("Row " + rowNumber + ": invalid date '" + dateText + "'");
            return;
        }

        BigDecimal amount = parseAmount(amountText);
        if (amount == null) {
            errors.add("Row " + rowNumber + ": invalid amount '" + amountText + "'");
            return;
        }

        transactions.add(new Transaction(date, description, amount));
    }

    private static String cell(Map<String, String> row, String column) {
        String value = row.get(column);
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static LocalDate parseDate(String text) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text, GENERIC_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static BigDecimal parseAmount(String text) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
