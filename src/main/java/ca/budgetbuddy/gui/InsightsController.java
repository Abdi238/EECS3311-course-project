package ca.budgetbuddy.gui;

import ca.budgetbuddy.BudgetBuddyFacade;
import ca.budgetbuddy.model.MonthlySummary;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

/**
 * Insights tab. Shows spending trends for a month range.
 */
public class InsightsController {
    @FXML
    private TextField fromMonth;
    @FXML
    private TextField toMonth;
    @FXML
    private TableView<MonthlySummary> trendTable;
    @FXML
    private TableColumn<MonthlySummary, String> monthColumn;
    @FXML
    private TableColumn<MonthlySummary, String> incomeColumn;
    @FXML
    private TableColumn<MonthlySummary, String> spentColumn;

    private BudgetBuddyFacade facade;
    private Consumer<String> banner = message -> {
    };

    @FXML
    private void initialize() {
        monthColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMonth().toString()));
        incomeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getIncome().toPlainString()));
        spentColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSpent().toPlainString()));
    }

    public void setFacade(BudgetBuddyFacade facade, Consumer<String> banner) {
        this.facade = facade;
        this.banner = banner;
    }

    @FXML
    private void onShowTrends() {
        UiFeedback.run(this::showTrends, banner);
    }

    private void showTrends() {
        if (facade == null) {
            return;
        }
        trendTable.getItems().setAll(facade.getSpendingTrends(
                BudgetsGoalsLogic.parseMonth(fromMonth.getText()),
                BudgetsGoalsLogic.parseMonth(toMonth.getText())));
        banner.accept("");
    }
}
