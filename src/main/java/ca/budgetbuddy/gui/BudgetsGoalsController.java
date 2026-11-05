package ca.budgetbuddy.gui;

import ca.budgetbuddy.BudgetBuddyFacade;
import ca.budgetbuddy.model.Budget;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.SavingsGoal;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.function.Consumer;

/**
 * Budgets and goals tab. Reads the forms and calls the facade.
 */
public class BudgetsGoalsController {
    @FXML
    private ChoiceBox<Category> budgetCategory;
    @FXML
    private TextField budgetAmount;
    @FXML
    private TextField budgetMonth;
    @FXML
    private TableView<Budget> budgetTable;
    @FXML
    private TableColumn<Budget, String> budgetMonthColumn;
    @FXML
    private TableColumn<Budget, String> budgetCategoryColumn;
    @FXML
    private TableColumn<Budget, String> budgetLimitColumn;
    @FXML
    private TableColumn<Budget, String> budgetSpentColumn;
    @FXML
    private TextField goalName;
    @FXML
    private TextField goalTarget;
    @FXML
    private TextField goalDeadline;
    @FXML
    private ChoiceBox<SavingsGoal> contributionGoal;
    @FXML
    private TextField contributionAmount;

    private BudgetBuddyFacade facade;
    private Consumer<String> banner = message -> {
    };

    @FXML
    private void initialize() {
        budgetCategory.getItems().addAll(Category.values());
        budgetCategory.setValue(Category.DINING);
        budgetMonthColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMonth().toString()));
        budgetCategoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategory().name()));
        budgetLimitColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLimit().toPlainString()));
        budgetSpentColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSpent().toPlainString()));
        contributionGoal.setConverter(new StringConverter<>() {
            @Override
            public String toString(SavingsGoal goal) {
                if (goal == null) {
                    return "";
                }
                return BudgetsGoalsLogic.goalLabel(goal);
            }

            @Override
            public SavingsGoal fromString(String text) {
                return null;
            }
        });
    }

    public void setFacade(BudgetBuddyFacade facade, Consumer<String> banner) {
        this.facade = facade;
        this.banner = banner;
        reload();
    }

    @FXML
    private void onSaveBudget() {
        UiFeedback.run(this::saveBudget, banner);
    }

    @FXML
    private void onCreateGoal() {
        UiFeedback.run(this::createGoal, banner);
    }

    @FXML
    private void onAddContribution() {
        UiFeedback.run(this::addContribution, banner);
    }

    private void saveBudget() {
        if (facade == null) {
            return;
        }
        Category category = budgetCategory.getValue();
        if (category == null) {
            banner.accept("Choose a budget category.");
            return;
        }
        var month = BudgetsGoalsLogic.parseMonth(budgetMonth.getText());
        facade.saveBudget(category, BudgetsGoalsLogic.parseAmount(budgetAmount.getText()), month);
        banner.accept(BudgetsGoalsLogic.savedBudgetMessage(month, category.name()));
        reloadBudgets();
    }

    private void createGoal() {
        if (facade == null) {
            return;
        }
        SavingsGoal saved = facade.createGoal(
                goalName.getText(),
                BudgetsGoalsLogic.parseAmount(goalTarget.getText()),
                BudgetsGoalsLogic.parseDate(goalDeadline.getText()));
        banner.accept(BudgetsGoalsLogic.savedGoalMessage(saved));
        reloadGoals();
    }

    private void addContribution() {
        if (facade == null) {
            return;
        }
        SavingsGoal selected = contributionGoal.getValue();
        if (selected == null) {
            banner.accept("Choose a savings goal.");
            return;
        }
        SavingsGoal updated = facade.addContribution(
                selected.getId(), BudgetsGoalsLogic.parseAmount(contributionAmount.getText()));
        banner.accept(BudgetsGoalsLogic.contributionMessage(updated));
        reloadGoals();
    }

    private void reload() {
        reloadBudgets();
        reloadGoals();
    }

    private void reloadBudgets() {
        if (facade == null || budgetMonth.getText() == null || budgetMonth.getText().isBlank()) {
            return;
        }
        UiFeedback.run(() -> budgetTable.getItems().setAll(
                facade.getBudgets(BudgetsGoalsLogic.parseMonth(budgetMonth.getText()))), banner);
    }

    private void reloadGoals() {
        if (facade == null) {
            return;
        }
        SavingsGoal selected = contributionGoal.getValue();
        contributionGoal.getItems().setAll(facade.getGoals());
        if (selected == null) {
            return;
        }
        for (SavingsGoal goal : contributionGoal.getItems()) {
            if (goal.getId() == selected.getId()) {
                contributionGoal.setValue(goal);
                return;
            }
        }
    }
}
