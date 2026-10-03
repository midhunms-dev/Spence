package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        EmiEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_track_database"
                )
                    .addCallback(ExpenseDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class ExpenseDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.expenseDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: ExpenseDao) {
                // Pre-populate core default accounts
                val bankId = dao.insertAccount(
                    AccountEntity(
                        name = "Federal Bank",
                        type = AccountType.BANK,
                        balance = 38500.0,
                        accountNumberLast4 = "4512",
                        colorHex = 0xFF0D9488
                    )
                )

                val cashId = dao.insertAccount(
                    AccountEntity(
                        name = "Cash in Hand",
                        type = AccountType.CASH,
                        balance = 4200.0,
                        accountNumberLast4 = "",
                        colorHex = 0xFF10B981
                    )
                )

                val ccId = dao.insertAccount(
                    AccountEntity(
                        name = "HDFC Regalia Card",
                        type = AccountType.CREDIT_CARD,
                        // Credit Limit is 80,000, current available credit is 62,500 (17,500 spent)
                        balance = 62500.0,
                        creditLimit = 80000.0,
                        billingDay = 15,
                        accountNumberLast4 = "9823",
                        colorHex = 0xFF6366F1
                    )
                )

                val loanId = dao.insertAccount(
                    AccountEntity(
                        name = "Personal Loan",
                        type = AccountType.LOAN,
                        // Outstanding principal
                        balance = 95000.0,
                        creditLimit = 150000.0,
                        accountNumberLast4 = "3301",
                        colorHex = 0xFFF59E0B
                    )
                )

                // Pre-populate an active EMI clubbed with the Credit Card
                val emiId = dao.insertEmi(
                    EmiEntity(
                        title = "iPhone 15 (No Cost EMI)",
                        monthlyAmount = 4500.0,
                        totalInstallments = 12,
                        paidInstallments = 3,
                        dueDayOfMonth = 10,
                        linkedAccountId = ccId,
                        autoDeduct = true,
                        lastDeductedMonthYear = "2026-09",
                        notes = "Linked to HDFC Credit Card (reduces CC limit)"
                    )
                )

                // Pre-populate a Loan EMI linked to Bank Account
                dao.insertEmi(
                    EmiEntity(
                        title = "Personal Loan EMI",
                        monthlyAmount = 5500.0,
                        totalInstallments = 24,
                        paidInstallments = 10,
                        dueDayOfMonth = 5,
                        linkedAccountId = bankId,
                        linkedLoanAccountId = loanId,
                        autoDeduct = true,
                        lastDeductedMonthYear = "2026-09",
                        notes = "Auto debited from Federal Bank"
                    )
                )

                // Seed some realistic sample transactions for initial beauty
                val now = System.currentTimeMillis()
                val oneDay = 86400000L

                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = 450.0,
                        accountId = cashId,
                        category = "Vegetable",
                        note = "Fresh vegetables from local market",
                        timestamp = now - 2 * 3600000L
                    )
                )

                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = 680.0,
                        accountId = cashId,
                        category = "Fish",
                        note = "Seer fish (Neymeen)",
                        timestamp = now - 6 * 3600000L
                    )
                )

                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = 1450.0,
                        accountId = ccId,
                        category = "Grocery",
                        note = "Supermarket monthly groceries & provisions",
                        timestamp = now - oneDay
                    )
                )

                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = 850.0,
                        accountId = bankId,
                        category = "Fuel",
                        note = "Petrol refill",
                        timestamp = now - 2 * oneDay
                    )
                )

                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = 550.0,
                        accountId = ccId,
                        category = "Meat",
                        note = "Chicken & mutton",
                        timestamp = now - 3 * oneDay
                    )
                )

                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = 120.0,
                        accountId = cashId,
                        category = "Egg",
                        note = "Farm fresh eggs tray",
                        timestamp = now - 3 * oneDay - 4000000L
                    )
                )

                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = 1899.0,
                        accountId = ccId,
                        category = "Online Purchase",
                        note = "Amazon order - electronics accessories",
                        timestamp = now - 4 * oneDay
                    )
                )

                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = 600.0,
                        accountId = ccId,
                        category = "Movie",
                        note = "Cinema tickets for weekend show",
                        timestamp = now - 5 * oneDay
                    )
                )

                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = 12000.0,
                        accountId = bankId,
                        category = "Monthly Rental",
                        note = "Apartment monthly rent",
                        timestamp = now - 8 * oneDay
                    )
                )
            }
        }
    }
}
