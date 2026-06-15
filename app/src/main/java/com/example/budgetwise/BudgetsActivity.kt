package com.example.budgetwise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetwise.data.DatabaseProvider
import com.example.budgetwise.data.entity.BudgetGoal
import com.example.budgetwise.data.entity.Category
import com.example.budgetwise.data.entity.CategoryBudgetLimit
import com.example.budgetwise.util.DateUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * BudgetsActivity handles the setup of monthly financial goals and 
 * the distribution of funds into specific category "envelopes".
 * Satisfies the "Data capture" and "Goal setting" POE requirements.
 */
class BudgetsActivity : AppCompatActivity() {

    private val TAG = "BudgetsActivity"
    private lateinit var db: com.example.budgetwise.data.AppDatabase
    private var userId: Int = -1
    private val currentMonth = DateUtils.currentMonth()

    private lateinit var minGoalEditText: TextInputEditText
    private lateinit var maxGoalEditText: TextInputEditText
    private lateinit var categoryLimitsContainer: LinearLayout

    private var categories = listOf<Category>()
    private var categoryLimits = listOf<CategoryBudgetLimit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budgets)

        // Retrieve user context from intent
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Log.e(TAG, "Activity started without a valid User ID. Terminating.")
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db = DatabaseProvider.getDatabase(this)
        Log.d(TAG, "Database initialized for user: $userId")

        initViews()
        loadOverallBudget()
        loadCategoriesAndLimits()
        setupBottomNavigation()
    }

    private fun initViews() {
        minGoalEditText = findViewById(R.id.minGoalEditText)
        maxGoalEditText = findViewById(R.id.maxGoalEditText)
        categoryLimitsContainer = findViewById(R.id.categoryLimitsContainer)
        
        findViewById<Button>(R.id.saveOverallButton).setOnClickListener { saveOverallBudget() }
        findViewById<Button>(R.id.fillEnvelopesButton).setOnClickListener { fillEnvelopesEqually() }
    }

    /**
     * Fetches the monthly overall budget goal from Room database.
     * Uses lifecycleScope.launch to ensure DB operations are off the Main Thread.
     */
    private fun loadOverallBudget() {
        lifecycleScope.launch {
            Log.d(TAG, "Loading overall budget for month: $currentMonth")
            val goal = db.budgetDao().getBudgetGoal(userId, currentMonth)
            runOnUiThread {
                minGoalEditText.setText(goal?.minGoal?.toString() ?: "")
                maxGoalEditText.setText(goal?.maxGoal?.toString() ?: "")
            }
        }
    }

    /**
     * Loads both available categories and existing limits to populate the dynamic UI.
     */
    private fun loadCategoriesAndLimits() {
        lifecycleScope.launch {
            categories = db.categoryDao().getCategoriesByUser(userId)
            categoryLimits = db.categoryBudgetDao().getLimitsForMonth(userId, currentMonth)
            Log.d(TAG, "Found ${categories.size} categories and ${categoryLimits.size} active limits.")

            runOnUiThread {
                populateCategoryLimits()
            }
        }
    }

    /**
     * Dynamically generates UI components for each category.
     * This bypasses ListView focus issues inside a ScrollView.
     */
    private fun populateCategoryLimits() {
        categoryLimitsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (cat in categories) {
            val view = inflater.inflate(R.layout.item_category_limit, categoryLimitsContainer, false)
            val existingLimit = categoryLimits.find { it.categoryId == cat.id }

            val categoryNameText = view.findViewById<TextView>(R.id.categoryNameText)
            val limitEditText = view.findViewById<TextInputEditText>(R.id.limitEditText)
            val saveButton = view.findViewById<Button>(R.id.saveLimitButton)

            categoryNameText.text = cat.name
            limitEditText.setText(existingLimit?.monthlyLimit?.toString() ?: "")

            saveButton.setOnClickListener {
                val newLimit = limitEditText.text.toString().toDoubleOrNull()
                if (newLimit != null) {
                    saveCategoryLimit(cat, newLimit)
                } else {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                }
            }
            categoryLimitsContainer.addView(view)
        }
    }

    private fun saveCategoryLimit(cat: Category, amount: Double) {
        lifecycleScope.launch {
            Log.d(TAG, "Saving limit of R$amount for category: ${cat.name}")
            db.categoryBudgetDao().insertLimit(
                CategoryBudgetLimit(
                    categoryId = cat.id,
                    monthlyLimit = amount,
                    month = currentMonth,
                    userId = userId
                )
            )
            categoryLimits = db.categoryBudgetDao().getLimitsForMonth(userId, currentMonth)
            runOnUiThread {
                Toast.makeText(this@BudgetsActivity, "Limit for ${cat.name} updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Saves the master Min/Max goals for the month.
     */
    private fun saveOverallBudget() {
        val min = minGoalEditText.text.toString().toDoubleOrNull() ?: 0.0
        val max = maxGoalEditText.text.toString().toDoubleOrNull() ?: 0.0
        
        lifecycleScope.launch {
            Log.d(TAG, "Persisting overall budget: Min R$min, Max R$max")
            db.budgetDao().insertBudgetGoal(
                BudgetGoal(minGoal = min, maxGoal = max, month = currentMonth, userId = userId)
            )
            runOnUiThread { 
                Toast.makeText(this@BudgetsActivity, "Overall budget saved", Toast.LENGTH_SHORT).show() 
            }
        }
    }

    /**
     * Requirement: Auto-allocation feature.
     * Logic: Splits the Max Overall Goal equally among all created categories.
     */
    private fun fillEnvelopesEqually() {
        val maxOverall = maxGoalEditText.text.toString().toDoubleOrNull() ?: 0.0
        if (maxOverall <= 0 || categories.isEmpty()) {
            Toast.makeText(this, "Set a max goal and ensure categories exist", Toast.LENGTH_SHORT).show()
            return
        }

        val perCategory = maxOverall / categories.size
        Log.d(TAG, "Performing auto-fill: $perCategory per category for ${categories.size} categories")
        
        lifecycleScope.launch {
            categories.forEach { cat ->
                db.categoryBudgetDao().insertLimit(
                    CategoryBudgetLimit(
                        categoryId = cat.id,
                        monthlyLimit = perCategory,
                        month = currentMonth,
                        userId = userId
                    )
                )
            }
            loadCategoriesAndLimits()
            runOnUiThread {
                Toast.makeText(this@BudgetsActivity, "Categories filled equally", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_budgets
        bottomNav.setOnItemSelectedListener { item ->
            Log.d(TAG, "Navigation selected: ${item.title}")
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java).putExtra("USER_ID", userId))
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    try {
                        startActivity(Intent(this, Class.forName("com.example.budgetwise.TransactionsActivity")).putExtra("USER_ID", userId))
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Module not available", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.nav_budgets -> true
                R.id.nav_insights -> {
                    startActivity(Intent(this, InsightsActivity::class.java).putExtra("USER_ID", userId))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    try {
                        startActivity(Intent(this, Class.forName("com.example.budgetwise.ProfileActivity")).putExtra("USER_ID", userId))
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Module not available", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                else -> false
            }
        }
    }
}
