package ca.budgetbuddy.gui;

import ca.budgetbuddy.BudgetBuddyFacade;
import ca.budgetbuddy.model.Anomaly;
import ca.budgetbuddy.model.MonthlySummary;
import ca.budgetbuddy.model.RecurringPayment;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * Insights tab. Reads the forms and calls the facade.
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
    @FXML
    private TableView<RecurringPayment> recurringTable;
    @FXML
    private TableColumn<RecurringPayment, String> recurringMerchantColumn;
    @FXML
    private TableColumn<RecurringPayment, String> recurringAmountColumn;
    @FXML
    private TableColumn<RecurringPayment, String> recurringFrequencyColumn;
    @FXML
    private TableColumn<RecurringPayment, String> recurringNextColumn;
    @FXML
    private TableColumn<RecurringPayment, String> recurringNameColumn;
    @FXML
    private TableColumn<RecurringPayment, String> recurringSubscriptionColumn;
    @FXML
    private TextField unusualMonth;
    @FXML
    private TableView<Anomaly> unusualTable;
    @FXML
    private TableColumn<Anomaly, String> unusualDateColumn;
    @FXML
    private TableColumn<Anomaly, String> unusualMerchantColumn;
    @FXML
    private TableColumn<Anomaly, String> unusualAmountColumn;
    @FXML
    private TableColumn<Anomaly, String> unusualReasonColumn;
    @FXML
    private TableColumn<Anomaly, String> unusualExplanationColumn;
    @FXML
    private TextField summaryMonth;
    @FXML
    private Label summaryText;

    private BudgetBuddyFacade facade;
    private Consumer<String> banner = message -> {
    };

    @FXML
    private void initialize() {
        monthColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMonth().toString()));
        incomeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getIncome().toPlainString()));
        spentColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSpent().toPlainString()));
        recurringMerchantColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchant()));
        recurringAmountColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAmount().toPlainString()));
        recurringFrequencyColumn.setCellValueFactory(cell -> new SimpleStringProperty(textOrEmpty(cell.getValue().getFrequency())));
        recurringNextColumn.setCellValueFactory(cell -> new SimpleStringProperty(dateText(cell.getValue().getNextExpectedDate())));
        recurringNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(textOrEmpty(cell.getValue().getDisplayName())));
        recurringSubscriptionColumn.setCellValueFactory(cell -> new SimpleStringProperty(Boolean.toString(cell.getValue().isSubscription())));
        unusualDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTransaction().getDate().toString()));
        unusualMerchantColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTransaction().getMerchant()));
        unusualAmountColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTransaction().getAmount().toPlainString()));
        unusualReasonColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getReason()));
        unusualExplanationColumn.setCellValueFactory(cell -> new SimpleStringProperty(textOrEmpty(cell.getValue().getExplanation())));
    }

    public void setFacade(BudgetBuddyFacade facade, Consumer<String> banner) {
        this.facade = facade;
        this.banner = banner;
    }

    @FXML
    private void onShowTrends() {
        UiFeedback.run(this::showTrends, banner);
    }

    @FXML
    private void onScanRecurring() {
        UiFeedback.run(this::scanRecurring, banner);
    }

    @FXML
    private void onScanUnusual() {
        UiFeedback.run(this::scanUnusual, banner);
    }

    @FXML
    private void onGenerateSummary() {
        UiFeedback.run(this::generateSummary, banner);
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

    private void scanRecurring() {
        if (facade == null) {
            return;
        }
        recurringTable.getItems().setAll(facade.findRecurringPayments());
        banner.accept(facade.recurringDataWarning());
    }

    private void scanUnusual() {
        if (facade == null) {
            return;
        }
        unusualTable.getItems().setAll(facade.findUnusualExpenses(BudgetsGoalsLogic.parseMonth(unusualMonth.getText())));
        banner.accept("");
    }

    private void generateSummary() {
        if (facade == null) {
            return;
        }
        MonthlySummary summary = facade.generateMonthlySummary(BudgetsGoalsLogic.parseMonth(summaryMonth.getText()));
        summaryText.setText(InsightsLogic.summaryText(summary));
        banner.accept("");
    }

    private static String textOrEmpty(String text) {
        if (text == null) {
            return "";
        }
        return text;
    }

    private static String dateText(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.toString();
    }
}
