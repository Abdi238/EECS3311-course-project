package ca.budgetbuddy.gui;

import ca.budgetbuddy.BudgetBuddyFacade;

import java.util.List;

/**
 * Banner and tool-list text for the Ask tab.
 */
public final class AskLogic {
    private AskLogic() {
    }

    public static String offlineBanner(String answer) {
        if (BudgetBuddyFacade.ASSISTANT_OFFLINE.equals(answer)) {
            return BudgetBuddyFacade.ASSISTANT_OFFLINE;
        }
        return "";
    }

    public static String toolsLine(List<String> tools) {
        if (tools == null || tools.isEmpty()) {
            return "Tools used: none";
        }
        return "Tools used: " + String.join(", ", tools);
    }
}
