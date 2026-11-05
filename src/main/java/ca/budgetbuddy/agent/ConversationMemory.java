package ca.budgetbuddy.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ordered turns for one conversation. Each entry is stored as {@code role: content}
 * so the model can tell a user question from a tool result.
 */
public final class ConversationMemory {
    private final List<String> turns = new ArrayList<>();

    public void add(String role, String content) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(content, "content");
        turns.add(role + ": " + content);
    }

    public List<String> recent() {
        return List.copyOf(turns);
    }
}
