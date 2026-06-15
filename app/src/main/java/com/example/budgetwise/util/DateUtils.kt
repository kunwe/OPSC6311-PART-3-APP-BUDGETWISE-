package com.example.budgetwise.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun currentMonth(): String = monthFormat.format(Date())

    fun formatDate(date: Date): String = isoFormat.format(date)

    fun startOfMonth(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return isoFormat.format(cal.time)
    }

    fun endOfMonth(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return isoFormat.format(cal.time)
    }

    fun formatDateForDisplay(dateStr: String): String {
        return try {
            val date = isoFormat.parse(dateStr)
            if (date != null) displayFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
}