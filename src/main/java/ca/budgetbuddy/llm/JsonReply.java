package ca.budgetbuddy.llm;

/**
 * Helpers for model replies that are supposed to be JSON but sometimes arrive
 * wrapped in Markdown code fences.
 */
public final class JsonReply {
    private JsonReply() {
    }

    /**
     * Returns the text with a leading {@code ```}/{@code ```json} fence and a
     * trailing fence removed when present. Otherwise returns the trimmed text.
     */
    public static String unwrap(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline < 0) {
            return trimmed;
        }
        String body = trimmed.substring(firstNewline + 1);
        int closingFence = body.lastIndexOf("```");
        if (closingFence >= 0) {
            body = body.substring(0, closingFence);
        }
        return body.trim();
    }
}
