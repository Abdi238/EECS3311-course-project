package ca.budgetbuddy.gui;

import ca.budgetbuddy.model.Category;
import ca.budgetbuddy.service.ImportResult;

/**
 * Text and click rules for the Transactions tab. No money rules and no JavaFX.
 */
public final class TransactionsLogic {
    private TransactionsLogic() {
    }

    public static String importMessage(ImportResult result) {
        StringBuilder text = new StringBuilder();
        text.append("Imported ").append(result.getTransactions().size()).append(" transaction(s).");
        if (!result.getSkippedDuplicates().isEmpty()) {
            text.append(" Skipped duplicates: ").append(result.getSkippedDuplicates().size()).append(".");
        }
        if (!result.getErrors().isEmpty()) {
            text.append(" Errors: ").append(result.getErrors().size()).append(".");
        }
        return text.toString();
    }

    public static String categorizeMessage(int updated) {
        return "Categorized " + updated + " transaction(s).";
    }

    public static String correctionMessage(long id, Category category, boolean remember, String merchant) {
        String message = "Transaction " + id + " set to " + category + ".";
        if (remember) {
            message = message + " Remembered " + merchant + ".";
        }
        return message;
    }

    public static boolean shouldEditCategory(int clickCount, boolean categoryColumn) {
        return clickCount == 2 && categoryColumn;
    }
}
