package com.example.budgetwise

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.budgetwise.data.DatabaseProvider
import com.example.budgetwise.data.entity.Badge
import com.example.budgetwise.data.entity.Category
import com.example.budgetwise.data.entity.Expense
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TransactionsActivity : AppCompatActivity() {

    private val TAG = "TransactionsActivity"
    private lateinit var db: com.example.budgetwise.data.AppDatabase
    private var userId: Int = -1

    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private val calendarStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private val calendarEnd = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private lateinit var expensesListView: ListView
    private val expenseList = mutableListOf<Expense>()
    private var categories: List<Category> = emptyList()
    
    // Dialog state
    private val dialogCal = Calendar.getInstance()
    private var dialogPhotoUri: Uri? = null
    private var pendingPhotoUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            dialogPhotoUri = pendingPhotoUri
            Toast.makeText(this, "Photo attached", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            dialogPhotoUri = uri
            Toast.makeText(this, "Photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) { finish(); return }

        db = DatabaseProvider.getDatabase(this)
        initUI()
        loadInitialData()
        setupBottomNavigation()
    }

    private fun initUI() {
        startDateButton = findViewById(R.id.startDateButton)
        endDateButton = findViewById(R.id.endDateButton)
        expensesListView = findViewById(R.id.expensesListView)
        
        updateRangeButtonTexts()

        startDateButton.setOnClickListener { showRangeDatePicker(true) }
        endDateButton.setOnClickListener { showRangeDatePicker(false) }
        findViewById<Button>(R.id.goButton).setOnClickListener { loadExpenses() }
        findViewById<Button>(R.id.exportPdfButton).setOnClickListener { exportToPdf() }
        findViewById<FloatingActionButton>(R.id.addExpenseFab).setOnClickListener { showAddExpenseDialog() }
    }

    private fun updateRangeButtonTexts() {
        startDateButton.text = "From: ${dateFormat.format(calendarStart.time)}"
        endDateButton.text = "To: ${dateFormat.format(calendarEnd.time)}"
    }

    private fun showRangeDatePicker(isStart: Boolean) {
        val cal = if (isStart) calendarStart else calendarEnd
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            updateRangeButtonTexts()
            loadExpenses()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            categories = db.categoryDao().getCategoriesByUser(userId)
            loadExpenses()
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val startStr = dateFormat.format(calendarStart.time)
            val endStr = dateFormat.format(calendarEnd.time)
            val expenses = db.expenseDao().getExpensesByPeriod(userId, startStr, endStr)
            
            expenseList.clear()
            expenseList.addAll(expenses)

            runOnUiThread { renderExpenses() }
        }
    }

    private fun renderExpenses() {
        val adapter = object : ArrayAdapter<Expense>(this, R.layout.item_expense, expenseList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false)
                val expense = getItem(position)!!
                val cat = categories.find { it.id == expense.categoryId }
                
                view.findViewById<TextView>(R.id.expenseAmountDate).text = 
                    "R${"%.2f".format(expense.amount)} – ${expense.date} (${cat?.name ?: "Other"})"
                view.findViewById<TextView>(R.id.expenseDescription).text = expense.description
                view.findViewById<ImageView>(R.id.expensePhotoIcon).visibility = 
                    if (expense.photoPath != null) View.VISIBLE else View.GONE
                
                return view
            }
        }
        expensesListView.adapter = adapter
    }

    private fun showAddExpenseDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null)
        val amountEdit = dialogView.findViewById<EditText>(R.id.amountEditText)
        val descEdit = dialogView.findViewById<EditText>(R.id.descriptionEditText)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.categorySpinner)
        
        val dateBtn = dialogView.findViewById<Button>(R.id.dateButton)
        val startBtn = dialogView.findViewById<Button>(R.id.startTimeButton)
        val endBtn = dialogView.findViewById<Button>(R.id.endTimeButton)
        val photoBtn = dialogView.findViewById<Button>(R.id.photoButton)
        val galleryBtn = dialogView.findViewById<Button>(R.id.galleryButton)
        val saveBtn = dialogView.findViewById<Button>(R.id.saveButton)

        dialogCal.time = Date()
        dialogPhotoUri = null
        
        val updateDialogDateTexts = {
            dateBtn.text = "Date: ${dateFormat.format(dialogCal.time)}"
            startBtn.text = "Start: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(dialogCal.time)}"
            val endCal = (dialogCal.clone() as Calendar).apply { add(Calendar.HOUR, 1) }
            endBtn.text = "End: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(endCal.time)}"
        }
        updateDialogDateTexts()

        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories.map { it.name })

        val alertDialog = AlertDialog.Builder(this).setTitle("New Expense").setView(dialogView).create()

        dateBtn.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                dialogCal.set(y, m, d)
                updateDialogDateTexts()
            }, dialogCal.get(Calendar.YEAR), dialogCal.get(Calendar.MONTH), dialogCal.get(Calendar.DAY_OF_MONTH)).show()
        }

        photoBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "receipt_${System.currentTimeMillis()}.jpg")
                pendingPhotoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
                cameraLauncher.launch(pendingPhotoUri)
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
            }
        }

        galleryBtn.setOnClickListener { galleryLauncher.launch("image/*") }

        saveBtn.setOnClickListener {
            val amount = amountEdit.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val catId = if (categorySpinner.selectedItemPosition in categories.indices) categories[categorySpinner.selectedItemPosition].id else 0
            saveTransaction(amount, descEdit.text.toString(), catId)
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun saveTransaction(amount: Double, desc: String, catId: Int) {
        lifecycleScope.launch {
            db.expenseDao().insertExpense(Expense(
                amount = amount,
                date = dateFormat.format(dialogCal.time),
                startTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(dialogCal.time),
                endTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(dialogCal.time), // simplified
                description = desc,
                categoryId = catId,
                photoPath = dialogPhotoUri?.toString(),
                userId = userId
            ))
            checkAndAwardBadges()
            loadExpenses()
        }
    }

    private suspend fun checkAndAwardBadges() {
        val count = db.expenseDao().getExpensesByPeriod(userId, "1900-01-01", "2100-01-01").size
        if (count >= 5) {
            val hasBadge = db.badgeDao().getBadgesForUser(userId).any { it.name == "Power User" }
            if (!hasBadge) {
                db.badgeDao().insertBadge(Badge(
                    userId = userId,
                    name = "Power User",
                    description = "Logged 5 transactions!",
                    dateEarned = dateFormat.format(Date())
                ))
                runOnUiThread { Toast.makeText(this, "🏆 UNLOCKED: Power User!", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun exportToPdf() {
        if (expenseList.isEmpty()) return
        lifecycleScope.launch {
            try {
                val doc = PdfDocument()
                val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
                val canvas = page.canvas
                val paint = android.graphics.Paint()
                canvas.drawText("Expense Report", 50f, 50f, paint)
                var y = 100f
                for (exp in expenseList) {
                    canvas.drawText("${exp.date}: R${exp.amount} - ${exp.description}", 50f, y, paint)
                    y += 30f
                }
                doc.finishPage(page)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "Expenses.pdf")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
                uri?.let { contentResolver.openOutputStream(it)?.use { out -> doc.writeTo(out) } }
                doc.close()
                runOnUiThread { Toast.makeText(this@TransactionsActivity, "PDF Saved", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) { Log.e(TAG, "PDF Error", e) }
        }
    }

    private fun setupBottomNavigation() {
        val nav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        nav.selectedItemId = R.id.nav_transactions
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { startActivity(Intent(this, DashboardActivity::class.java).putExtra("USER_ID", userId)); finish(); true }
                R.id.nav_transactions -> true
                R.id.nav_budgets -> { startActivity(Intent(this, BudgetsActivity::class.java).putExtra("USER_ID", userId)); finish(); true }
                R.id.nav_insights -> { startActivity(Intent(this, InsightsActivity::class.java).putExtra("USER_ID", userId)); finish(); true }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java).putExtra("USER_ID", userId)); finish(); true }
                else -> false
            }
        }
    }
}
