package ca.budgetbuddy.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsonReplyTest {
    @Test
    void unwrapRemovesJsonFence() {
        assertEquals(
                "[{\"index\":0}]",
                JsonReply.unwrap("""
                        ```json
                        [{"index":0}]
                        ```
                        """));
    }

    @Test
    void unwrapLeavesPlainJsonAlone() {
        assertEquals("[{\"index\":0}]", JsonReply.unwrap("  [{\"index\":0}]  "));
    }

    @Test
    void unwrapNullStaysNull() {
        assertNull(JsonReply.unwrap(null));
    }
}
