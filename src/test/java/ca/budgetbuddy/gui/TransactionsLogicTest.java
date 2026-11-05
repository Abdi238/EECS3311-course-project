package ca.budgetbuddy.gui;

import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.service.ImportResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionsLogicTest {

    @Test
    void importMessageCountsRowsAndSkippedDuplicates() {
        ImportResult result = new ImportResult(
                List.of(new Transaction(LocalDate.of(2026, 10, 1), "Sample Grocery Store", new BigDecimal("12.50"))),
                List.of(),
                List.of("Row 2: duplicate skipped", "Row 3: duplicate skipped"));

        assertEquals("Imported 1 transaction(s). Skipped duplicates: 2.", TransactionsLogic.importMessage(result));
    }

    @Test
    void categorizeAndCorrectionMessagesStayOnTheScreenText() {
        assertEquals("Categorized 2 transaction(s).", TransactionsLogic.categorizeMessage(2));
        assertEquals(
                "Transaction 4 set to DINING. Remembered Campus Coffee Shop.",
                TransactionsLogic.correctionMessage(4, Category.DINING, true, "Campus Coffee Shop"));
        assertEquals(
                "Transaction 4 set to DINING.",
                TransactionsLogic.correctionMessage(4, Category.DINING, false, "Campus Coffee Shop"));
    }

    @Test
    void onlyADoubleClickOnTheCategoryColumnOpensTheEditor() {
        assertTrue(TransactionsLogic.shouldEditCategory(2, true));
        assertFalse(TransactionsLogic.shouldEditCategory(1, true));
        assertFalse(TransactionsLogic.shouldEditCategory(2, false));
    }
}
