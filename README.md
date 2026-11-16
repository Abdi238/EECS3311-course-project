# Budget Buddy: AI Personal Finance Assistant

EECS3311 Fall 2026 Course Project
Author: Mohamed Abdi, 218945246

Budget Buddy imports bank CSV exports, categorizes transactions, tracks monthly budgets and savings goals, reports spending trends, and answers questions about your money in plain language. It has a JavaFX window and a command-line interface (CLI). Both call the same `BudgetBuddyFacade`, and data is stored locally in SQLite.

The AI features use Claude through the Anthropic API. AI output is informational and is not financial advice.

## Prerequisites

- JDK 21
- Maven 3.9 or newer
- An Anthropic API key (optional; the app runs without one)

Check your tools with `java -version` and `mvn -version`.

## Install dependencies

Maven downloads all dependencies from `pom.xml` the first time you build:

```powershell
git clone https://github.com/Abdi238/eecs3311-course-project.git
cd eecs3311-course-project
mvn clean install
```

## Local configuration (API key)

The repository never contains a real API key. Configuration uses a local `.env` file, which is listed in `.gitignore`.

1. Copy the example file:

   ```powershell
   copy .env.example .env
   ```

   On macOS or Linux, use `cp .env.example .env`.

2. Open `.env` in a text editor and replace the placeholder after `LLM_API_KEY=` with your own key. Do not add quotes or spaces.

3. Leave the key blank to run without AI. The app then uses rules-only categorization and an offline assistant.

`.env.example` contains only placeholders. Never commit `.env` or `config.local.json`. The model name is set with `ANTHROPIC_MODEL` in the same file, and `BUDGET_BUDDY_DB` sets the database file (default `budgetbuddy.db`).

## Run the GUI

```powershell
mvn javafx:run
```

The window has four tabs: Transactions, Budgets and Goals, Insights, and Ask. It uses `budgetbuddy.db` in the project folder.

## Run the CLI

Commands run through `mvn -q exec:java`. Add `--db <file>` to any command to use a different database.

```powershell
mvn -q exec:java "-Dexec.args=import samples/generic-sample.csv --bank GENERIC"
mvn -q exec:java "-Dexec.args=categorize"
mvn -q exec:java "-Dexec.args=budget status --month 2026-10"
mvn -q exec:java "-Dexec.args=trends --from 2026-10 --to 2026-10"
mvn -q exec:java "-Dexec.args=recurring"
mvn -q exec:java "-Dexec.args=unusual --month 2026-10"
mvn -q exec:java "-Dexec.args=summary --month 2026-10"
mvn -q exec:java "-Dexec.args=goal add EmergencyFund 500.00 --deadline 2026-12-31"
mvn -q exec:java "-Dexec.args=goal plan 1"
```

`goal plan` takes the goal id printed by `goal add`. Without an API key it prints only the required amount per month.

The `ask` command takes a free-form question. Run it with an API key set in `.env`.

## Tests

```powershell
mvn test
```

Tests use an in-memory database and `MockLlmClient`. They never call Anthropic and do not create `budgetbuddy.db`.

## Sample data

`samples/generic-sample.csv` contains fake merchants and amounts. Use it to try the app. Columns are `date,description,amount`.

## Troubleshooting

- **Auto-categorize shows "Categorized 0":** check that `LLM_API_KEY` in `.env` is set to a valid key, then restart the app.
- **Key not picked up:** `.env` must be in the project folder where you run the command, and the line must be `LLM_API_KEY=your-key`.
