package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val accountId: Long,
    val toAccountId: Long? = null,
    val category: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val linkedEmiId: Long? = null
)
