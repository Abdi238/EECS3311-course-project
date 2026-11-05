package ca.budgetbuddy.service;

import ca.budgetbuddy.model.Budget;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.CategorySource;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.persistence.Database;
import ca.budgetbuddy.persistence.SqliteBudgetRepository;
import ca.budgetbuddy.persistence.SqliteTransactionRepository;
import ca.budgetbuddy.persistence.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetServiceTest {
    private static final YearMonth MONTH = YearMonth.of(2026, 10);

    private Database database;
    private TransactionRepository transactions;
    private BudgetService service;

    @BeforeEach
    void setUp() {
        database = Database.inMemory();
        transactions = new SqliteTransactionRepository(database);
        service = new BudgetService(new SqliteBudgetRepository(database), new SpendingCalculator(transactions));
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void alertFiresOnceAt80ThenAgainAt100AndDoesNotRepeat() {
        List<String> alerts = new ArrayList<>();
        service.addListener(alerts::add);
        service.saveBudget(Category.DINING, new BigDecimal("100.00"), MONTH);
        assertTrue(alerts.isEmpty());

        saveDining("Campus Coffee Shop", "80.00", 2);
        service.recalculate(MONTH);

        assertEquals(List.of("Dining budget at 80%"), alerts);
        assertEquals(80, service.getBudgets(MONTH).get(0).getLastAlertPercent());

        service.recalculate(MONTH);
        assertEquals(1, alerts.size());

        saveDining("Campus Coffee Shop", "20.00", 3);
        service.recalculate(MONTH);

        assertEquals(List.of("Dining budget at 80%", "Dining budget at 100%"), alerts);
        Budget reloaded = service.getBudgets(MONTH).get(0);
        assertEquals(100, reloaded.getLastAlertPercent());
        assertEquals(0, new BigDecimal("100.00").compareTo(reloaded.getSpent()));

        service.recalculate(MONTH);
        assertEquals(2, alerts.size());
    }

    private void saveDining(String merchant, String amount, int day) {
        Transaction tx = new Transaction(LocalDate.of(2026, 10, day), merchant, new BigDecimal(amount));
        tx.setCategory(Category.DINING, CategorySource.USER, 1.0);
        transactions.saveAll(List.of(tx));
    }
}
