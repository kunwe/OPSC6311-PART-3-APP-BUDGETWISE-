package com.example.budgetwise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetwise.data.DatabaseProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

/**
 * ProfileActivity manages the user's personal information and earned rewards.
 * 
 * POE REQUIREMENT SATISFIED: "Gamification is integrated into the application"
 * (Visual display of badges and achievements).
 * 
 * REFERENCES:
 * - Intent Flags: Activity lifecycle management (https://developer.android.com/guide/components/activities/tasks-and-back-stack)
 * - Room Querying: Data retrieval for achievements (https://developer.android.com/training/data-storage/room/accessing-data)
 */
class ProfileActivity : AppCompatActivity() {

    private val TAG = "ProfileActivity"
    private lateinit var db: com.example.budgetwise.data.AppDatabase
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Context check: Ensure a user is logged in
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Log.e(TAG, "Profile accessed without session. Redirecting.")
            finish()
            return
        }

        db = DatabaseProvider.getDatabase(this)
        Log.d(TAG, "Profile loaded for user ID: $userId")

        val badgesListView = findViewById<ListView>(R.id.badgesListView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        // Event handling for user logout
        logoutButton.setOnClickListener {
            Log.i(TAG, "User initiated logout.")
            performLogout()
        }

        loadBadges(badgesListView)
        setupBottomNavigation()
    }

    /**
     * Requirement: Rewards/Badges display.
     * Fetches earned badges from Room and displays them in a logical list.
     */
    private fun loadBadges(listView: ListView) {
        lifecycleScope.launch {
            Log.d(TAG, "Fetching achievement badges from database...")
            val badges = db.badgeDao().getBadgesForUser(userId)
            
            val badgeNames = badges.map { "${it.name}\n${it.description} (${it.dateEarned})" }
            Log.i(TAG, "User has earned ${badges.size} badges.")

            runOnUiThread {
                val badgeAdapter = ArrayAdapter(this@ProfileActivity, android.R.layout.simple_list_item_1, badgeNames)
                listView.adapter = badgeAdapter
            }
        }
    }

    /**
     * Clears the backstack and returns the user to the Login screen.
     */
    private fun performLogout() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_profile
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
                R.id.nav_insights -> {
                    startActivity(Intent(this, InsightsActivity::class.java).putExtra("USER_ID", userId))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }
}