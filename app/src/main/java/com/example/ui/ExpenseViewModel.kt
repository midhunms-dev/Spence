package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.BillingCycleHelper
import com.example.data.BillingCycleRange
import com.example.data.EmiEntity
import com.example.data.ExpenseDatabase
import com.example.data.ExpenseRepository
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePreferences
import com.example.util.BackupFrequency
import com.example.util.BackupManager
import com.example.util.NotificationHelper
import com.example.widget.ExpenseWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CategorySpendItem(
    val category: String,
    val amount: Double,
    val percentage: Float
)

data class FinanceOverview(
    val totalLiquidBalance: Double = 0.0,
    val totalCreditLimit: Double = 0.0,
    val totalCreditAvailable: Double = 0.0,
    val totalCreditUsed: Double = 0.0,
    val totalLoanOutstanding: Double = 0.0,
    val thisMonthExpense: Double = 0.0,
    val thisMonthIncome: Double = 0.0,
    val pendingDueEmisCount: Int = 0
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val database: ExpenseDatabase
    val backupManager: BackupManager = BackupManager(application)
    val themePreferences: ThemePreferences = ThemePreferences(application)

    val accounts: StateFlow<List<AccountEntity>>
    val transactions: StateFlow<List<TransactionEntity>>
    val recentTransactions: StateFlow<List<TransactionEntity>>
    val emis: StateFlow<List<EmiEntity>>

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode

    private val _backupFrequency = MutableStateFlow(backupManager.getBackupFrequency())
    val backupFrequency: StateFlow<BackupFrequency> = _backupFrequency.asStateFlow()

    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage: StateFlow<String?> = _notificationMessage.asStateFlow()

    init {
        database = ExpenseDatabase.getDatabase(application, viewModelScope)
        repository = ExpenseRepository(database.expenseDao())

        accounts = repository.allAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        transactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        recentTransactions = repository.recentTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        emis = repository.allEmis
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Run auto-deduct and notification check on launch
        checkAndTriggerAutoEmiDeductions()
        checkEmiReminders()
    }

    private fun getCurrentMonthYearKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    private fun getCurrentDayOfMonth(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }

    val financeOverview: StateFlow<FinanceOverview> = combine(
        accounts,
        transactions,
        emis
    ) { accList, txList, emiList ->
        var liquid = 0.0
        var ccLimit = 0.0
        var ccAvailable = 0.0
        var ccUsed = 0.0
        var loanTotal = 0.0

        for (acc in accList) {
            when (acc.type) {
                AccountType.BANK, AccountType.CASH -> liquid += acc.balance
                AccountType.CREDIT_CARD -> {
                    ccLimit += acc.creditLimit
                    ccAvailable += acc.balance
                    ccUsed += acc.usedCredit
                }
                AccountType.LOAN -> loanTotal += acc.balance
            }
        }

        // Calculate this month's spending
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH)
        val txCal = Calendar.getInstance()

        var monthExpense = 0.0
        var monthIncome = 0.0

        for (tx in txList) {
            txCal.timeInMillis = tx.timestamp
            if (txCal.get(Calendar.YEAR) == curYear && txCal.get(Calendar.MONTH) == curMonth) {
                when (tx.type) {
                    TransactionType.EXPENSE, TransactionType.EMI_PAYMENT -> monthExpense += tx.amount
                    TransactionType.INCOME -> monthIncome += tx.amount
                    else -> {}
                }
            }
        }

        val curKey = getCurrentMonthYearKey()
        val curDay = getCurrentDayOfMonth()
        val pendingCount = emiList.count {
            it.isActive && !it.isCompleted && it.dueDayOfMonth <= curDay && it.lastDeductedMonthYear != curKey
        }

        // Update home screen widget
        ExpenseWidgetProvider.updateWidgetBalances(
            getApplication(),
            liquidBalance = liquid,
            creditAvailable = ccAvailable
        )

        FinanceOverview(
            totalLiquidBalance = liquid,
            totalCreditLimit = ccLimit,
            totalCreditAvailable = ccAvailable,
            totalCreditUsed = ccUsed,
            totalLoanOutstanding = loanTotal,
            thisMonthExpense = monthExpense,
            thisMonthIncome = monthIncome,
            pendingDueEmisCount = pendingCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceOverview())

    val categoryBreakdown: StateFlow<List<CategorySpendItem>> = transactions.combine(accounts) { txList, _ ->
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH)
        val txCal = Calendar.getInstance()

        val spendByCategory = mutableMapOf<String, Double>()
        var total = 0.0

        for (tx in txList) {
            txCal.timeInMillis = tx.timestamp
            if (txCal.get(Calendar.YEAR) == curYear && txCal.get(Calendar.MONTH) == curMonth) {
                if (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.EMI_PAYMENT) {
                    val cat = tx.category.ifBlank { "Other" }
                    spendByCategory[cat] = (spendByCategory[cat] ?: 0.0) + tx.amount
                    total += tx.amount
                }
            }
        }

        spendByCategory.entries
            .map { (cat, amt) ->
                CategorySpendItem(
                    category = cat,
                    amount = amt,
                    percentage = if (total > 0) ((amt / total) * 100f).toFloat() else 0f
                )
            }
            .sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun setBackupFrequency(frequency: BackupFrequency) {
        backupManager.setBackupFrequency(frequency)
        _backupFrequency.value = frequency
    }

    fun clearNotification() {
        _notificationMessage.value = null
    }

    fun checkEmiReminders() {
        viewModelScope.launch {
            val emiList = emis.value
            val accMap = accounts.value.associateBy { it.id }
            if (emiList.isNotEmpty()) {
                NotificationHelper.checkAndNotifyDueEmis(getApplication(), emiList, accMap)
            }
        }
    }

    fun checkAndTriggerAutoEmiDeductions() {
        viewModelScope.launch {
            val key = getCurrentMonthYearKey()
            val day = getCurrentDayOfMonth()
            val count = repository.checkAndAutoDeductDueEmis(key, day)
            if (count > 0) {
                _notificationMessage.value = "Automatically processed $count due EMI(s)!"
            }
        }
    }

    fun addExpense(
        amount: Double,
        accountId: Long,
        category: String,
        note: String
    ) {
        viewModelScope.launch {
            repository.addExpense(amount, accountId, category, note)
            _notificationMessage.value = "Expense of ₹$amount recorded"
        }
    }

    fun addIncome(
        amount: Double,
        accountId: Long,
        category: String,
        note: String
    ) {
        viewModelScope.launch {
            repository.addIncome(amount, accountId, category, note)
            _notificationMessage.value = "Income of ₹$amount added"
        }
    }

    fun addTransfer(
        amount: Double,
        fromAccountId: Long,
        toAccountId: Long,
        note: String
    ) {
        viewModelScope.launch {
            repository.addTransfer(amount, fromAccountId, toAccountId, note)
            _notificationMessage.value = "Transferred ₹$amount successfully"
        }
    }

    fun payCreditCardBill(
        creditCardAccountId: Long,
        sourceAccountId: Long,
        amount: Double
    ) {
        viewModelScope.launch {
            repository.payCreditCardBill(creditCardAccountId, sourceAccountId, amount)
            _notificationMessage.value = "Credit card bill of ₹$amount paid! Credit limit restored."
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _notificationMessage.value = "Transaction removed and balances restored"
        }
    }

    fun addAccount(
        name: String,
        type: AccountType,
        balance: Double,
        creditLimit: Double,
        billingDay: Int,
        last4: String
    ) {
        viewModelScope.launch {
            val color = when (type) {
                AccountType.BANK -> 0xFF0D9488
                AccountType.CASH -> 0xFF10B981
                AccountType.CREDIT_CARD -> 0xFF6366F1
                AccountType.LOAN -> 0xFFF59E0B
            }
            val initialBalance = if (type == AccountType.CREDIT_CARD) {
                if (balance > 0) balance else creditLimit
            } else balance

            repository.insertAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    balance = initialBalance,
                    creditLimit = creditLimit,
                    billingDay = billingDay,
                    accountNumberLast4 = last4,
                    colorHex = color
                )
            )
            _notificationMessage.value = "Account '$name' created"
        }
    }

    fun updateCreditCardDetails(accountId: Long, newLimit: Double, newBillingDay: Int) {
        viewModelScope.launch {
            val acc = repository.getAccountById(accountId) ?: return@launch
            if (acc.type == AccountType.CREDIT_CARD) {
                val used = acc.usedCredit
                val newAvailable = (newLimit - used).coerceAtLeast(0.0)
                repository.updateAccount(
                    acc.copy(
                        creditLimit = newLimit,
                        balance = newAvailable,
                        billingDay = newBillingDay
                    )
                )
                _notificationMessage.value = "Credit Card details updated (Limit: ₹$newLimit, Bill Day: ${newBillingDay}th)"
            }
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            _notificationMessage.value = "Account '${account.name}' deleted"
        }
    }

    fun addEmi(
        title: String,
        monthlyAmount: Double,
        totalInstallments: Int,
        paidInstallments: Int,
        dueDayOfMonth: Int,
        linkedAccountId: Long,
        linkedLoanAccountId: Long?,
        autoDeduct: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            repository.insertEmi(
                EmiEntity(
                    title = title,
                    monthlyAmount = monthlyAmount,
                    totalInstallments = totalInstallments,
                    paidInstallments = paidInstallments,
                    dueDayOfMonth = dueDayOfMonth,
                    linkedAccountId = linkedAccountId,
                    linkedLoanAccountId = linkedLoanAccountId,
                    autoDeduct = autoDeduct,
                    notes = notes
                )
            )
            _notificationMessage.value = "EMI '$title' added (₹$monthlyAmount/mo)"
            checkEmiReminders()
        }
    }

    fun processEmiPaymentManually(emiId: Long) {
        viewModelScope.launch {
            val key = getCurrentMonthYearKey()
            val success = repository.processEmiPayment(emiId, key)
            if (success) {
                _notificationMessage.value = "EMI payment processed successfully!"
            } else {
                _notificationMessage.value = "Could not process EMI payment (already paid or completed)"
            }
        }
    }

    fun deleteEmi(emi: EmiEntity) {
        viewModelScope.launch {
            repository.deleteEmi(emi)
            _notificationMessage.value = "EMI '${emi.title}' removed"
        }
    }

    // --- Backup & Restore ---
    fun getBackupJson(): String {
        return backupManager.generateBackupJson(
            accounts = accounts.value,
            transactions = transactions.value,
            emis = emis.value
        )
    }

    fun restoreBackupFromJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreFromJson(jsonString, database)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _notificationMessage.value = "Successfully restored $count items from backup!"
                onComplete(true, "Restored $count items")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown error"
                _notificationMessage.value = "Backup restore failed: $err"
                onComplete(false, err)
            }
        }
    }
}
