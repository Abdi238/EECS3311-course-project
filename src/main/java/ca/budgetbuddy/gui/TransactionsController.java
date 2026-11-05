package ca.budgetbuddy.gui;

import ca.budgetbuddy.BudgetBuddyFacade;
import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.model.Transaction;
import ca.budgetbuddy.service.ImportResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.function.Consumer;

/**
 * Transactions tab. Reads the form and calls the facade.
 */
public class TransactionsController {
    @FXML
    private ComboBox<String> bankBox;
    @FXML
    private TableView<Transaction> transactionTable;
    @FXML
    private TableColumn<Transaction, String> dateColumn;
    @FXML
    private TableColumn<Transaction, String> merchantColumn;
    @FXML
    private TableColumn<Transaction, String> amountColumn;
    @FXML
    private TableColumn<Transaction, String> categoryColumn;

    private BudgetBuddyFacade facade;
    private Consumer<String> banner = message -> {
    };
    private GuiBudgetAlertListener alerts;

    @FXML
    private void initialize() {
        bankBox.getItems().addAll("TD", "RBC", "GENERIC");
        bankBox.setValue("GENERIC");
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDate().toString()));
        merchantColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMerchant()));
        amountColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAmount().toPlainString()));
        categoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategory().name()));
        transactionTable.setOnMouseClicked(event -> {
            var cell = transactionTable.getFocusModel().getFocusedCell();
            boolean categoryColumnClicked = false;
            if (cell != null && cell.getTableColumn() == categoryColumn) {
                categoryColumnClicked = true;
            }
            if (!TransactionsLogic.shouldEditCategory(event.getClickCount(), categoryColumnClicked)) {
                return;
            }
            Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            editCategory(selected);
        });
    }

    public void setFacade(BudgetBuddyFacade facade, Consumer<String> banner, GuiBudgetAlertListener alerts) {
        this.facade = facade;
        this.banner = banner;
        this.alerts = alerts;
        reload();
    }

    @FXML
    private void onImport() {
        UiFeedback.run(this::importFile, banner);
    }

    @FXML
    private void onCategorize() {
        UiFeedback.run(this::categorize, banner);
    }

    private void importFile() {
        if (facade == null) {
            return;
        }
        String bank = bankBox.getValue();
        if (bank == null || bank.isBlank()) {
            banner.accept("Choose a bank: TD, RBC, or GENERIC.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import bank CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showOpenDialog(transactionTable.getScene().getWindow());
        if (file == null) {
            return;
        }
        ImportResult result = facade.importTransactions(file.toPath(), bank);
        banner.accept(alerts.combine(TransactionsLogic.importMessage(result)));
        reload();
    }

    private void categorize() {
        if (facade == null) {
            return;
        }
        int updated = facade.categorizeUncategorized();
        String message = TransactionsLogic.categorizeMessage(updated);
        String notice = facade.aiCategorizationNotice();
        if (!notice.isBlank()) {
            message = message + " " + notice + ".";
        }
        banner.accept(alerts.combine(message));
        reload();
    }

    private void editCategory(Transaction tx) {
        Dialog<CategoryEdit> dialog = new Dialog<>();
        dialog.setTitle("Change category");
        dialog.setHeaderText(tx.getMerchant());
        ChoiceBox<Category> choices = new ChoiceBox<>();
        choices.getItems().addAll(Category.values());
        choices.setValue(tx.getCategory());
        CheckBox remember = new CheckBox("remember this merchant");
        VBox content = new VBox(8, new Label("Category"), choices, remember);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            return new CategoryEdit(choices.getValue(), remember.isSelected());
        });
        dialog.showAndWait().ifPresent(edit -> applyCategory(tx, edit));
    }

    private void applyCategory(Transaction tx, CategoryEdit edit) {
        UiFeedback.run(() -> {
            if (facade == null) {
                return;
            }
            if (edit.category() == null) {
                banner.accept("Choose a category.");
                return;
            }
            facade.correctCategory(tx.getId(), edit.category(), edit.remember());
            banner.accept(alerts.combine(TransactionsLogic.correctionMessage(
                    tx.getId(), edit.category(), edit.remember(), tx.getMerchant())));
            reload();
        }, banner);
    }

    private void reload() {
        if (facade == null) {
            return;
        }
        transactionTable.getItems().setAll(facade.getTransactions());
    }

    private record CategoryEdit(Category category, boolean remember) {
    }
}
