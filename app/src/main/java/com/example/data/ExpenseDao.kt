package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // --- Accounts ---
    @Query("SELECT * FROM accounts ORDER BY type ASC, name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 30): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // --- EMIs ---
    @Query("SELECT * FROM emis ORDER BY isActive DESC, dueDayOfMonth ASC")
    fun getAllEmis(): Flow<List<EmiEntity>>

    @Query("SELECT * FROM emis WHERE isActive = 1 ORDER BY dueDayOfMonth ASC")
    fun getActiveEmis(): Flow<List<EmiEntity>>

    @Query("SELECT * FROM emis WHERE id = :id")
    suspend fun getEmiById(id: Long): EmiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmi(emi: EmiEntity): Long

    @Update
    suspend fun updateEmi(emi: EmiEntity)

    @Delete
    suspend fun deleteEmi(emi: EmiEntity)

    // --- High-Level Transactional Operations ---

    @Transaction
    suspend fun addExpenseTransaction(
        amount: Double,
        accountId: Long,
        category: String,
        note: String,
        timestamp: Long
    ): Long {
        val account = getAccountById(accountId) ?: return -1
        val updatedAccount = when (account.type) {
            AccountType.CREDIT_CARD -> {
                // Spending reduces available credit limit
                account.copy(balance = account.balance - amount)
            }
            AccountType.BANK, AccountType.CASH -> {
                // Spending reduces bank/cash balance
                account.copy(balance = account.balance - amount)
            }
            AccountType.LOAN -> {
                // Directly paying down a loan
                account.copy(balance = (account.balance - amount).coerceAtLeast(0.0))
            }
        }
        updateAccount(updatedAccount)

        return insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = amount,
                accountId = accountId,
                category = category,
                note = note,
                timestamp = timestamp
            )
        )
    }

    @Transaction
    suspend fun addIncomeTransaction(
        amount: Double,
        accountId: Long,
        category: String,
        note: String,
        timestamp: Long
    ): Long {
        val account = getAccountById(accountId) ?: return -1
        val updatedAccount = when (account.type) {
            AccountType.BANK, AccountType.CASH -> {
                account.copy(balance = account.balance + amount)
            }
            AccountType.CREDIT_CARD -> {
                // Cashback or refund adds back to available credit
                account.copy(balance = (account.balance + amount).coerceAtMost(account.creditLimit))
            }
            AccountType.LOAN -> account
        }
        updateAccount(updatedAccount)

        return insertTransaction(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = amount,
                accountId = accountId,
                category = category,
                note = note,
                timestamp = timestamp
            )
        )
    }

    @Transaction
    suspend fun addTransferTransaction(
        amount: Double,
        fromAccountId: Long,
        toAccountId: Long,
        note: String,
        timestamp: Long
    ): Long {
        val fromAccount = getAccountById(fromAccountId) ?: return -1
        val toAccount = getAccountById(toAccountId) ?: return -1

        // Deduct from sender
        val updatedFrom = fromAccount.copy(balance = fromAccount.balance - amount)
        // Add to receiver (if CC, restores available credit up to credit limit)
        val updatedTo = if (toAccount.type == AccountType.CREDIT_CARD) {
            toAccount.copy(balance = (toAccount.balance + amount).coerceAtMost(toAccount.creditLimit))
        } else {
            toAccount.copy(balance = toAccount.balance + amount)
        }

        updateAccount(updatedFrom)
        updateAccount(updatedTo)

        val txType = if (toAccount.type == AccountType.CREDIT_CARD) {
            TransactionType.CC_BILL_PAYMENT
        } else {
            TransactionType.TRANSFER
        }

        return insertTransaction(
            TransactionEntity(
                type = txType,
                amount = amount,
                accountId = fromAccountId,
                toAccountId = toAccountId,
                category = if (txType == TransactionType.CC_BILL_PAYMENT) "Bills & Utilities" else "Transfer",
                note = note,
                timestamp = timestamp
            )
        )
    }

    @Transaction
    suspend fun processEmiPayment(
        emiId: Long,
        monthYearKey: String,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean {
        val emi = getEmiById(emiId) ?: return false
        if (!emi.isActive || emi.isCompleted) return false

        val linkedAccount = getAccountById(emi.linkedAccountId) ?: return false

        // Deduct EMI amount from linked account (Credit Card limit or Bank balance)
        val updatedLinkedAccount = when (linkedAccount.type) {
            AccountType.CREDIT_CARD -> {
                // Spending/charging EMI reduces available credit card limit
                linkedAccount.copy(balance = linkedAccount.balance - emi.monthlyAmount)
            }
            AccountType.BANK, AccountType.CASH -> {
                linkedAccount.copy(balance = linkedAccount.balance - emi.monthlyAmount)
            }
            AccountType.LOAN -> linkedAccount
        }
        updateAccount(updatedLinkedAccount)

        // If this EMI is clubbed to pay off a separate Loan account, reduce the loan outstanding
        if (emi.linkedLoanAccountId != null) {
            val loanAccount = getAccountById(emi.linkedLoanAccountId)
            if (loanAccount != null) {
                val updatedLoan = loanAccount.copy(
                    balance = (loanAccount.balance - emi.monthlyAmount).coerceAtLeast(0.0)
                )
                updateAccount(updatedLoan)
            }
        }

        val newPaidInstallments = emi.paidInstallments + 1
        val newIsActive = newPaidInstallments < emi.totalInstallments

        val updatedEmi = emi.copy(
            paidInstallments = newPaidInstallments,
            isActive = newIsActive,
            lastDeductedMonthYear = monthYearKey
        )
        updateEmi(updatedEmi)

        // Record the transaction
        insertTransaction(
            TransactionEntity(
                type = TransactionType.EMI_PAYMENT,
                amount = emi.monthlyAmount,
                accountId = emi.linkedAccountId,
                toAccountId = emi.linkedLoanAccountId,
                category = "EMI / Loan Payment",
                note = "EMI Payment (${newPaidInstallments}/${emi.totalInstallments}): ${emi.title}",
                timestamp = timestamp,
                linkedEmiId = emiId
            )
        )

        return true
    }

    @Transaction
    suspend fun removeTransactionAndRevertBalance(transaction: TransactionEntity) {
        val account = getAccountById(transaction.accountId)
        if (account != null) {
            when (transaction.type) {
                TransactionType.EXPENSE -> {
                    // Revert expense -> add back to balance / available credit
                    val restoredAccount = when (account.type) {
                        AccountType.CREDIT_CARD -> account.copy(balance = (account.balance + transaction.amount).coerceAtMost(account.creditLimit))
                        else -> account.copy(balance = account.balance + transaction.amount)
                    }
                    updateAccount(restoredAccount)
                }
                TransactionType.INCOME -> {
                    // Revert income -> subtract from balance
                    updateAccount(account.copy(balance = account.balance - transaction.amount))
                }
                TransactionType.TRANSFER, TransactionType.CC_BILL_PAYMENT -> {
                    // Revert transfer: refund fromAccount, deduct toAccount
                    updateAccount(account.copy(balance = account.balance + transaction.amount))
                    if (transaction.toAccountId != null) {
                        val toAccount = getAccountById(transaction.toAccountId)
                        if (toAccount != null) {
                            updateAccount(toAccount.copy(balance = toAccount.balance - transaction.amount))
                        }
                    }
                }
                TransactionType.EMI_PAYMENT -> {
                    // Revert EMI deduction
                    val restoredAccount = when (account.type) {
                        AccountType.CREDIT_CARD -> account.copy(balance = (account.balance + transaction.amount).coerceAtMost(account.creditLimit))
                        else -> account.copy(balance = account.balance + transaction.amount)
                    }
                    updateAccount(restoredAccount)

                    // Revert loan balance if linked
                    if (transaction.toAccountId != null) {
                        val loanAccount = getAccountById(transaction.toAccountId)
                        if (loanAccount != null) {
                            updateAccount(loanAccount.copy(balance = loanAccount.balance + transaction.amount))
                        }
                    }

                    // Decrement EMI paid count if linked
                    if (transaction.linkedEmiId != null) {
                        val emi = getEmiById(transaction.linkedEmiId)
                        if (emi != null) {
                            updateEmi(
                                emi.copy(
                                    paidInstallments = (emi.paidInstallments - 1).coerceAtLeast(0),
                                    isActive = true
                                )
                            )
                        }
                    }
                }
            }
        }
        deleteTransaction(transaction)
    }
}
