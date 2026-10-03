package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emis")
data class EmiEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val monthlyAmount: Double,
    val totalInstallments: Int,
    val paidInstallments: Int = 0,
    val dueDayOfMonth: Int = 1,
    // ID of the Credit Card or Bank account from which this EMI is deducted
    val linkedAccountId: Long,
    // Optional ID of a Loan account if this EMI is for paying off a Loan
    val linkedLoanAccountId: Long? = null,
    val autoDeduct: Boolean = true,
    // Tracks month-year like "2026-10" to avoid duplicate auto-cuts
    val lastDeductedMonthYear: String = "",
    val isActive: Boolean = true,
    val startDate: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val remainingInstallments: Int
        get() = (totalInstallments - paidInstallments).coerceAtLeast(0)

    val isCompleted: Boolean
        get() = paidInstallments >= totalInstallments

    val progress: Float
        get() = if (totalInstallments > 0) {
            (paidInstallments.toFloat() / totalInstallments).coerceIn(0f, 1f)
        } else 0f

    val totalAmount: Double
        get() = monthlyAmount * totalInstallments

    val paidAmount: Double
        get() = monthlyAmount * paidInstallments

    val remainingAmount: Double
        get() = monthlyAmount * remainingInstallments
}
