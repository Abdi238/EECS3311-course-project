package ca.budgetbuddy.service;

import budgetbuddy.model.Transaction;
import budgetbuddy.persistence.Database;
import budgetbuddy.persistence.SqliteTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportServiceTest {

    private ImportService importService;
    private Database database;

    @BeforeEach
    void setUp() {
        importService = new ImportService();
    }

    @AfterEach
    void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void importGenericCsvReturnsAllValidRows() {
        ImportResult result = importService.importFile(resource("csv/generic-valid.csv"), "generic");

        List<Transaction> txs = result.getTransactions();
        assertEquals(3, txs.size());
        assertTrue(result.getErrors().isEmpty());

        assertEquals(LocalDate.of(2026, 10, 1), txs.get(0).getDate());
        assertEquals("Sample Grocery Store", txs.get(0).getMerchant());
        assertEquals(new BigDecimal("54.21"), txs.get(0).getAmount());

        assertEquals("Campus Coffee Shop", txs.get(1).getMerchant());
        assertEquals(new BigDecimal("6.25"), txs.get(1).getAmount());

        assertEquals("City Transit", txs.get(2).getMerchant());
        assertEquals(new BigDecimal("3.30"), txs.get(2).getAmount());
    }

    @Test
    void importGenericCsvIsCaseInsensitiveForBankName() {
        ImportResult result = importService.importFile(resource("csv/generic-valid.csv"), "GENERIC");
        assertEquals(3, result.getTransactions().size());
    }

    @Test
    void importGenericCsvSkipsInvalidDateAndAmountRows() {
        ImportResult result = importService.importFile(resource("csv/generic-invalid-rows.csv"), "generic");

        List<Transaction> txs = result.getTransactions();
        assertEquals(2, txs.size());
        assertEquals("Campus Bookstore", txs.get(0).getMerchant());
        assertEquals("Community Gym", txs.get(1).getMerchant());

        List<String> errors = result.getErrors();
        assertEquals(4, errors.size());
        assertEquals("Row 3: invalid date 'not-a-date'", errors.get(0));
        assertEquals("Row 4: invalid amount 'abc'", errors.get(1));
        assertEquals("Row 6: invalid date '2026-13-40'", errors.get(2));
        assertEquals("Row 7: invalid amount ''", errors.get(3));
    }

    @Test
    void importGenericCsvWithOnlyHeaderReturnsEmptyLists() {
        ImportResult result = importService.importFile(resource("csv/generic-header-only.csv"), "generic");
        assertTrue(result.getTransactions().isEmpty());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void importTdCsvReturnsValidRows() {
        ImportResult result = importService.importFile(resource("csv/td-valid.csv"), "td");

        List<Transaction> txs = result.getTransactions();
        assertEquals(3, txs.size());
        assertTrue(result.getErrors().isEmpty());

        assertEquals(LocalDate.of(2026, 10, 1), txs.get(0).getDate());
        assertEquals("Sample Grocery Store", txs.get(0).getMerchant());
        assertEquals(new BigDecimal("54.21"), txs.get(0).getAmount());

        assertEquals("Campus Coffee Shop", txs.get(1).getMerchant());
        assertEquals(new BigDecimal("6.25"), txs.get(1).getAmount());

        assertEquals("Payroll Deposit", txs.get(2).getMerchant());
        assertEquals(new BigDecimal("1500.00"), txs.get(2).getAmount());
    }

    @Test
    void importTdCsvSkipsBadRows() {
        ImportResult result = importService.importFile(resource("csv/td-invalid-rows.csv"), "TD");

        List<Transaction> txs = result.getTransactions();
        assertEquals(2, txs.size());
        assertEquals("Campus Bookstore", txs.get(0).getMerchant());
        assertEquals("Community Gym", txs.get(1).getMerchant());

        List<String> errors = result.getErrors();
        assertEquals(2, errors.size());
        assertEquals("Row 3: invalid date 'not-a-date'", errors.get(0));
        assertEquals("Row 4: invalid amount 'abc'", errors.get(1));
    }

    @Test
    void importRbcCsvReturnsValidRows() {
        ImportResult result = importService.importFile(resource("csv/rbc-valid.csv"), "rbc");

        List<Transaction> txs = result.getTransactions();
        assertEquals(3, txs.size());
        assertTrue(result.getErrors().isEmpty());

        assertEquals(LocalDate.of(2026, 10, 1), txs.get(0).getDate());
        assertEquals("Purchase Sample Grocery Store", txs.get(0).getMerchant());
        assertEquals(new BigDecimal("-54.21"), txs.get(0).getAmount());

        assertEquals("Purchase Campus Coffee Shop", txs.get(1).getMerchant());
        assertEquals(new BigDecimal("-6.25"), txs.get(1).getAmount());

        assertEquals("Deposit Payroll Deposit", txs.get(2).getMerchant());
        assertEquals(new BigDecimal("1500.00"), txs.get(2).getAmount());
    }

    @Test
    void importRbcCsvSkipsBadRows() {
        ImportResult result = importService.importFile(resource("csv/rbc-invalid-rows.csv"), "RBC");

        List<Transaction> txs = result.getTransactions();
        assertEquals(2, txs.size());
        assertEquals("Purchase Campus Bookstore", txs.get(0).getMerchant());
        assertEquals("Purchase Community Gym", txs.get(1).getMerchant());

        List<String> errors = result.getErrors();
        assertEquals(2, errors.size());
        assertEquals("Row 3: invalid date 'not-a-date'", errors.get(0));
        assertEquals("Row 4: invalid amount 'abc'", errors.get(1));
    }

    @Test
    void importSkipsDuplicateRowsInTheSameFile() {
        ImportResult result = importService.importFile(resource("csv/generic-duplicates.csv"), "generic");

        assertEquals(2, result.getTransactions().size());
        assertEquals("Sample Grocery Store", result.getTransactions().get(0).getMerchant());
        assertEquals("Campus Coffee Shop", result.getTransactions().get(1).getMerchant());
        assertEquals(1, result.getSkippedDuplicates().size());
        assertEquals("Row 3: duplicate skipped", result.getSkippedDuplicates().get(0));
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void importSkipsRowsAlreadyStoredInTheDatabase() {
        database = Database.inMemory();
        SqliteTransactionRepository repository = new SqliteTransactionRepository(database);
        ImportService importing = new ImportService(repository);

        ImportResult first = importing.importFile(resource("csv/generic-valid.csv"), "generic");
        repository.saveAll(first.getTransactions());

        ImportResult second = importing.importFile(resource("csv/generic-valid.csv"), "generic");
        assertTrue(second.getTransactions().isEmpty());
        assertEquals(3, second.getSkippedDuplicates().size());
        assertEquals("Row 2: duplicate skipped", second.getSkippedDuplicates().get(0));
        assertEquals("Row 3: duplicate skipped", second.getSkippedDuplicates().get(1));
        assertEquals("Row 4: duplicate skipped", second.getSkippedDuplicates().get(2));
    }

    @Test
    void importFileRejectsUnsupportedBankFormat() {
        Path file = resource("csv/generic-valid.csv");
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> importService.importFile(file, "bmo")
        );
        assertTrue(thrown.getMessage().contains("Unsupported bank format"));
    }

    @Test
    void importFileRejectsNullBank() {
        Path file = resource("csv/generic-valid.csv");
        assertThrows(IllegalArgumentException.class, () -> importService.importFile(file, null));
    }

    private Path resource(String name) {
        URL url = ImportServiceTest.class.getClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalStateException("Missing test resource: " + name);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Bad test resource URI: " + name, e);
        }
    }
}
