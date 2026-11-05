package ca.budgetbuddy.persistence;

import ca.budgetbuddy.model.CategoryRule;

import java.util.List;

public interface CategoryRuleRepository {
    List<CategoryRule> findAll();

    void save(CategoryRule rule);
}
