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
import java.util.Locale;
import java.util.Map;

/**
 * Reads bank CSV exports into Transaction objects.
 * Supports generic (date, description, amount), TD, and RBC layouts.
 */
public class ImportService {
    private static final String GENERIC = "generic";
    private static final String TD = "td";
    private static final String RBC = "rbc";

    private static final DateTimeFormatter GENERIC_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter BANK_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    public ImportResult importFile(Path file, String bank) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        String format = normalizeBank(bank);
        if (GENERIC.equals(format)) {
            return readCsv(file, this::parseGenericRow);
        }
        if (TD.equals(format)) {
            return readCsv(file, this::parseTdRow);
        }
        if (RBC.equals(format)) {
            return readCsv(file, this::parseRbcRow);
        }
        throw new IllegalArgumentException("Unsupported bank format: " + bank);
    }

    private String normalizeBank(String bank) {
        if (bank == null) {
            return "";
        }
        return bank.trim().toLowerCase(Locale.ROOT);
    }

    private ImportResult readCsv(Path file, RowParser parser) {
        List<Transaction> transactions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVReaderHeaderAware csv = new CSVReaderHeaderAware(reader)) {
            int rowNumber = 1;
            Map<String, String> row = csv.readMap();
            while (row != null) {
                rowNumber++;
                parser.parse(row, rowNumber, transactions, errors);
                row = csv.readMap();
            }
        } catch (IOException | CsvValidationException e) {
            throw new IllegalArgumentException("Could not read CSV file: " + file, e);
        }
        return new ImportResult(transactions, errors);
    }

    private void parseGenericRow(Map<String, String> row, int rowNumber,
                                 List<Transaction> transactions, List<String> errors) {
        addTransaction(
                rowNumber,
                cell(row, "date"),
                cell(row, "description"),
                cell(row, "amount"),
                GENERIC_DATE,
                transactions,
                errors
        );
    }

    /**
     * TD EasyWeb spreadsheet layout:
     * Date, Description, Withdrawals, Deposits, Balance.
     * Dates are month-first (M/d/yyyy).
     */
    private void parseTdRow(Map<String, String> row, int rowNumber,
                            List<Transaction> transactions, List<String> errors) {
        String amountText = cell(row, "Withdrawals");
        if (amountText.isEmpty()) {
            amountText = cell(row, "Deposits");
        }
        addTransaction(
                rowNumber,
                cell(row, "Date"),
                cell(row, "Description"),
                amountText,
                BANK_DATE,
                transactions,
                errors
        );
    }

    /**
     * RBC download layout:
     * Account Type, Account Number, Transaction Date, Cheque Number,
     * Description 1, Description 2, CAD$, USD$.
     * Dates are month-first (M/d/yyyy). Amount comes from CAD$, or USD$ if CAD$ is empty.
     */
    private void parseRbcRow(Map<String, String> row, int rowNumber,
                             List<Transaction> transactions, List<String> errors) {
        String amountText = cell(row, "CAD$");
        if (amountText.isEmpty()) {
            amountText = cell(row, "USD$");
        }
        addTransaction(
                rowNumber,
                cell(row, "Transaction Date"),
                rbcMerchant(row),
                amountText,
                BANK_DATE,
                transactions,
                errors
        );
    }

    private static String rbcMerchant(Map<String, String> row) {
        String description1 = cell(row, "Description 1");
        String description2 = cell(row, "Description 2");
        if (description1.isEmpty()) {
            return description2;
        }
        if (description2.isEmpty()) {
            return description1;
        }
        return description1 + " " + description2;
    }

    private void addTransaction(int rowNumber, String dateText, String merchant, String amountText,
                                DateTimeFormatter dateFormat, List<Transaction> transactions,
                                List<String> errors) {
        LocalDate date = parseDate(dateText, dateFormat);
        if (date == null) {
            errors.add("Row " + rowNumber + ": invalid date '" + dateText + "'");
            return;
        }

        BigDecimal amount = parseAmount(amountText);
        if (amount == null) {
            errors.add("Row " + rowNumber + ": invalid amount '" + amountText + "'");
            return;
        }

        transactions.add(new Transaction(date, merchant, amount));
    }

    private static String cell(Map<String, String> row, String column) {
        for (Map.Entry<String, String> entry : row.entrySet()) {
            String header = entry.getKey();
            if (header != null && header.trim().equalsIgnoreCase(column)) {
                String value = entry.getValue();
                if (value == null) {
                    return "";
                }
                return value.trim();
            }
        }
        return "";
    }

    private static LocalDate parseDate(String text, DateTimeFormatter formatter) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text, formatter);
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

    @FunctionalInterface
    private interface RowParser {
        void parse(Map<String, String> row, int rowNumber,
                   List<Transaction> transactions, List<String> errors);
    }
}
