# Budget Buddy: AI Personal Finance Assistant

EECS3311 Fall 2026 Course Project
Author: Mohamed Abdi, 218945246

Budget Buddy imports bank CSV exports, categorizes transactions, tracks monthly budgets and savings goals, reports spending trends, and answers questions about your money in plain language. It has a JavaFX window and a command-line interface (CLI). Both call the same `BudgetBuddyFacade`, and data is stored locally in SQLite.

The AI features use Claude through the Anthropic API. AI output is informational and is not financial advice.

## Project reports

- [Stage 1 report](https://github.com/Abdi238/EECS3311-course-project/blob/main/docs/Stage1_Report.md): the design (features, classes, patterns, use cases, sequence diagrams).
- [Stage 2 report](https://github.com/Abdi238/EECS3311-course-project/blob/main/docs/stage2-report.md): the implementation, changes from Stage 1, AI collaboration log, weekly commits, and the demonstration index.
- [Stage 2 report (PDF)](https://github.com/Abdi238/EECS3311-course-project/blob/main/docs/stage2-report.pdf): the same report as a PDF, for submission.

## Demonstration videos

The index of feature videos (F01 to F11) is in Section 7 of the [Stage 2 report](https://github.com/Abdi238/EECS3311-course-project/blob/main/docs/stage2-report.md).

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

The AI features (categorization, Ask, recurring and unusual explanations, the monthly recap, and saving plans) need an API key. Without one, they fall back to offline output, and the app still runs.

`.env.example` contains only placeholders. Never commit `.env` or `config.local.json`. The model name is set with `ANTHROPIC_MODEL` in the same file, and `BUDGET_BUDDY_DB` sets the database file (default `budgetbuddy.db`).

## Run the GUI

```powershell
mvn javafx:run
```

The window has four tabs: Transactions, Budgets and Goals, Insights, and Ask. It uses `budgetbuddy.db` in the project folder.

## Run the CLI

Every CLI command runs through Maven from the project folder:

```powershell
mvn -q exec:java "-Dexec.args=<command and options>"
```

Put the whole command inside `-Dexec.args=` and wrap that in double quotes. Use single quotes for any text inside the command that contains spaces, such as a question or a goal name with spaces. The `-q` flag hides Maven's build messages. The `SLF4J(W)` lines you may see are harmless.

Every command accepts `--db <file>` to use a different database file. Without it, the app uses `budgetbuddy.db` in the project folder. The database keeps your transactions, budgets, and goals between runs, so the commands below build on each other in the order shown.

### Quick reference

| Command | What it does | Needs an API key? |
|---|---|---|
| `import <file> --bank TD\|RBC\|GENERIC` | Loads a bank CSV and skips duplicate rows | No (new rows are then categorized) |
| `categorize` | Categorizes transactions that have no category yet | Optional (AI step) |
| `recategorize <ID> <CATEGORY> [--remember]` | Changes the category of one transaction | No |
| `budget set <CATEGORY> <AMOUNT> --month YYYY-MM` | Sets a monthly spending limit for a category | No |
| `budget status --month YYYY-MM` | Shows spending against each budget for a month | No |
| `trends --from YYYY-MM --to YYYY-MM` | Shows monthly spending totals across a range | No |
| `recurring` | Lists charges that look like subscriptions or regular bills | No (labels need AI) |
| `unusual --month YYYY-MM` | Lists transactions that look unusual for that month | No (explanations need AI) |
| `summary --month YYYY-MM` | Shows the month's totals with a short written recap | Optional (recap needs AI) |
| `goal add <NAME> <TARGET> --deadline YYYY-MM-DD` | Creates a savings goal and prints its ID | No |
| `goal plan <ID>` | Shows what you need to save each month, and an AI plan if a key is set | Optional (plan needs AI) |
| `ask "<QUESTION>"` | Answers a question about your money in plain language | Yes |

### Command details and examples

**1. `import`: load a bank statement**

```powershell
mvn -q exec:java "-Dexec.args=import samples/generic-sample.csv --bank GENERIC"
```

- `<file>` is the path to the CSV, relative to the project folder.
- `--bank` is required and must be `TD`, `RBC`, or `GENERIC`. Use `GENERIC` for the sample files in `samples/`. The generic layout needs the columns `date`, `description`, and `amount`.
- Rows that are already in the database are skipped, so running the same import twice does not create duplicates. The output reports how many rows were imported and how many were skipped as duplicates.
- Imported rows are categorized automatically. Saved merchant rules are applied first. If a key is set, the AI step handles the rest. Without a key, rules-only categorization runs.

**2. `categorize`: fill in missing categories**

```powershell
mvn -q exec:java "-Dexec.args=categorize"
```

- Categorizes only transactions that have no category yet.
- The output reports how many transactions were categorized. If it reports 0 and you expected more, check that `LLM_API_KEY` is set in `.env` and that the AI step is running. See Troubleshooting.
- The AI step only accepts a suggestion when Claude is at least 60% confident. Lower-confidence rows stay uncategorized.

**3. `recategorize`: fix one transaction's category**

```powershell
mvn -q exec:java "-Dexec.args=recategorize 2 OTHER --remember"
```

- `<ID>` is the transaction's ID in the database. Use the ID shown in the GUI Transactions tab for that row.
- `<CATEGORY>` is the category name, such as `GROCERIES` or `OTHER`.
- `--remember` saves a rule for that merchant. Future transactions from the same merchant get this category automatically, unless you have set their category yourself.
- Without `--remember`, only this one transaction changes.

**4. `budget set`: set a monthly limit**

```powershell
mvn -q exec:java "-Dexec.args=budget set GROCERIES 100.00 --month 2026-10"
```

- `<CATEGORY>` is the category to limit, `<AMOUNT>` is the monthly limit in dollars, and `--month` is required.
- Run it again with a different amount to change the limit for that category and month.
- When spending reaches 80% or 100% of a limit, the app raises a budget alert in the CLI and the GUI.

**5. `budget status`: check budgets for a month**

```powershell
mvn -q exec:java "-Dexec.args=budget status --month 2026-10"
```

- Shows each budget for the month with the amount spent against the limit.
- Use it after `budget set` and after importing transactions to see where you stand.

**6. `trends`: spending over a range of months**

```powershell
mvn -q exec:java "-Dexec.args=trends --from 2026-10 --to 2026-10"
```

- Shows spending totals for each month from `--from` to `--to`, both inclusive. Use the same month for both to see one month.

**7. `recurring`: find subscriptions and regular bills**

```powershell
mvn -q exec:java "-Dexec.args=recurring"
```

- Looks at the last six months for merchants that charge similar amounts (within about 20% of each other) at a regular interval. A charge counts when a merchant has at least three of them.
- Recognizes weekly charges (6 to 8 days apart), monthly charges (27 to 32 days apart), and yearly charges (350 to 380 days apart).
- With a key set, Claude gives each one a short label.

**8. `unusual`: flag unusual transactions**

```powershell
mvn -q exec:java "-Dexec.args=unusual --month 2026-10"
```

- Flags a transaction if it is more than three times the average for its category over the three months before. It also flags a new merchant charging more than $200.
- With a key set, Claude explains each flagged item.

**9. `summary`: monthly recap**

```powershell
mvn -q exec:java "-Dexec.args=summary --month 2026-10"
```

- The app works out the month's totals and category breakdown first. With a key set, Claude writes a short recap of those totals.

**10. `goal add`: create a savings goal**

```powershell
mvn -q exec:java "-Dexec.args=goal add EmergencyFund 500.00 --deadline 2026-12-31"
```

- `<NAME>` is the goal's name. Use single quotes around a name with spaces, such as `'New Car'`.
- `<TARGET>` is the amount you want to save, in dollars.
- `--deadline` is the date by which you want to reach the target, in `YYYY-MM-DD` form.
- The output prints the goal's ID. You need it for `goal plan`.
- The CLI cannot add money to a goal. Use the Budgets and Goals tab in the GUI for that.

**11. `goal plan`: what to save each month**

```powershell
mvn -q exec:java "-Dexec.args=goal plan 1"
```

- `<ID>` is the goal ID printed by `goal add`.
- The app always calculates the required amount per month. It divides the remaining amount by the number of whole months from the current month to the deadline month. The current month is not counted.
- With a key set, Claude also writes a saving plan based on your spending. If Claude cannot finish the plan, the app shows the required amount per month instead.

**12. `ask`: ask a question in plain language**

```powershell
mvn -q exec:java "-Dexec.args=ask 'How much did I spend in October?'"
```

- Put the question in single quotes inside the double quotes, as shown.
- Needs an API key in `.env`. Without one, the app says the assistant is offline.
- Claude uses your spending, budget, and goal data to answer, and it reports which tools it used.

### A suggested first run

Run these in order on a new database:

1. `import samples/generic-sample.csv --bank GENERIC`
2. `categorize`
3. `budget set GROCERIES 100.00 --month 2026-10`
4. `budget status --month 2026-10`
5. `recategorize <ID> OTHER --remember`, with an ID from the Transactions tab
6. `trends --from 2026-10 --to 2026-10`
7. `recurring`
8. `unusual --month 2026-10`
9. `summary --month 2026-10`
10. `goal add EmergencyFund 500.00 --deadline 2026-12-31`, then `goal plan <ID>` with the printed ID
11. `ask 'How much did I spend in October?'`, with a key set

Import, budget, and goal commands write to the same database, so the GUI shows the same data after a CLI run. Close the GUI before you delete `budgetbuddy.db` or run a fresh import.

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
