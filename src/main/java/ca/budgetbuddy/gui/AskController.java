package ca.budgetbuddy.gui;

import ca.budgetbuddy.BudgetBuddyFacade;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.function.Consumer;

/**
 * Ask tab. Sends the question to the facade and shows the answer and tools used.
 */
public class AskController {
    @FXML
    private TextField questionField;
    @FXML
    private TextArea answerArea;
    @FXML
    private Label toolsLabel;

    private BudgetBuddyFacade facade;
    private Consumer<String> banner = message -> {
    };

    public void setFacade(BudgetBuddyFacade facade, Consumer<String> banner) {
        this.facade = facade;
        this.banner = banner;
    }

    @FXML
    private void onSend() {
        if (facade == null) {
            return;
        }
        String question = questionField.getText();
        if (question == null || question.isBlank()) {
            banner.accept("Enter a question.");
            return;
        }
        try {
            String answer = facade.ask(question);
            answerArea.setText(answer);
            toolsLabel.setText(AskLogic.toolsLine(facade.getLastToolsUsed()));
            banner.accept(AskLogic.offlineBanner(answer));
        } catch (RuntimeException ex) {
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = "The request failed.";
            }
            answerArea.setText(message);
            toolsLabel.setText(AskLogic.toolsLine(List.of()));
            banner.accept(message);
        }
    }
}
