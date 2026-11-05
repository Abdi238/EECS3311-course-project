package ca.budgetbuddy.llm;

/**
 * Placeholder for a tool the model can request by name.
 * Concrete tools are added in a later commit.
 */
public interface AgentTool {
    String getName();

    String getDescription();
}
