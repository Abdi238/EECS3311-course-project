---
title: "Budget Buddy: Stage 2 Implementation Report"
subtitle: "EECS3311 Course Project, Fall 2026"
author: "Mohamed Abdi"
---

# 1. Project Overview and Implementation Status

Budget Buddy is a personal finance assistant for a single user. The user imports bank CSV exports (TD, RBC, or a generic layout), and the application categorizes transactions, tracks monthly budgets and savings goals, reports spending trends, and answers questions in plain language. It runs as a JavaFX desktop GUI and as a picocli command-line interface. Both interfaces call the same `BudgetBuddyFacade`. Data is stored locally in SQLite.

The AI component is Claude, accessed through the Anthropic API via `ClaudeAdapter`. The LLM is used in three ways:

- **Categorization (F02):** the LLM labels transactions that the merchant rules do not cover, using a JSON response that is validated before use.
- **Agent behavior (F09, F11):** a tool-using agent loop (`BudgetBuddyAgent`) lets the LLM call spending, budget, and goal tools over several steps, with conversation memory, to answer questions and write a saving plan.
- **Explanation and labeling (F07, F08, F10):** the LLM names recurring subscriptions, explains unusual expenses, and writes the monthly recap. Java computes the underlying facts first.

If no API key is configured, the application runs with rules-only categorization and an offline assistant. Mock and offline paths are covered by tests.

**Implementation status:** all eleven Stage 1 features (F01 to F11) are implemented and reachable from both the GUI and the CLI.

**Limitations and incomplete work:**

- The full deterministic test suite and KUMA agent validation are Stage 3 work. Stage 2 includes only basic verification (see Section 6).
- The AI-assisted parts depend on a live API key. Output is not guaranteed to be identical between runs.
- Import supports only the TD, RBC, and generic CSV layouts.
- The AI saving plan (F11) can return an incomplete answer. The agent sometimes runs out of its 6-step limit while looking up spending, and the app then shows a step-limit message instead of a plan. The plan prompt does not include today's date, so the model can pick a month with no data.
- Savings goals and transactions cannot be edited or deleted in the GUI. Test data had to be cleared by resetting the local database.
- The GUI has minimal empty-state guidance (for example after a fresh database, tabs show blank tables with little help text). Budgets and goals cannot be edited in place; a wrong goal requires deleting the local database or creating another goal. Ask (F09) does not show a clear offline banner in the GUI when the API key is missing or invalid, so a failed key can look like a normal empty reply until checked in the CLI. CSV import does not handle layouts beyond TD, RBC, and generic.

# 2. Feature to Implementation Mapping

Each feature is listed with its Stage 1 reference, status, implementing classes, key methods, patterns, and demonstration video. Stage 1 details are in `docs/Stage1_Report.md`.

