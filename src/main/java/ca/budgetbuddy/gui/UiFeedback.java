package ca.budgetbuddy.gui;

import java.util.function.Consumer;

/**
 * Shows a facade or input failure on the banner instead of letting it crash the window.
 */
public final class UiFeedback {
    private UiFeedback() {
    }

    public static void run(Runnable action, Consumer<String> banner) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = "The request failed.";
            }
            banner.accept(message);
        }
    }
}
