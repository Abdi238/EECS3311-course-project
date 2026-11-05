package ca.budgetbuddy.service;

import ca.budgetbuddy.model.SavingsGoal;
import ca.budgetbuddy.persistence.GoalRepository;

import java.util.Objects;

/**
 * Loads savings goals for the saving plan. The facade is the only caller.
 */
public final class GoalService {
    private final GoalRepository goals;

    public GoalService(GoalRepository goals) {
        this.goals = Objects.requireNonNull(goals, "goals");
    }

    public SavingsGoal getGoal(long goalId) {
        return goals.findById(goalId);
    }
}
