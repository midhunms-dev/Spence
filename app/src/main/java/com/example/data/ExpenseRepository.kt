package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ExpenseRepository(private val dao: ExpenseDao) {

    val allAccounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = dao.getRecentTransactions(30)
    val allEmis: Flow<List<EmiEntity>> = dao.getAllEmis()
    val activeEmis: Flow<List<EmiEntity>> = dao.getActiveEmis()

    suspend fun getAccountById(id: Long): AccountEntity? = dao.getAccountById(id)

    suspend fun insertAccount(account: AccountEntity): Long = dao.insertAccount(account)

    suspend fun updateAccount(account: AccountEntity) = dao.updateAccount(account)

    suspend fun deleteAccount(account: AccountEntity) = dao.deleteAccount(account)

    suspend fun addExpense(
        amount: Double,
        accountId: Long,
        category: String,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long = dao.addExpenseTransaction(amount, accountId, category, note, timestamp)

    suspend fun addIncome(
        amount: Double,
        accountId: Long,
        category: String,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long = dao.addIncomeTransaction(amount, accountId, category, note, timestamp)

    suspend fun addTransfer(
        amount: Double,
        fromAccountId: Long,
        toAccountId: Long,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long = dao.addTransferTransaction(amount, fromAccountId, toAccountId, note, timestamp)

    suspend fun payCreditCardBill(
        creditCardAccountId: Long,
        sourceAccountId: Long,
        amount: Double,
        note: String = "Credit Card Bill Payment",
        timestamp: Long = System.currentTimeMillis()
    ): Long = dao.addTransferTransaction(amount, sourceAccountId, creditCardAccountId, note, timestamp)

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.removeTransactionAndRevertBalance(transaction)
    }

    suspend fun insertEmi(emi: EmiEntity): Long = dao.insertEmi(emi)

    suspend fun updateEmi(emi: EmiEntity) = dao.updateEmi(emi)

    suspend fun deleteEmi(emi: EmiEntity) = dao.deleteEmi(emi)

    suspend fun processEmiPayment(emiId: Long, monthYearKey: String): Boolean {
        return dao.processEmiPayment(emiId, monthYearKey)
    }

    /**
     * Checks and automatically executes due EMIs for the given month/year and day.
     * Returns the count of processed EMIs.
     */
    suspend fun checkAndAutoDeductDueEmis(currentMonthYear: String, currentDay: Int): Int {
        val active = dao.getActiveEmis().first()
        var processedCount = 0
        for (emi in active) {
            if (emi.autoDeduct &&
                emi.dueDayOfMonth <= currentDay &&
                emi.lastDeductedMonthYear != currentMonthYear &&
                !emi.isCompleted
            ) {
                val success = dao.processEmiPayment(emi.id, currentMonthYear)
                if (success) {
                    processedCount++
                }
            }
        }
        return processedCount
    }
}