| Feature | Stage 1 ref. | Status | Classes and files | Key methods | Patterns | Video |
|---|---|---|---|---|---|---|
| F01 Import transactions | F01 | Implemented | `ImportService`, `TransactionsController`, `BudgetBuddyCli`, `BudgetBuddyFacade` | `importFile()`, `parseRow()`, `importTransactions()` | Facade, MVC | [F01 video](https://drive.google.com/file/d/1edJRtekiVCy1tNSHZtkapC8zIw6ISOwF/view?usp=drive_link) |
| F02 AI auto-categorization | F02 | Implemented (hybrid) | `CategorizationService`, `CategorizationStrategy`, `RuleBasedStrategy`, `AiAssistedStrategy`, `PromptBuilder`, `ClaudeAdapter` | `categorize()`, `categorizeUncategorized()`, `categorizePrompt()`, `complete()` | Strategy, Adapter | [F02 video](https://drive.google.com/file/d/1mmB7g29cfgacoRwTIsPxu5ploezEjeQd/view?usp=drive_link) |
| F03 Category correction and learning | F03 | Implemented (hybrid) | `CategorizationService`, `CategoryRuleRepository`, `BudgetBuddyFacade`, `TransactionsController` | `correctCategory()`, `setCategory()`, `save()` | Facade, Strategy | [F03 video](https://drive.google.com/file/d/1VnK_T5DSReParZvzGUhb4vScgT2HXuUe/view?usp=drive_link) |
| F04 Budget management | F04 | Implemented | `BudgetService`, `BudgetRepository`, `Budget`, `BudgetsGoalsController`, `BudgetBuddyFacade` | `saveBudget()`, `deleteBudget()`, `copyBudgets()` | Facade, MVC | [F04 video](https://drive.google.com/file/d/12HoTtHiatxse6n-DuCJNfGgts0wKkDm8/view?usp=drive_link) |
| F05 Budget tracking and alerts | F05 | Implemented | `BudgetService`, `BudgetAlertListener`, `GuiBudgetAlertListener`, `BudgetBuddyCli` | `recalculate()`, `notifyListeners()`, `onBudgetAlert()` | Observer | [F05 video](https://drive.google.com/file/d/14fdMME_70fTajs-fI3orGKiu19BqT6aZ/view?usp=drive_link) |
| F06 Spending trends | F06 | Implemented | `AnalyticsService`, `InsightsController`, `InsightsLogic`, `MonthlySummary` | `getTrends()`, `getSpendingTrends()` | Facade, MVC | [F06 video](https://drive.google.com/file/d/1PRUUtAXOKR_kUGtTG6PhCdF67fk8r4co/view?usp=drive_link) |
| F07 Recurring payments | F07 | Implemented (hybrid) | `AnalyticsService`, `RecurringPayment`, `BudgetBuddyAgent` | `detectRecurring()`, `labelSubscriptions()` | Facade, Adapter | [F07 video](https://drive.google.com/file/d/14EzGuTKlZy-7jtr5Va4UtqXCCS-VBy4W/view?usp=drive_link) |
| F08 Unusual expenses | F08 | Implemented (hybrid) | `AnalyticsService`, `Anomaly`, `BudgetBuddyAgent` | `detectUnusual()`, `explainAnomalies()` | Facade, Adapter | [F08 video](https://drive.google.com/file/d/1xDQFumH5AUeYzkWPH0XtSIQeP9P4m8tE/view?usp=drive_link) |
| F09 Ask Budget Buddy | F09 | Implemented (AI agent) | `BudgetBuddyAgent`, `ToolRegistry`, `AgentTool`, `SpendingTool`, `BudgetTool`, `GoalsTool`, `ConversationMemory`, `AskController`, `AskLogic` | `ask()`, `execute()`, `complete()`, `getLastToolsUsed()` | Command, Adapter, Facade | [F09 video](https://drive.google.com/file/d/1S2oPRilMdNJgTRyi2V3XgLCrZlkkaDbx/view?usp=drive_link) |
| F10 Monthly summary | F10 | Implemented (hybrid) | `AnalyticsService`, `MonthlySummary`, `BudgetBuddyAgent`, `PromptBuilder`, `InsightsController` | `generateMonthlySummary()`, `buildMonthlyStats()`, `writeMonthlySummary()` | Facade, Adapter | [F10 video](https://drive.google.com/file/d/1YPsWSvFhtETNshCNymXdtcsIgH9Q7HIQ/view?usp=drive_link) |
| F11 Savings goals and AI plan | F11 | Implemented (hybrid) | `GoalService`, `GoalRepository`, `SavingsGoal`, `BudgetBuddyAgent`, `GoalsTool`, `BudgetsGoalsController` | `createGoal()`, `addContribution()`, `generateSavingPlan()`, `proposeSavingPlan()` | Command, Adapter, Facade | [F11 video](https://drive.google.com/file/d/16TmqAJia6-IpeV8NJQrQ5VcxDdfwrX0L/view?usp=drive_link) |

**How the classes collaborate (examples).**

- **F01, import (deterministic, with an AI step):** the user picks a file in `TransactionsController` (or runs `budgetbuddy import` in the CLI). The controller calls `BudgetBuddyFacade.importTransactions()`, which passes the file to `ImportService`. `ImportService` converts each row with `parseRow()` using the layout for the chosen bank, skips duplicates, and sends new rows to `CategorizationService` (F02). The results are saved through `TransactionRepository`, and the facade asks `BudgetService` to recheck budgets (F05).
- **F09, ask (AI agent):** the question enters through `AskController` or `budgetbuddy ask`, goes to `BudgetBuddyFacade.ask()`, and then to `BudgetBuddyAgent.ask()`. The agent sends the conversation to `ClaudeAdapter` along with the tool list from `ToolRegistry`. When the LLM requests a tool, the agent runs it through `execute()`. `SpendingTool`, `BudgetTool`, and `GoalsTool` read data from the services, and `ConversationMemory` keeps earlier turns. The loop stops when the LLM gives a final answer or reaches the step limit.
- **F05, alerts (deterministic):** after `BudgetService.recalculate()` totals spending, any budget at 80% or 100% notifies each registered `BudgetAlertListener`, which is the GUI listener or the CLI.

# 3. Implemented Design Patterns

The application implements six patterns. The requirement is five.

**3.1 Facade.** Problem: the GUI and CLI should not depend on the services, repositories, and agent directly. `BudgetBuddyFacade` (in `src/main/java/ca/budgetbuddy/`) is the single entry point. Its public methods such as `importTransactions()`, `saveBudget()`, `correctCategory()`, `ask()`, `generateMonthlySummary()`, and `generateSavingPlan()` each coordinate several collaborators. It matches Stage 1.

**3.2 Strategy.** Problem: categorization can use rules alone or rules plus the LLM. `CategorizationStrategy` is the interface. `RuleBasedStrategy` and `AiAssistedStrategy` are the implementations. `CategorizationService.setStrategy()` switches between them, and it falls back to `RuleBasedStrategy` if the API key is missing or the call fails. It matches Stage 1.

**3.3 Observer.** Problem: budget alerts must reach the GUI and CLI without `BudgetService` knowing about either. `BudgetAlertListener` is the observer interface. `BudgetService.addAlertListener()` registers listeners, `notifyListeners()` sends alerts, and `GuiBudgetAlertListener` and `BudgetBuddyCli` implement the interface. It matches Stage 1.

**3.4 Command.** Problem: the agent must run different tools chosen by the LLM through one uniform call. `AgentTool` is the command interface. `SpendingTool`, `BudgetTool`, and `GoalsTool` implement it, and `ToolRegistry` looks them up by name and executes them. The agent loop in `BudgetBuddyAgent` is the invoker. It matches Stage 1 (`AgentTool` and `ToolRegistry` naming matches the Stage 1 diagrams).

**3.5 Adapter.** Problem: the application should not depend on the Anthropic SDK directly, so the LLM can be swapped and tested offline. `LLMClient` is the target interface. `ClaudeAdapter` adapts the Anthropic API to it, and `MockLlmClient` is a test double. It matches Stage 1.

**3.6 MVC.** Problem: JavaFX views should not contain business logic. Views are the FXML files under `src/main/resources/ca/budgetbuddy/gui/`. Controllers (`MainController`, `TransactionsController`, `BudgetsGoalsController`, `InsightsController`, `AskController`) handle events. Logic classes (`TransactionsLogic`, `BudgetsGoalsLogic`, `InsightsLogic`, `AskLogic`) hold the testable presentation logic. The models are in `ca.budgetbuddy.model`. It matches Stage 1 in principle.

**Changes from Stage 1 pattern design.** Stage 1 named some classes `ChatController` and `DashboardController`. The implementation uses `AskController` and `MainController` for those roles. `InsightsController` was added for the trends and recurring views. Stage 1 did not list `GoalService` or `SpendingCalculator` as classes. These were added to keep goal logic and spending arithmetic out of the facade and the analytics code.

# 4. Changes and Rationale

Format: original design, implemented change, reason, impact.

1. **F01 to F11, all features.** Original: Stage 1 design. Implemented: all 11 features, with the same IDs. Reason: none needed. Impact: none.
2. **Presentation class names.** Original: `ChatController`, `DashboardController`. Implemented: `AskController`, `MainController`, `InsightsController`. Reason: the GUI was organized into four tabs (Transactions, Budgets and Goals, Insights, Ask) instead of one screen per feature, as Stage 1 scope already described. Impact: UC09 and the MVC diagram need the new names.
3. **Service layer.** Original: `AnalyticsService`, `BudgetService`, `CategorizationService`, `ImportService`. Implemented: those plus `GoalService` and `SpendingCalculator`. Reason: goal rules and spending arithmetic were separated for testing. Impact: F11 and F05 route through them.
4. **Agent memory.** Original: Stage 1 mentions an agent loop. Implemented: `ConversationMemory` keeps earlier turns for F09. Reason: follow-up questions need prior context. Impact: `BudgetBuddyAgent` takes a memory object and a step limit.
5. **Model configuration.** Original: Stage 1 names Claude as the LLM. Implemented: the model name is read from configuration (`ANTHROPIC_MODEL` in `.env`), not hard-coded. Reason: lets the model change without code edits. Impact: `LlmConfig` and `LlmConfigLoader`.
6. **Root package.** Original: early code used `budgetbuddy.model` and `budgetbuddy.persistence`. Implemented: all packages under `ca.budgetbuddy.*`. Reason: one root package so the report can trace classes consistently. Impact: imports and tests updated; package names in the Stage 1 diagram should be read as `ca.budgetbuddy.*`.
7. **F09 tool results.** Original: `SpendingTool` returned empty category maps with no hint which months had data. Implemented: empty results include `monthsWithData`, and `ConversationMemory` is cleared at the start of each ask or plan. Reason: the LLM was guessing months with no transactions and burning the step limit. Impact: F09 and F11 recover with usable tool output or a clean fallback plan.
8. **JSON replies from Claude.** Original: AI categorization and other JSON parsers expected raw JSON. Implemented: `JsonReply` strips markdown fences before parsing. Reason: Claude often wraps JSON in markdown code fences, which caused silent parse failures. Impact: F02 and other JSON LLM replies parse reliably.

# 5. AI Tools and Collaboration Log

**Tools used.**

| Tool / platform | LLM / model | Used for | Token cost | Money cost |
|---|---|---|---|---|
| Cursor (Agent mode) | Cursor Agent (Auto / Composer) | Writing most Java code for the GUI, CLI, services, and tests | Included in Cursor Pro (student plan) | $0 (Cursor Pro student plan, 1 year free) |
| Claude (Anthropic API, runtime) | claude-sonnet-4-6 (used locally; `.env.example` default is claude-sonnet-4-20250514) | Runtime LLM for F02, F07, F08, F09, F10, F11 | Not recorded separately; reflected in API spend below | About $1.14 ($5.65 credit on Nov 5, 2026 minus $4.51 remaining balance) |

Models used: Cursor Agent (Auto / Composer) for implementation, and Claude `claude-sonnet-4-6` at runtime via the Anthropic API. Cursor money cost was $0 under the Cursor Pro student plan (1 year free); token use is included in that plan. Anthropic API spend during Stage 2 was about $1.14. Exact Anthropic token totals were not copied from the Usage page at report time; the dollar figure comes from the Billing page credit balance.

**Representative interactions.**

1. **LLM layer:** Add the LLM layer in package `ca.budgetbuddy.llm`. Create `LLMClient`, `LlmResponse`, `MockLlmClient`, and `ClaudeAdapter`. Load the API key from env/`.env`/`config.local.json`. Add tests that never call the real API. Produced the `llm` package and tests; kept after review.
2. **Agent loop:** Add the agent engine in `ca.budgetbuddy.agent` with `ToolRegistry`, `ConversationMemory`, and `BudgetBuddyAgent.ask()` that loops on tool calls until a text reply or `maxSteps`. Unknown tools return JSON errors. Tests use `MockLlmClient` only. Produced the F09 agent loop; later added `clear()` on memory.
3. **JavaFX GUI:** Add a JavaFX GUI under `ca.budgetbuddy.gui` that calls only `BudgetBuddyFacade`. Use MVC with FXML. Tabs for Transactions, Budgets & Goals, Insights, and Ask. Launch with `mvn javafx:run`. Produced the GUI; controllers renamed vs Stage 1 (`AskController` / `MainController`).
4. **F02 defect fix:** Auto-categorize returns Categorized 0 even with a working API key; Claude is wrapping JSON in markdown fences — strip fences before parse. Produced `JsonReply` and tests (commit `fdaf5c5`).
5. **F11 defect fix:** Get AI Plan shows Incomplete answer after empty-month tool calls; include `monthsWithData` and fall back to Required per month. Produced the saving-plan fix (commit `66c3e98`).

**Human review.** The code was reviewed and run by the author. Two defects in AI-generated or AI-related output were corrected: (1) `AiAssistedStrategy` treated Claude's fenced JSON replies as invalid JSON, so F02 reported Categorized 0 with no error — fixed by adding `JsonReply` to strip markdown fences before parsing (commit `fdaf5c5`); (2) the agent returned the raw Incomplete answer step-limit string for F11 when the model queried empty months — fixed by returning `monthsWithData` from `SpendingTool`, steering the plan prompt, clearing conversation memory per request, and falling back to a Required per month plan (commit `66c3e98`).

# 6. Development and Verification Summary

**Weekly commit summary.** The project repository contains at least five meaningful code commits in each reporting period.

| Period | Dates (Toronto) | Code commits | Result |
|---|---|---|---|
| 1 | Oct 20 to 26 | 0 | Not met. Penalty applies (5 points, once). |
| 2 | Oct 27 to Nov 2 | 5 | Met |
| 3 | Nov 3 to 9 | 7 | Met |

Commits for period 2:

- [`ba67da9`](https://github.com/Abdi238/eecs3311-course-project/commit/ba67da9) Add TD and RBC bank layouts to CSV import (Oct 30)
- [`5efb319`](https://github.com/Abdi238/eecs3311-course-project/commit/5efb319) Add generic CSV import service with validation (Oct 30)
- [`8e4f167`](https://github.com/Abdi238/eecs3311-course-project/commit/8e4f167) Add domain model and SQLite repositories (Oct 30)
- [`2d97a7b`](https://github.com/Abdi238/eecs3311-course-project/commit/2d97a7b) Add duplicate detection and rule-based categorization (Oct 31)
- [`dbd4ef7`](https://github.com/Abdi238/eecs3311-course-project/commit/dbd4ef7) Add category correction and remember-this-merchant learning (Oct 31)

Commits for period 3:

- [`7ebd240`](https://github.com/Abdi238/eecs3311-course-project/commit/7ebd240) Add LLM client interface, mock client, and Claude adapter
- [`e5c33d7`](https://github.com/Abdi238/eecs3311-course-project/commit/e5c33d7) Add agent loop with tool registry and conversation memory
- [`d1b2683`](https://github.com/Abdi238/eecs3311-course-project/commit/d1b2683) Add facade and CLI with real finance tools
- [`5ab799c`](https://github.com/Abdi238/eecs3311-course-project/commit/5ab799c) Add JavaFX GUI with transactions, budgets, goals, and ask tabs
- [`8ce0ad4`](https://github.com/Abdi238/eecs3311-course-project/commit/8ce0ad4) Add budget alerts observer and AI categorization strategy
- [`5a0b77a`](https://github.com/Abdi238/eecs3311-course-project/commit/5a0b77a) Add recurring payments, unusual expenses, and monthly summary
- [`61d31d9`](https://github.com/Abdi238/eecs3311-course-project/commit/61d31d9) Add AI saving plan and README run instructions

**Checks performed.**

- Static review that every Stage 1 feature has a reachable method from both the GUI and the CLI (see Section 2).
- The repository contains unit tests for the domain model, CSV import, categorization, budget service, agent, facade, GUI logic, and the Claude adapter (`src/test/java`).
- Local verification: `mvn test` on the author's computer produced Tests run: 91, Failures: 0, Errors: 0, Skipped: 0 (`BUILD SUCCESS`).
- Secrets check: `.env` and `config.local.json` are in `.gitignore`, and `.env.example` contains only a blank placeholder. A search of the repository history found no committed real API key. Any previously revoked Anthropic key was replaced locally in `.env` and was never present in git history.

**Defects found and corrected.** (1) F02 auto-categorization reported Categorized 0 because Claude wrapped JSON in markdown fences and the parser failed silently — fixed with `JsonReply` fence stripping (`fdaf5c5`). (2) F11 Get AI Plan showed the internal Incomplete answer step-limit text after empty-month tool calls — fixed with `monthsWithData`, prompt steering, memory clear, and a Required per month fallback (`66c3e98`). (3) During demo testing, a revoked or invalid Anthropic key caused Ask/categorize to fail without a clear GUI error; resolved by putting a valid key in local `.env` and verifying with the CLI before relying on the GUI.

**Remaining limitations.** Covered in Section 1. Full deterministic testing and KUMA validation are Stage 3.

# 7. Demonstration Index

Each feature has its own video clip. Both the GUI and the CLI are shown across the set. F09 is demonstrated in the GUI only, and the CLI is shown in the other clips.

| Feature | Interface | Video link |
|---|---|---|---|
| F01 Import transactions | GUI and CLI | [F01 video](https://drive.google.com/file/d/1edJRtekiVCy1tNSHZtkapC8zIw6ISOwF/view?usp=drive_link) |
| F02 AI auto-categorization | GUI | [F02 video](https://drive.google.com/file/d/1mmB7g29cfgacoRwTIsPxu5ploezEjeQd/view?usp=drive_link) |
| F03 Category correction and learning | GUI | [F03 video](https://drive.google.com/file/d/1VnK_T5DSReParZvzGUhb4vScgT2HXuUe/view?usp=drive_link) |
| F04 Budget management | GUI | [F04 video](https://drive.google.com/file/d/12HoTtHiatxse6n-DuCJNfGgts0wKkDm8/view?usp=drive_link) |
| F05 Budget tracking and alerts | GUI and CLI | [F05 video](https://drive.google.com/file/d/14fdMME_70fTajs-fI3orGKiu19BqT6aZ/view?usp=drive_link) |
| F06 Spending trends | GUI and CLI | [F06 video](https://drive.google.com/file/d/1PRUUtAXOKR_kUGtTG6PhCdF67fk8r4co/view?usp=drive_link) |
| F07 Recurring payments | GUI and CLI | [F07 video](https://drive.google.com/file/d/14EzGuTKlZy-7jtr5Va4UtqXCCS-VBy4W/view?usp=drive_link) |
| F08 Unusual expenses | GUI and CLI | [F08 video](https://drive.google.com/file/d/1xDQFumH5AUeYzkWPH0XtSIQeP9P4m8tE/view?usp=drive_link) |
| F09 Ask Budget Buddy | GUI | [F09 video](https://drive.google.com/file/d/1S2oPRilMdNJgTRyi2V3XgLCrZlkkaDbx/view?usp=drive_link) |
| F10 Monthly summary | GUI and CLI | [F10 video](https://drive.google.com/file/d/1YPsWSvFhtETNshCNymXdtcsIgH9Q7HIQ/view?usp=drive_link) |
| F11 Savings goals and AI plan | GUI and CLI | [F11 video](https://drive.google.com/file/d/16TmqAJia6-IpeV8NJQrQ5VcxDdfwrX0L/view?usp=drive_link) |

All video links must be accessible without a request. No API keys or credentials may appear in the recordings.
