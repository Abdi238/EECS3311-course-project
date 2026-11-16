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

`.env.example` contains only
