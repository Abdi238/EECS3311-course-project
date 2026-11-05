# Budget Buddy: AI Personal Finance Assistant

EECS3311 Fall 2026 Course Project  
Author: Mohamed Abdi, 218945246

Budget Buddy imports bank transactions, categorizes them, tracks budgets and savings goals, and answers questions about a fake ledger. It has a JavaFX window and a command line. Both call the same facade.

AI output is informational and not financial advice.

## Prerequisites

- JDK 21
- Maven 3.9 or newer

Check with `java -version` and `mvn -version`.

## Setup

```powershell
git clone https://github.com/Abdi238/EECS3311-course-project.git
cd EECS3311-course-project
copy .env.example .env
```

On macOS or Linux, use `cp .env.example .env`. Open `.env` on your machine and set `ANTHROPIC_API_KEY` to your own key. Leave it blank to run with rules-only categorization and the offline assistant. Do not commit `.env`. The sample file `samples/generic-sample.csv` uses fake merchants only.

## Build

```powershell
mvn clean install
```

## Tests

```powershell
mvn test
```

Tests use an in-memory database and `MockLlmClient`. They do not call Anthropic and they do not create `budgetbuddy.db`.

## GUI

```powershell
mvn javafx:run
```

This opens the Budget Buddy window against `budgetbuddy.db` in the project folder.

## CLI

Commands go through `mvn -q exec:java` and use `budgetbuddy.db` unless you pass `--db`.

```powershell
mvn -q exec:java "-Dexec.args=import samples/generic-sample.csv --bank GENERIC"
mvn -q exec:java "-Dexec.args=categorize"
mvn -q exec:java "-Dexec.args=budget status --month 2026-10"
mvn -q exec:java "-Dexec.args=trends --from 2026-10 --to 2026-10"
mvn -q exec:java "-Dexec.args=recurring"
mvn -q exec:java "-Dexec.args=unusual --month 2026-10"
mvn -q exec:java "-Dexec.args=summary --month 2026-10"
mvn -q exec:java "-Dexec.args=ask ""How much did Sample Grocery Store cost?"""
mvn -q exec:java "-Dexec.args=goal add ""Emergency fund"" 500.00 --deadline 2026-12-31"
mvn -q exec:java "-Dexec.args=goal plan 1"
```

`goal plan` prints a saving plan for that goal id. With no API key it prints only the required amount per month.
