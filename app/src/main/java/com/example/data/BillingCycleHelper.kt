package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BillingCycleRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String,
    val cycleIndexOffset: Int
)

object BillingCycleHelper {

    /**
     * Calculates the billing cycle time window for a given billing day of month (e.g. 20th).
     * @param billingDay Day of month when bill is generated (1 to 31)
     * @param offset 0 for current cycle, -1 for previous cycle, -2 for 2 cycles ago, etc.
     */
    fun getBillingCycle(billingDay: Int, offset: Int = 0): BillingCycleRange {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_MONTH)

        // Determine the end date of the base current cycle
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)

            if (currentDay > billingDay) {
                // If today is past billing day, the current cycle ends next month on billingDay
                add(Calendar.MONTH, 1)
            }
            // Clamp billing day to the maximum days in that month (e.g. Feb 28/29)
            val maxDaysInEndMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, billingDay.coerceIn(1, maxDaysInEndMonth))

            // Apply offset (e.g. -1 for previous cycle)
            add(Calendar.MONTH, offset)
        }

        // Start date is 1 day after previous month's billing day
        val startCal = (endCal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
            val maxDaysInPrevMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
            val prevBillDay = billingDay.coerceIn(1, maxDaysInPrevMonth)
            set(Calendar.DAY_OF_MONTH, prevBillDay)
            add(Calendar.DAY_OF_MONTH, 1) // Day after previous billing day
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val df = SimpleDateFormat("d MMM", Locale.getDefault())
        val label = when (offset) {
            0 -> "Current Cycle (${df.format(Date(startCal.timeInMillis))} – ${df.format(Date(endCal.timeInMillis))})"
            -1 -> "Last Cycle (${df.format(Date(startCal.timeInMillis))} – ${df.format(Date(endCal.timeInMillis))})"
            else -> "${df.format(Date(startCal.timeInMillis))} – ${df.format(Date(endCal.timeInMillis))}"
        }

        return BillingCycleRange(
            startMillis = startCal.timeInMillis,
            endMillis = endCal.timeInMillis,
            label = label,
            cycleIndexOffset = offset
        )
    }

    /**
     * Returns the list of recent cycles (current and past 2 cycles) for selection.
     */
    fun getAvailableCycles(billingDay: Int): List<BillingCycleRange> {
        return listOf(
            getBillingCycle(billingDay, 0),
            getBillingCycle(billingDay, -1),
            getBillingCycle(billingDay, -2)
        )
    }
}
