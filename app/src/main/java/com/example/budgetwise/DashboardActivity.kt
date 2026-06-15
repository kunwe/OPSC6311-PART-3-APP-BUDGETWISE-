package com.example.budgetwise

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetwise.data.DatabaseProvider
import com.example.budgetwise.data.entity.Category
import com.example.budgetwise.data.entity.Expense
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private val TAG = "DashboardActivity"
    private lateinit var db: com.example.budgetwise.data.AppDatabase
    private var userId: Int = -1
    
    private lateinit var budgetStatusText: TextView
    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var progressMessageText: TextView
    private lateinit var categoriesContainer: LinearLayout

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) { finish(); return }

        db = DatabaseProvider.getDatabase(this)
        initUI()
        refreshData()
        setupBottomNavigation()
    }

    private fun initUI() {
        budgetStatusText = findViewById(R.id.budgetStatusText)
        budgetProgressBar = findViewById(R.id.budgetProgressBar)
        progressMessageText = findViewById(R.id.progressMessageText)
        categoriesContainer = findViewById(R.id.categoriesContainer)
        findViewById<Button>(R.id.reloadButton).setOnClickListener { refreshData() }
    }

    private fun refreshData() {
        lifecycleScope.launch {
            val now = Calendar.getInstance()
            val currentMonthStr = monthKeyFormat.format(now.time)
            
            // Get start of month for current year
            now.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = isoFormat.format(now.time)
            
            val goal = db.budgetDao().getBudgetGoal(userId, currentMonthStr)
            val expenses = db.expenseDao().getExpensesByPeriod(userId, startOfMonth, "2100-01-01")
            val categories = db.categoryDao().getCategoriesByUser(userId)

            runOnUiThread {
                updateBudgetProgress(goal, expenses)
                renderCategories(categories, expenses)
            }
        }
    }

    private fun updateBudgetProgress(goal: com.example.budgetwise.data.entity.BudgetGoal?, expenses: List<Expense>) {
        val totalSpent = expenses.sumOf { it.amount }
        if (goal != null) {
            val max = goal.maxGoal
            budgetStatusText.text = "Spent: R${"%.2f".format(totalSpent)} | Range: R${"%.2f".format(goal.minGoal)} - R${"%.2f".format(max)}"
            val progressPercent = if (max > 0) ((totalSpent / max) * 100).toInt() else 0
            budgetProgressBar.progress = progressPercent.coerceAtMost(100)

            when {
                totalSpent > max -> {
                    progressMessageText.text = "⚠️ Budget Exceeded!"
                    progressMessageText.setTextColor(Color.RED)
                    budgetProgressBar.progressTintList = ColorStateList.valueOf(Color.RED)
                }
                totalSpent >= goal.minGoal -> {
                    progressMessageText.text = "✅ Perfect! Within target range."
                    progressMessageText.setTextColor(Color.parseColor("#2E7D32"))
                    budgetProgressBar.progressTintList = ColorStateList.valueOf(Color.parseColor("#2E7D32"))
                }
                else -> {
                    progressMessageText.text = "Currently under minimum target."
                    progressMessageText.setTextColor(Color.BLUE)
                    budgetProgressBar.progressTintList = ColorStateList.valueOf(Color.BLUE)
                }
            }
        } else {
            budgetStatusText.text = "Total Spent: R${"%.2f".format(totalSpent)}"
            progressMessageText.text = "No budget set for this month."
        }
    }

    private fun renderCategories(categories: List<Category>, expenses: List<Expense>) {
        categoriesContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val expenseMap = expenses.groupBy { it.categoryId }

        categories.forEach { cat ->
            val view = inflater.inflate(R.layout.item_category, categoriesContainer, false)
            val spent = expenseMap[cat.id]?.sumOf { it.amount } ?: 0.0
            view.findViewById<TextView>(R.id.categoryName).text = cat.name
            view.findViewById<TextView>(R.id.categorySpentAmount).text = "R${"%.2f".format(spent)}"
            categoriesContainer.addView(view)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_dashboard
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_transactions -> { startActivity(Intent(this, TransactionsActivity::class.java).putExtra("USER_ID", userId)); finish(); true }
                R.id.nav_budgets -> { startActivity(Intent(this, BudgetsActivity::class.java).putExtra("USER_ID", userId)); finish(); true }
                R.id.nav_insights -> { startActivity(Intent(this, InsightsActivity::class.java).putExtra("USER_ID", userId)); finish(); true }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java).putExtra("USER_ID", userId)); finish(); true }
                else -> false
            }
        }
    }
}
