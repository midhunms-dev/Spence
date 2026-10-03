package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.EmiEntity
import com.example.data.ExpenseDatabase
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BackupFrequency(val title: String, val intervalDays: Int) {
    OFF("Manual Only", 0),
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30)
}

class BackupManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("expense_backup_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FREQUENCY = "backup_frequency"
        private const val KEY_LAST_BACKUP = "last_backup_timestamp"
    }

    fun getBackupFrequency(): BackupFrequency {
        val name = prefs.getString(KEY_FREQUENCY, BackupFrequency.WEEKLY.name)
        return try {
            BackupFrequency.valueOf(name ?: BackupFrequency.WEEKLY.name)
        } catch (e: Exception) {
            BackupFrequency.WEEKLY
        }
    }

    fun setBackupFrequency(frequency: BackupFrequency) {
        prefs.edit().putString(KEY_FREQUENCY, frequency.name).apply()
    }

    fun getLastBackupTimestamp(): Long {
        return prefs.getLong(KEY_LAST_BACKUP, 0L)
    }

    fun markBackupCompleted() {
        prefs.edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply()
    }

    fun isAutoBackupDue(): Boolean {
        val freq = getBackupFrequency()
        if (freq == BackupFrequency.OFF) return false
        val last = getLastBackupTimestamp()
        if (last == 0L) return true
        val diffDays = (System.currentTimeMillis() - last) / (1000 * 60 * 60 * 24)
        return diffDays >= freq.intervalDays
    }

    fun generateBackupJson(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        emis: List<EmiEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "Spence")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // Accounts
        val accountsArray = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("type", acc.type.name)
            obj.put("balance", acc.balance)
            obj.put("creditLimit", acc.creditLimit)
            obj.put("billingDay", acc.billingDay)
            obj.put("accountNumberLast4", acc.accountNumberLast4)
            obj.put("colorHex", acc.colorHex)
            obj.put("createdAt", acc.createdAt)
            accountsArray.put(obj)
        }
        root.put("accounts", accountsArray)

        // Transactions
        val transactionsArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("type", tx.type.name)
            obj.put("amount", tx.amount)
            obj.put("accountId", tx.accountId)
            if (tx.toAccountId != null) obj.put("toAccountId", tx.toAccountId)
            obj.put("category", tx.category)
            obj.put("note", tx.note)
            obj.put("timestamp", tx.timestamp)
            if (tx.linkedEmiId != null) obj.put("linkedEmiId", tx.linkedEmiId)
            transactionsArray.put(obj)
        }
        root.put("transactions", transactionsArray)

        // EMIs
        val emisArray = JSONArray()
        for (emi in emis) {
            val obj = JSONObject()
            obj.put("id", emi.id)
            obj.put("title", emi.title)
            obj.put("monthlyAmount", emi.monthlyAmount)
            obj.put("totalInstallments", emi.totalInstallments)
            obj.put("paidInstallments", emi.paidInstallments)
            obj.put("dueDayOfMonth", emi.dueDayOfMonth)
            obj.put("linkedAccountId", emi.linkedAccountId)
            if (emi.linkedLoanAccountId != null) obj.put("linkedLoanAccountId", emi.linkedLoanAccountId)
            obj.put("autoDeduct", emi.autoDeduct)
            obj.put("lastDeductedMonthYear", emi.lastDeductedMonthYear)
            obj.put("isActive", emi.isActive)
            obj.put("startDate", emi.startDate)
            obj.put("notes", emi.notes)
            emisArray.put(obj)
        }
        root.put("emis", emisArray)

        return root.toString(2)
    }

    suspend fun restoreFromJson(
        jsonString: String,
        database: ExpenseDatabase
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val dao = database.expenseDao()

            val accountsArray = root.optJSONArray("accounts") ?: JSONArray()
            val transactionsArray = root.optJSONArray("transactions") ?: JSONArray()
            val emisArray = root.optJSONArray("emis") ?: JSONArray()

            var count = 0

            for (i in 0 until accountsArray.length()) {
                val obj = accountsArray.getJSONObject(i)
                val acc = AccountEntity(
                    id = obj.optLong("id", 0L),
                    name = obj.getString("name"),
                    type = AccountType.valueOf(obj.getString("type")),
                    balance = obj.getDouble("balance"),
                    creditLimit = obj.optDouble("creditLimit", 0.0),
                    billingDay = obj.optInt("billingDay", 1),
                    accountNumberLast4 = obj.optString("accountNumberLast4", ""),
                    colorHex = obj.optLong("colorHex", 0xFF0F766E),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                dao.insertAccount(acc)
                count++
            }

            for (i in 0 until transactionsArray.length()) {
                val obj = transactionsArray.getJSONObject(i)
                val tx = TransactionEntity(
                    id = obj.optLong("id", 0L),
                    type = TransactionType.valueOf(obj.getString("type")),
                    amount = obj.getDouble("amount"),
                    accountId = obj.getLong("accountId"),
                    toAccountId = if (obj.has("toAccountId")) obj.getLong("toAccountId") else null,
                    category = obj.getString("category"),
                    note = obj.optString("note", ""),
                    timestamp = obj.getLong("timestamp"),
                    linkedEmiId = if (obj.has("linkedEmiId")) obj.getLong("linkedEmiId") else null
                )
                dao.insertTransaction(tx)
                count++
            }

            for (i in 0 until emisArray.length()) {
                val obj = emisArray.getJSONObject(i)
                val emi = EmiEntity(
                    id = obj.optLong("id", 0L),
                    title = obj.getString("title"),
                    monthlyAmount = obj.getDouble("monthlyAmount"),
                    totalInstallments = obj.getInt("totalInstallments"),
                    paidInstallments = obj.optInt("paidInstallments", 0),
                    dueDayOfMonth = obj.getInt("dueDayOfMonth"),
                    linkedAccountId = obj.getLong("linkedAccountId"),
                    linkedLoanAccountId = if (obj.has("linkedLoanAccountId")) obj.getLong("linkedLoanAccountId") else null,
                    autoDeduct = obj.optBoolean("autoDeduct", true),
                    lastDeductedMonthYear = obj.optString("lastDeductedMonthYear", ""),
                    isActive = obj.optBoolean("isActive", true),
                    startDate = obj.optLong("startDate", System.currentTimeMillis()),
                    notes = obj.optString("notes", "")
                )
                dao.insertEmi(emi)
                count++
            }

            markBackupCompleted()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Intent to save JSON directly to Google Drive or chosen storage location.
     */
    fun createSaveToDriveIntent(): Intent {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "Spence_Backup_$dateFormat.json")
        }
    }

    /**
     * Intent to pick JSON file from Google Drive or device storage to restore.
     */
    fun createRestoreFromDriveIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain"))
        }
    }

    /**
     * Intent to share backup directly to Google Drive app or email.
     */
    fun createShareBackupIntent(jsonString: String): Intent {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "Spence_Backup_$dateFormat.json"
        val file = File(context.cacheDir, fileName)
        file.writeText(jsonString)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Spence Backup ($dateFormat)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
