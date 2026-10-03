package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    // For Bank & Cash: current money balance.
    // For Credit Card: current available credit limit.
    // For Loan: current remaining outstanding balance.
    val balance: Double,
    // For Credit Card: total sanctioned credit limit.
    // For Loan: total principal loan sanctioned amount.
    val creditLimit: Double = 0.0,
    val billingDay: Int = 1,
    val accountNumberLast4: String = "",
    val colorHex: Long = 0xFF0F766E,
    val createdAt: Long = System.currentTimeMillis()
) {
    // For Credit Card: spent / used amount = creditLimit - balance (available limit)
    val usedCredit: Double
        get() = if (type == AccountType.CREDIT_CARD) {
            (creditLimit - balance).coerceAtLeast(0.0)
        } else 0.0

    // Percentage of limit used
    val creditUsedPercentage: Float
        get() = if (type == AccountType.CREDIT_CARD && creditLimit > 0) {
            ((usedCredit / creditLimit) * 100f).coerceIn(0.0, 100.0).toFloat()
        } else 0f
}
