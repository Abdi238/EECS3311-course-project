package ca.budgetbuddy.gui;

import ca.budgetbuddy.BudgetBuddyFacade;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AskLogicTest {

    @Test
    void offlineAnswerRaisesTheBannerAndToolsAreListed() {
        assertEquals(BudgetBuddyFacade.ASSISTANT_OFFLINE, AskLogic.offlineBanner("assistant offline"));
        assertEquals("", AskLogic.offlineBanner("Sample Grocery Store total is 12.50."));
        assertEquals("Tools used: none", AskLogic.toolsLine(List.of()));
        assertEquals("Tools used: SpendingTool", AskLogic.toolsLine(List.of("SpendingTool")));
    }
}
