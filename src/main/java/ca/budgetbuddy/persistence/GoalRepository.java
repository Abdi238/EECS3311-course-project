package ca.budgetbuddy.persistence;

import ca.budgetbuddy.model.SavingsGoal;

import java.util.List;

public interface GoalRepository {
    SavingsGoal save(SavingsGoal goal);

    SavingsGoal findById(long goalId);

    List<SavingsGoal> findAll();
}
