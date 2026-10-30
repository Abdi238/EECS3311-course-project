package budgetbuddy.persistence;

import budgetbuddy.model.SavingsGoal;

public interface GoalRepository {
    SavingsGoal save(SavingsGoal goal);

    SavingsGoal findById(long goalId);
}
