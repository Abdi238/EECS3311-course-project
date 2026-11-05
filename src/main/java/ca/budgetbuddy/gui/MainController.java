package ca.budgetbuddy.gui;

import ca.budgetbuddy.BudgetBuddyFacade;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Owns the window shell and the banner. Tab controllers do the rest.
 */
public class MainController {
    @FXML
    private Label banner;
    @FXML
    private TransactionsController transactionsController;
    @FXML
    private BudgetsGoalsController budgetsGoalsController;
    @FXML
    private InsightsController insightsController;
    @FXML
    private AskController askController;

    public void setFacade(BudgetBuddyFacade facade) {
        GuiBudgetAlertListener alerts = new GuiBudgetAlertListener(this::showBanner);
        transactionsController.setFacade(facade, this::showBanner, alerts);
        budgetsGoalsController.setFacade(facade, this::showBanner, alerts);
        insightsController.setFacade(facade, this::showBanner);
        askController.setFacade(facade, this::showBanner);
    }

    public void showBanner(String message) {
        if (message == null || message.isBlank()) {
            banner.setText("");
            banner.setVisible(false);
            banner.setManaged(false);
            return;
        }
        banner.setText(message);
        banner.setVisible(true);
        banner.setManaged(true);
    }
}
