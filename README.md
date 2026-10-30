# Budget Buddy: AI Personal Finance Assistant

EECS3311 Fall 2026 Course Project  
Author: Mohamed Abdi, 218945246

Budget Buddy is an AI agent that helps users understand and manage their spending.
It imports bank transactions, categorizes them, tracks budgets, detects
subscriptions and unusual expenses, and answers questions about the user's
finances using an LLM with tool calling. It provides both a GUI and a CLI.

## Project Stages

- **Stage 1: Design Report:** [docs/Stage1_Report.md](docs/Stage1_Report.md)
- **Stage 2: Implementation** (in progress) — domain model and SQLite repositories
- Stage 3: (coming soon)

## Prerequisites

- Java 21
- Maven 3.9+

## Setup

```bash
git clone https://github.com/Abdi238/EECS3311-course-project.git
cd EECS3311-course-project
copy .env.example .env
```

On macOS/Linux use `cp .env.example .env`. Open `.env` and add an Anthropic API
key if you want AI features. Leave `ANTHROPIC_API_KEY` blank to run with
rules-only categorization. Do not commit `.env`.

## Tests

```bash
mvn test
```

Repository tests use an in-memory SQLite database. They do not create
`budgetbuddy.db` and they do not call the Anthropic API.

## Current layout

```
src/main/java/budgetbuddy/model/         Domain classes from Stage 1 Section 3.3
src/main/java/budgetbuddy/persistence/   SQLite JDBC repositories
src/main/resources/schema.sql            Database tables
src/test/java/budgetbuddy/               JUnit 5 tests
```

Persisted types: `Transaction`, `CategoryRule`, `Budget`, `SavingsGoal`.  
Computed types (not stored): `RecurringPayment`, `Anomaly`, `MonthlySummary`.
`Budget.spent` is calculated at runtime and is not saved.
