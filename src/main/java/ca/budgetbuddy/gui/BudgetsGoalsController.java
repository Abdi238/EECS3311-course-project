package ca.budgetbuddy.gui;

import ca.budgetbuddy.BudgetBuddyFacade;
import ca.budgetbuddy.model.Budget;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.SavingsGoal;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.List;
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
    @FXML
    private TableView<SavingsGoal> goalTable;
    @FXML
    private TableColumn<SavingsGoal, String> goalNameColumn;
    @FXML
    private TableColumn<SavingsGoal, String> goalTargetColumn;
    @FXML
    private TableColumn<SavingsGoal, String> goalSavedColumn;
    @FXML
    private TableColumn<SavingsGoal, String> goalDeadlineColumn;

    private BudgetBuddyFacade facade;
    private Consumer<String> banner = message -> {
    };
    private GuiBudgetAlertListener alerts;

    @FXML
    private void initialize() {
        budgetCategory.getItems().addAll(Category.values());
        budgetCategory.setValue(Category.DINING);
        budgetMonthColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMonth().toString()));
        budgetCategoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategory().name()));
        budgetLimitColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLimit().toPlainString()));
        budgetSpentColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSpent().toPlainString()));
        goalNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        goalTargetColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTarget().toPlainString()));
        goalSavedColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSaved().toPlainString()));
        goalDeadlineColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDeadline().toString()));
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

    public void setFacade(BudgetBuddyFacade facade, Consumer<String> banner, GuiBudgetAlertListener alerts) {
        this.facade = facade;
        this.banner = banner;
        this.alerts = alerts;
        facade.addBudgetAlertListener(alerts);
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

    @FXML
    private void onGeneratePlan() {
        UiFeedback.run(this::generatePlan, banner);
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
        banner.accept(alerts.combine(BudgetsGoalsLogic.savedBudgetMessage(month, category.name())));
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

    private void generatePlan() {
        if (facade == null) {
            return;
        }
        SavingsGoal selected = goalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            banner.accept("Choose a savings goal.");
            return;
        }
        String plan = facade.generateSavingPlan(selected.getId());
        showPlan(selected.getName(), plan);
    }

    private void showPlan(String name, String plan) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Saving plan");
        dialog.setHeaderText(name);
        TextArea area = new TextArea(plan);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(480, 220);
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
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
        SavingsGoal selectedRow = goalTable.getSelectionModel().getSelectedItem();
        long selectedRowId = -1;
        if (selectedRow != null) {
            selectedRowId = selectedRow.getId();
        }
        List<SavingsGoal> loaded = facade.getGoals();
        contributionGoal.getItems().setAll(loaded);
        goalTable.getItems().setAll(loaded);
        if (selected != null) {
            for (SavingsGoal goal : contributionGoal.getItems()) {
                if (goal.getId() == selected.getId()) {
                    contributionGoal.setValue(goal);
                    break;
                }
            }
        }
        if (selectedRowId < 0) {
            return;
        }
        for (SavingsGoal goal : goalTable.getItems()) {
            if (goal.getId() == selectedRowId) {
                goalTable.getSelectionModel().select(goal);
                return;
            }
        }
    }
}
