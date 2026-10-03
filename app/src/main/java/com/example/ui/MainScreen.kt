package com.example.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.ui.dialogs.AddAccountDialog
import com.example.ui.dialogs.AddEmiDialog
import com.example.ui.dialogs.AddTransactionSheet
import com.example.ui.dialogs.EditCreditLimitDialog
import com.example.ui.dialogs.PayCreditCardBillDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmisScreen
import com.example.ui.screens.TransactionsScreen
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainTab(val title: String) {
    OVERVIEW("Overview"),
    TRANSACTIONS("Transactions"),
    ACCOUNTS("Accounts & Cards"),
    EMIS("EMIs & Loans"),
    ANALYTICS("Reports")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ExpenseViewModel,
    initialTab: MainTab = MainTab.OVERVIEW,
    initialOpenAddTransaction: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(initialTab) }

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val emis by viewModel.emis.collectAsStateWithLifecycle()
    val overview by viewModel.financeOverview.collectAsStateWithLifecycle()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsStateWithLifecycle()
    val notificationMessage by viewModel.notificationMessage.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val currentBackupFrequency by viewModel.backupFrequency.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notificationMessage) {
        notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    // Dialog & Sheet states
    var showAddTransactionSheet by remember { mutableStateOf(initialOpenAddTransaction) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddEmiDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var payCcBillCardId by remember { mutableStateOf<Long?>(null) }
    var showPayCcBillDialog by remember { mutableStateOf(false) }
    var editingCcAccount by remember { mutableStateOf<AccountEntity?>(null) }

    // Launcher for Saving Backup directly to Google Drive / Documents
    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = viewModel.getBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { writer ->
                        writer.write(json)
                    }
                }
                viewModel.backupManager.markBackupCompleted()
                viewModel.clearNotification()
                // notify user
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    // Launcher for Restoring Backup from Google Drive / Files
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val sb = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            sb.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                viewModel.restoreBackupFromJson(sb.toString()) { success, _ -> }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_spence_logo),
                            contentDescription = "Spence Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Spence",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddAccountDialog = true },
                        modifier = Modifier.testTag("top_add_account_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = "Add Account"
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("top_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.OVERVIEW,
                    onClick = { selectedTab = MainTab.OVERVIEW },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Overview") },
                    label = { Text("Overview") },
                    modifier = Modifier.testTag("nav_overview")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.TRANSACTIONS,
                    onClick = { selectedTab = MainTab.TRANSACTIONS },
                    icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = "Transactions") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_transactions")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.ACCOUNTS,
                    onClick = { selectedTab = MainTab.ACCOUNTS },
                    icon = { Icon(Icons.Filled.CreditCard, contentDescription = "Accounts & Cards") },
                    label = { Text("Cards") },
                    modifier = Modifier.testTag("nav_accounts")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.EMIS,
                    onClick = { selectedTab = MainTab.EMIS },
                    icon = { Icon(Icons.Filled.Payments, contentDescription = "EMIs") },
                    label = { Text("EMIs") },
                    modifier = Modifier.testTag("nav_emis")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.ANALYTICS,
                    onClick = { selectedTab = MainTab.ANALYTICS },
                    icon = { Icon(Icons.Filled.PieChart, contentDescription = "Reports") },
                    label = { Text("Reports") },
                    modifier = Modifier.testTag("nav_analytics")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTransactionSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Transaction",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.OVERVIEW -> {
                    DashboardScreen(
                        overview = overview,
                        accounts = accounts,
                        recentTransactions = recentTransactions,
                        onAddTransactionClick = { showAddTransactionSheet = true },
                        onPayCcBillClick = { cardId ->
                            payCcBillCardId = cardId
                            showPayCcBillDialog = true
                        },
                        onEditCcLimitClick = { card -> editingCcAccount = card },
                        onProcessDueEmisClick = { viewModel.checkAndTriggerAutoEmiDeductions() },
                        onViewAllTransactionsClick = { selectedTab = MainTab.TRANSACTIONS },
                        onViewAllAccountsClick = { selectedTab = MainTab.ACCOUNTS },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                    )
                }

                MainTab.TRANSACTIONS -> {
                    TransactionsScreen(
                        transactions = transactions,
                        accounts = accounts,
                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                    )
                }

                MainTab.ACCOUNTS -> {
                    AccountsScreen(
                        accounts = accounts,
                        onAddAccountClick = { showAddAccountDialog = true },
                        onPayCcBillClick = { cardId ->
                            payCcBillCardId = cardId
                            showPayCcBillDialog = true
                        },
                        onEditLimitClick = { card -> editingCcAccount = card }
                    )
                }

                MainTab.EMIS -> {
                    EmisScreen(
                        emis = emis,
                        accounts = accounts,
                        onAddEmiClick = { showAddEmiDialog = true },
                        onPayEmiClick = { emiId -> viewModel.processEmiPaymentManually(emiId) },
                        onProcessAllDueEmisClick = { viewModel.checkAndTriggerAutoEmiDeductions() },
                        onDeleteEmiClick = { viewModel.deleteEmi(it) }
                    )
                }

                MainTab.ANALYTICS -> {
                    AnalyticsScreen(
                        overview = overview,
                        categoryBreakdown = categoryBreakdown,
                        accounts = accounts,
                        transactions = transactions
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet: Add Transaction
    if (showAddTransactionSheet) {
        AddTransactionSheet(
            accounts = accounts,
            onDismiss = { showAddTransactionSheet = false },
            onSaveExpense = { amount, accountId, category, note ->
                viewModel.addExpense(amount, accountId, category, note)
            },
            onSaveIncome = { amount, accountId, category, note ->
                viewModel.addIncome(amount, accountId, category, note)
            },
            onSaveTransfer = { amount, fromAccountId, toAccountId, note ->
                viewModel.addTransfer(amount, fromAccountId, toAccountId, note)
            }
        )
    }

    // Dialog: Add Account
    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, type, balance, creditLimit, billingDay, last4 ->
                viewModel.addAccount(name, type, balance, creditLimit, billingDay, last4)
            }
        )
    }

    // Dialog: Pay Credit Card Bill
    if (showPayCcBillDialog) {
        val creditCards = accounts.filter { it.type == AccountType.CREDIT_CARD }
        val sources = accounts.filter { it.type == AccountType.BANK || it.type == AccountType.CASH }

        if (creditCards.isNotEmpty() && sources.isNotEmpty()) {
            PayCreditCardBillDialog(
                creditCards = creditCards,
                sourceAccounts = sources,
                initialCreditCardId = payCcBillCardId,
                onDismiss = {
                    showPayCcBillDialog = false
                    payCcBillCardId = null
                },
                onConfirm = { ccId, sourceId, amount ->
                    viewModel.payCreditCardBill(ccId, sourceId, amount)
                }
            )
        }
    }

    // Dialog: Add EMI
    if (showAddEmiDialog) {
        AddEmiDialog(
            accounts = accounts,
            onDismiss = { showAddEmiDialog = false },
            onConfirm = { title, monthlyAmount, totalInstallments, paidInstallments, dueDay, linkedAccountId, linkedLoanId, autoDeduct, notes ->
                viewModel.addEmi(
                    title,
                    monthlyAmount,
                    totalInstallments,
                    paidInstallments,
                    dueDay,
                    linkedAccountId,
                    linkedLoanId,
                    autoDeduct,
                    notes
                )
            }
        )
    }

    // Dialog: Edit CC Limit & Billing Date
    editingCcAccount?.let { ccAccount ->
        EditCreditLimitDialog(
            account = ccAccount,
            onDismiss = { editingCcAccount = null },
            onConfirm = { newLimit, newBillingDay ->
                viewModel.updateCreditCardDetails(ccAccount.id, newLimit, newBillingDay)
                editingCcAccount = null
            }
        )
    }

    // Dialog: Settings (Themes, Google Drive Backup, EMI Reminders)
    if (showSettingsDialog) {
        SettingsDialog(
            currentThemeMode = currentThemeMode,
            onSelectThemeMode = { viewModel.setThemeMode(it) },
            currentBackupFrequency = currentBackupFrequency,
            onSelectBackupFrequency = { viewModel.setBackupFrequency(it) },
            lastBackupTimestamp = viewModel.backupManager.getLastBackupTimestamp(),
            onSaveToDrive = {
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                saveBackupLauncher.launch("ExpenseTrack_Backup_$dateStr.json")
            },
            onShareToDrive = {
                val json = viewModel.getBackupJson()
                val shareIntent = viewModel.backupManager.createShareBackupIntent(json)
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Save to Google Drive / Share"))
            },
            onRestoreBackup = {
                restoreBackupLauncher.launch(arrayOf("application/json", "text/*"))
            },
            onTestEmiReminder = {
                viewModel.checkEmiReminders()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}
