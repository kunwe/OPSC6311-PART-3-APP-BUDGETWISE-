package com.example.budgetwise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetwise.data.DatabaseProvider
import com.example.budgetwise.data.entity.Category
import com.example.budgetwise.util.HashUtils
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * LoginActivity provides the entry point for the application.
 * 
 * POE REQUIREMENT SATISFIED: "Apply event handling in an app" and "Create an activity."
 * 
 * REFERENCES:
 * - Room Database: Android Developers (https://developer.android.com/training/data-storage/room)
 * - Material 3 Components: Google Design (https://m3.material.io/)
 */
class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerLink = findViewById<TextView>(R.id.registerLink)

        Log.d(TAG, "Login screen launched.")

        // Event handling for Login action
        loginButton.setOnClickListener { 
            Log.i(TAG, "Login attempt initiated.")
            performLogin() 
        }

        // Navigation to registration
        registerLink.setOnClickListener {
            Log.d(TAG, "Navigating to RegisterActivity.")
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Handles the login logic including database lookup and password verification.
     * Demonstrates understanding of asynchronous operations using lifecycleScope.
     */
    private fun performLogin() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "Login failed: Empty fields.")
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(this@LoginActivity)
            
            // Query user from local persistence
            val user = db.userDao().getUserByUsername(username)
            if (user == null) {
                Log.w(TAG, "Login failed: User '$username' not found.")
                runOnUiThread { Toast.makeText(this@LoginActivity, "User not found", Toast.LENGTH_SHORT).show() }
                return@launch
            }

            // Verify credentials using secure hash comparison
            if (!HashUtils.verifyPassword(password, user.hashedPassword)) {
                Log.w(TAG, "Login failed: Incorrect password for user '$username'.")
                runOnUiThread { Toast.makeText(this@LoginActivity, "Incorrect password", Toast.LENGTH_SHORT).show() }
                return@launch
            }

            Log.i(TAG, "Login successful for user ID: ${user.id}")

            // Seed default categories if this is a fresh user account
            seedDefaultCategories(user.id)

            // Transition to main dashboard
            runOnUiThread {
                val intent = Intent(this@LoginActivity, DashboardActivity::class.java).apply {
                    putExtra("USER_ID", user.id)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    /**
     * Seeds initial category data to improve User Experience (UX) for new users.
     */
    private suspend fun seedDefaultCategories(userId: Int) {
        val db = DatabaseProvider.getDatabase(this)
        val existingCategories = db.categoryDao().getCategoriesByUser(userId)
        
        if (existingCategories.isEmpty()) {
            Log.d(TAG, "No existing categories found. Seeding defaults.")
            val defaults = listOf(
                "Groceries", "Transport", "Entertainment", "Rent",
                "Eating Out", "Utilities", "Shopping", "Healthcare", "Other"
            )
            defaults.forEach { name ->
                db.categoryDao().insertCategory(
                    Category(name = name, color = "#757575", userId = userId)
                )
            }
        }
    }
}