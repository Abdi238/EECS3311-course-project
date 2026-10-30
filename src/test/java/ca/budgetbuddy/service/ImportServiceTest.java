package ca.budgetbuddy.service;

import budgetbuddy.model.Transaction;
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

    @BeforeEach
    void setUp() {
        importService = new ImportService();
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
    void importFileRejectsUnsupportedBankFormat() {
        Path file = resource("csv/generic-valid.csv");
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> importService.importFile(file, "td")
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
