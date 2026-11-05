package ca.budgetbuddy.gui;

import ca.budgetbuddy.BudgetBuddyFacade;
import ca.budgetbuddy.llm.LlmConfigLoader;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;

/**
 * Opens the Budget Buddy window. The database file matches the CLI default.
 */
public class MainApp extends Application {
    private BudgetBuddyFacade facade;

    @Override
    public void start(Stage stage) throws Exception {
        facade = BudgetBuddyFacade.create(Path.of("budgetbuddy.db"), configuredApiKey());
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("main.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.setFacade(facade);
        stage.setTitle("Budget Buddy");
        stage.setScene(new Scene(root, 980, 680));
        stage.show();
    }

    @Override
    public void stop() {
        if (facade != null) {
            facade.close();
        }
    }

    private static String configuredApiKey() {
        try {
            return new LlmConfigLoader().load().getApiKey();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
