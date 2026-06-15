# OPSC6311-PART-3-APP-BUDGETWISE-
# BudgetWise – Personal Budget Tracker (Final POE)

## Group Members
- Kun’we Tyrone Mdaka (ST10262122)
- Oratile Maungwa (ST10443081)
- Gontse Rakosa (ST10449265)
- Richard Sebola (ST10441486)

---

## 1. Part 3 Compliance (Final POE Requirements)

The following list shows how BudgetWise meets every required feature for the final submission:

- **Graph of amount spent per category over user‑selected period, including min/max goals**  
  Status: Implemented  
  Location: Insights → bar chart with goals overlay

- **Visual display of progress relative to monthly spending goals**  
  Status: Implemented  
  Location: Dashboard → Budget Health Score + visual alerts

- **Gamification (badges/rewards for meeting goals and consistent logging)**  
  Status: Implemented  
  Location: Profile → earned badges (e.g., “Budget Keeper”)

- **Own feature 1 – Custom Monthly Income**  
  Status: Implemented  
  Location: Dashboard → set income, balance updates instantly

- **Own feature 2 – PDF Export with Receipts**  
  Status: Implemented  
  Location: Transactions → export PDF to Downloads

- **App runs on a physical mobile phone (not emulator)**  
  Status: Tested on physical Android device (minimum API 26)

- **Demonstration video with voice‑over**  
  Status: Included (link provided in this README)

- **GitHub Actions + automated testing**  
  Status: Configured in `.github/workflows/build.yml`

---

## 2. Overview

BudgetWise is a feature‑rich Android budgeting app built with:
- Kotlin
- Room database (SQLite)
- Pure XML layouts (Material3 theme)

The app helps users:
- Track expenses and set budget goals
- Capture receipt photos (camera or gallery)
- Export reports as PDF
- Stay motivated through gamification badges

---

## 3. Core Features (Part 2 and Final POE)

### Authentication and Data Management
- User registration and login with hashed passwords (SHA‑256 plus salt)
- Default spending categories: Groceries, Transport, Entertainment, Rent, Eating Out, Utilities, Shopping, Healthcare, Other

### Expense Tracking
- Add expense with:
  - Amount, date, start and end times, description
  - Category dropdown
  - Optional receipt photo (camera or gallery)
- View expenses filtered by any custom date range
- View expense details (including stored receipt photo)
- View total spending per category for any period

### Budgeting
- Set monthly overall budget (minimum and maximum goals)
- Set per‑category envelope limits

### Final POE – Added Features
- Budget progress dashboard – shows balance, custom monthly income, Budget Health Score, and overspent category alerts
- Graphical insights – bar chart of category spending using MPAndroidChart, including minimum and maximum goal lines
- Gamification – users earn badges (e.g., “Budget Keeper”) when they stay within budget
- PDF export with receipts – generates a formatted report of filtered expenses, includes receipt thumbnails, and saves to Downloads

### Own Features (from design document)
1. Custom Monthly Income – user can set their income; the dashboard balance updates instantly.
2. PDF Export with Receipts – detailed PDF report with receipt images, saved or shared from the Transactions screen.

---

## 4. New / Unique Features in BudgetWise

The following features are **not commonly found** in existing budgeting apps (YNAB, Goodbudget, Credit Karma). BudgetWise includes them as differentiators.

- **Photo Receipts**  
  What it does: Attach a photo (from camera or gallery) to any expense entry.  
  Why it's new: Goodbudget has no photo support; others require bank sync.

- **Gamification Badges**  
  What it does: Earn badges like "Budget Master" (3 months under budget) or "Consistent Logger" (7 days of tracking).  
  Why it's new: YNAB has "Age of Money" but no badge system; most apps lack gamification.

- **Fill Envelopes Equally**  
  What it does: One‑tap button to distribute available money equally across all spending envelopes.  
  Why it's new: Goodbudget requires manual allocation per envelope; this automates equal distribution.

- **Budget Health Score**  
  What it does: A quick numeric/visual score (e.g., 85/100) based on spending vs goals.  
  Why it's new: Credit Karma shows trends but no simplified health score; YNAB lacks a single health metric.

- **Combined Proactive + Envelope + Credit View**  
  What it does: Zero‑based planning (like YNAB) + envelope limits (like Goodbudget) + holistic dashboard (like Credit Karma) in one free app.  
  Why it's new: No existing app combines all three approaches without a paid subscription.

- **Local Database with Optional Photo Storage**  
  What it does: All data (including receipt images) stored locally – privacy first.  
  Why it's new: Most apps rely on cloud sync; BudgetWise respects user privacy by keeping everything on device.

---

## 5. Screens Overview

| Screen | Purpose |
|--------|---------|
| Login / Register | Secure authentication with password visibility toggle |
| Dashboard | Balance, health score, income setting, category list |
| Transactions | Add expense (photo, category), filter by date, view details, export PDF |
| Budgets | Set overall budget and per‑category envelope limits; “Fill Envelopes Equally” helper |
| Insights | Two tabs: category totals list and spending bar chart (with min/max goals) |
| Profile | View earned badges, logout |

---

## 6. Tech Stack

- Language: Kotlin
- UI: XML with Material Components (Material3 theme)
- Database: Room (SQLite)
- Charts: MPAndroidChart version 3.1.0
- Camera: CameraX and FileProvider
- Testing: JUnit, AndroidX Test (instrumented tests)
- Build: Gradle with version catalog (libs.versions.toml)

---

## 7. Project Structure
com.example.budgetwise
├── data/
│ ├── entity/ // Room entities (User, Category, Expense, BudgetGoal, CategoryBudgetLimit, Badge)
│ ├── dao/ // Room DAOs
│ ├── AppDatabase.kt
│ └── DatabaseProvider.kt
├── util/
│ ├── HashUtils.kt
│ └── DateUtils.kt
├── viewmodel/ // ViewModels
├── ui/
│ ├── screens/auth/ // LoginActivity, RegisterActivity
│ └── screens/main/ // DashboardActivity, TransactionsActivity, BudgetsActivity, InsightsActivity, ProfileActivity
└── DatabaseTest.kt // Instrumented tests (androidTest)

text

---

## 8. How to Build and Run

1. Open the project in Android Studio (Hedgehog or later).
2. Sync Gradle: File → Sync Project with Gradle Files.
3. Clean then rebuild the project:
   - Build → Clean Project
   - Build → Rebuild Project
4. Run on a physical Android device (minimum API 26) – this is required for the Final POE.

Note: The app has been tested on a physical phone, not only on an emulator.

---

## 9. Testing and Continuous Integration

### Instrumented Tests (DatabaseTest.kt)
- User insertion and password verification
- Category separation per user
- Expense insertion and date filtering
- Budget goal storage
- Badge insertion and retrieval

### GitHub Actions
- Automated build and test on every push.
- Workflow file: `.github/workflows/build.yml`

---

## 11. video

Youtube video link: https://youtu.be/7OgdQCrSjDw?si=bxqLSOQDXkvRTvPa 

## 13. GitHub project file

GitHub LiNK: https://github.com/kunwe/OPSC6311-PART-3-APP-BUDGETWISE- 
PROJECT FILE REPO GITHUB LINK: https://github.com/kunwe/BudgetWise.git 
---

## 13. References

- MPAndroidChart – https://github.com/PhilJay/MPAndroidChart
- Room Persistence Library – Android Developers (https://developer.android.com/training/data-storage/room)
- CameraX – Android Developers (https://developer.android.com/training/camerax)
- Icons – Material Symbols and Icons
- PDF generation – android.graphics.pdf.PdfDocument
