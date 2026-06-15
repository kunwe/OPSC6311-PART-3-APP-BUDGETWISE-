# BudgetWise - Personal Finance Manager

## Project Overview
BudgetWise is a comprehensive Android application designed to help users take control of their financial health. The app allows users to track expenses, set monthly budget goals, and visualize their spending habits through interactive charts and progress indicators.

## Core Features
- **Dashboard**: Real-time visual progress of monthly spending against minimum and maximum goals.
- **Transaction Management**: Log expenses with categories, descriptions, and timestamps.
- **Budgeting**: Set overall monthly goals and specific "category envelopes" to limit spending.
- **Insights**: Interactive bar charts showing spending distribution with visual goal markers.
- **Gamification**: Earn badges (like "Budget Keeper") for maintaining financial discipline.

## Custom Features (POE Requirements)
To exceed the required standards, the following two custom features were implemented:

### 1. Receipt Capture & Digital Storage
Users can attach physical receipts to any transaction using the device camera or gallery. The app manages file providers to store these images securely, ensuring users have proof of purchase for every expense.
- **Implementation**: Uses CameraX and MediaStore API for high-quality image handling.

### 2. PDF Expense Reporting
Users can generate a professional PDF report of their transactions for any selected date range. This report includes transaction details and embedded receipt thumbnails, making it easy to share for tax or accounting purposes.
- **Implementation**: Uses Android's `PdfDocument` and `ContentResolver` to generate and save documents to the system Downloads folder.

## Technical Implementation
- **Architecture**: MVVM pattern using Kotlin Coroutines for asynchronous database operations.
- **Persistence**: SQLite database managed via the Room Persistence Library.
- **CI/CD**: Automated builds and testing configured via **GitHub Actions** (`build.yml`).
- **UI/UX**: Modern Material 3 components, including `MaterialCardView`, `TextInputLayout`, and `NestedScrollView` for optimal accessibility.

## Automated Testing
Functional logic is verified through JUnit tests located in `app/src/test`. These tests ensure that budget calculations and goal-tracking logic remain accurate during development.

## Demonstration Video
[LINK TO YOUR UNLISTED YOUTUBE VIDEO HERE]
*(Note: Please record your demo on a physical phone as per rubric requirements.)*
