package com.example.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        format.minimumFractionDigits = 0
        return format.format(amount).replace("INR", "₹").trim()
    }

    fun formatDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val txDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            isSameDay(now, txDate) -> {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                "Today, ${timeFormat.format(Date(timestamp))}"
            }
            isYesterday(now, txDate) -> {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                "Yesterday, ${timeFormat.format(Date(timestamp))}"
            }
            now.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) -> {
                val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
            else -> {
                val fullFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                fullFormat.format(Date(timestamp))
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, past: Calendar): Boolean {
        val clone = now.clone() as Calendar
        clone.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(clone, past)
    }
}
