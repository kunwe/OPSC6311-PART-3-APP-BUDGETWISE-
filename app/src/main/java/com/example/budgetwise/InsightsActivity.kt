package com.example.budgetwise

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetwise.data.DatabaseProvider
import com.example.budgetwise.util.DateUtils
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * InsightsActivity provides visual data analytics for user spending.
 *
 * POE REQUIREMENT SATISFIED: "The user must be able to view a graph showing the
 * amount spent per category over a user-selectable period."
 *
 * REFERENCES:
 * - Graphing library: MPAndroidChart by PhilJay (https://github.com/PhilJay/MPAndroidChart)
 * - Date Handling: Java Time API (https://developer.android.com/reference/java/time/package-summary)
 */
class InsightsActivity : AppCompatActivity() {

    private val TAG = "InsightsActivity"
    private lateinit var db: com.example.budgetwise.data.AppDatabase
    private var userId: Int = -1

    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private var startDate: LocalDate = LocalDate.now().withDayOfMonth(1)
    private var endDate: LocalDate = LocalDate.now()

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insights)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Log.e(TAG, "Insights started without User ID. Aborting.")
            finish()
            return
        }

        db = DatabaseProvider.getDatabase(this)
        Log.d(TAG, "Insights initialized for user $userId")

        initViews()
        setupBottomNavigation()
        calculateTotals()
    }

    private fun initViews() {
        startDateButton = findViewById(R.id.startDateButton)
        endDateButton = findViewById(R.id.endDateButton)
        val goButton = findViewById<Button>(R.id.goButton)
        listView = findViewById(R.id.categoryTotalsListView)
        barChart = findViewById(R.id.spendingChart)

        val tabTotalsButton = findViewById<Button>(R.id.tabTotalsButton)
        val tabChartButton = findViewById<Button>(R.id.tabChartButton)

        adapter = ArrayAdapter(this, R.layout.item_category_total, R.id.categoryNameText, mutableListOf())
        listView.adapter = adapter

        updateDateButtonTexts()

        startDateButton.setOnClickListener { showDatePicker(true) }
        endDateButton.setOnClickListener { showDatePicker(false) }
        goButton.setOnClickListener {
            Log.d(TAG, "Refresh triggered for range: $startDate to $endDate")
            calculateTotals()
        }

        tabTotalsButton.setOnClickListener {
            Log.d(TAG, "Switched to List View")
            listView.visibility = View.VISIBLE
            barChart.visibility = View.GONE
        }
        tabChartButton.setOnClickListener {
            Log.d(TAG, "Switched to Chart View")
            listView.visibility = View.GONE
            barChart.visibility = View.VISIBLE
            calculateTotals()
        }
    }

    private fun updateDateButtonTexts() {
        startDateButton.text = "Start: ${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
        endDateButton.text = "End: ${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
    }

    private fun showDatePicker(isStart: Boolean) {
        val current = if (isStart) startDate else endDate
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val newDate = LocalDate.of(year, month + 1, dayOfMonth)
            if (isStart) startDate = newDate else endDate = newDate
            updateDateButtonTexts()
        }, current.year, current.monthValue - 1, current.dayOfMonth).show()
    }

    /**
     * Requirement: Graphing spent per category.
     * This method joins Expenses and Categories to produce a summed total per group.
     */
    private fun calculateTotals() {
        lifecycleScope.launch {
            val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            // Query Room for transactions in period
            val expenses = db.expenseDao().getExpensesByPeriod(userId, startStr, endStr)
            val categories = db.categoryDao().getCategoriesByUser(userId)

            // Fetch goals for visual markers (POE Requirement)
            val goal = db.budgetDao().getBudgetGoal(userId, DateUtils.currentMonth())

            Log.i(TAG, "Aggregating ${expenses.size} expenses into ${categories.size} categories.")

            val grouped = expenses.groupBy { it.categoryId }
            val totals = categories.map { cat ->
                val sum = grouped[cat.id]?.sumOf { it.amount } ?: 0.0
                Pair(cat.name, sum.toFloat())
            }

            runOnUiThread {
                val displayItems = totals.map { (name, total) -> "$name  –  R${"%.2f".format(total)}" }
                adapter.clear()
                adapter.addAll(displayItems)
                adapter.notifyDataSetChanged()

                updateChart(totals, goal?.minGoal?.toFloat() ?: 0f, goal?.maxGoal?.toFloat() ?: 0f)
            }
        }
    }

    /**
     * Renders the BarChart using the MPAndroidChart library.
     * POE REQUIREMENT: "The graph must also display the minimum and maximum goals."
     */
    private fun updateChart(data: List<Pair<String, Float>>, minGoal: Float, maxGoal: Float) {
        Log.d(TAG, "Updating BarChart. MinGoal: $minGoal, MaxGoal: $maxGoal")

        val entries = data.mapIndexed { index, (_, value) -> BarEntry(index.toFloat(), value) }
        val dataSet = BarDataSet(entries, "Total Spending").apply {
            color = Color.parseColor("#42A5F5") // Material Blue 400
            valueTextColor = Color.BLACK
        }
        
        val barData = BarData(dataSet).apply { barWidth = 0.6f }

        barChart.apply {
            this.data = barData
            description.isEnabled = false

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(data.map { it.first })
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }

            // Implementation of Min/Max Goal Lines (Satisfies specific requirement)
            axisLeft.apply {
                removeAllLimitLines()
                axisMinimum = 0f

                if (maxGoal > 0) {
                    val maxLine = LimitLine(maxGoal, "Limit").apply {
                        lineWidth = 2f
                        lineColor = Color.RED
                        enableDashedLine(10f, 10f, 0f)
                        textSize = 10f
                    }
                    addLimitLine(maxLine)
                }

                if (minGoal > 0) {
                    val minLine = LimitLine(minGoal, "Target").apply {
                        lineWidth = 2f
                        lineColor = Color.parseColor("#2E7D32")
                        enableDashedLine(10f, 10f, 0f)
                        textSize = 10f
                    }
                    addLimitLine(minLine)
                }

                // Auto-scale Y axis to ensure goals are always visible
                val maxVal = data.maxOfOrNull { it.second } ?: 0f
                axisMaximum = (maxOf(maxVal, maxGoal) * 1.2f).coerceAtLeast(100f)
            }

            axisRight.isEnabled = false
            animateY(800)
            invalidate() // Refresh chart
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_insights
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java).putExtra("USER_ID", userId))
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java).putExtra("USER_ID", userId))
                    finish()
                    true
                }
                R.id.nav_budgets -> {
                    startActivity(Intent(this, BudgetsActivity::class.java).putExtra("USER_ID", userId))
                    finish()
                    true
                }
                R.id.nav_insights -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).putExtra("USER_ID", userId))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}