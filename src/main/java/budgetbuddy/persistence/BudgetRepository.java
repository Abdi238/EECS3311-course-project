package budgetbuddy.persistence;

import budgetbuddy.model.Budget;

import java.time.YearMonth;
import java.util.List;

public interface BudgetRepository {
    Budget save(Budget budget);

    void delete(long budgetId);

    List<Budget> findByMonth(YearMonth month);
}
