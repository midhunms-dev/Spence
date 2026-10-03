package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.Categories
import com.example.data.TransactionType
import com.example.ui.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSaveExpense: (amount: Double, accountId: Long, category: String, note: String) -> Unit,
    onSaveIncome: (amount: Double, accountId: Long, category: String, note: String) -> Unit,
    onSaveTransfer: (amount: Double, fromAccountId: Long, toAccountId: Long, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Expense, 1: Income, 2: Transfer
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Grocery") }
    var noteText by remember { mutableStateOf("") }

    val usableAccounts = accounts.filter { it.type != AccountType.LOAN }
    val defaultAccount = usableAccounts.firstOrNull()

    var selectedAccountId by remember {
        mutableLongStateOf(defaultAccount?.id ?: 0L)
    }

    var selectedToAccountId by remember {
        mutableLongStateOf(usableAccounts.getOrNull(1)?.id ?: defaultAccount?.id ?: 0L)
    }

    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
    val amountDouble = amountText.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sheet_btn")
                ) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Type Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf("Expense", "Income", "Transfer")
                types.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            if (index == 1) {
                                selectedCategory = "Salary"
                            } else if (index == 0) {
                                selectedCategory = "Grocery"
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                        modifier = Modifier.testTag("type_tab_$index")
                    ) {
                        Text(text = label, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                    }
                },
                label = { Text("Amount (₹)") },
                prefix = { Text("₹ ", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input_field"),
                shape = RoundedCornerShape(12.dp)
            )

            // Quick amount chips
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(100, 500, 1000, 2000).forEach { chipAmt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val current = amountText.toDoubleOrNull() ?: 0.0
                                amountText = (current + chipAmt).toInt().toString()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+₹$chipAmt",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Account Selection
            if (selectedTabIndex == 2) {
                // Transfer: From and To
                Text(
                    text = "Transfer Route",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountDropdown(
                        label = "From",
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        onSelect = { selectedAccountId = it },
                        modifier = Modifier.weight(1f)
                    )
                    AccountDropdown(
                        label = "To",
                        accounts = accounts,
                        selectedAccountId = selectedToAccountId,
                        onSelect = { selectedToAccountId = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = if (selectedTabIndex == 0) "Paid From Account" else "Deposit Into Account",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                AccountDropdown(
                    label = "Account",
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onSelect = { selectedAccountId = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // If Credit Card is selected for Expense: show live Credit Limit indicator!
            if (selectedTabIndex == 0 && selectedAccount?.type == AccountType.CREDIT_CARD) {
                Spacer(modifier = Modifier.height(10.dp))
                val availableNow = selectedAccount.balance
                val newAvailable = availableNow - amountDouble
                val isOverLimit = newAvailable < 0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isOverLimit) Color(0xFFEF4444).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.12f))
                        .border(
                            width = 1.dp,
                            color = if (isOverLimit) Color(0xFFEF4444) else Color(0xFF6366F1).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOverLimit) Icons.Filled.Warning else Icons.Filled.CreditCard,
                                contentDescription = null,
                                tint = if (isOverLimit) Color(0xFFEF4444) else Color(0xFF6366F1),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Credit Card Limit Impact",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverLimit) Color(0xFFEF4444) else Color(0xFF6366F1)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current Available:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatCurrency(availableNow),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Available After Spend:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatCurrency(newAvailable.coerceAtLeast(0.0)),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverLimit) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        }
                        if (isOverLimit) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ Warning: Amount exceeds available credit limit!",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Categories Selector (for Expense and Income)
            if (selectedTabIndex != 2) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val categoryList = if (selectedTabIndex == 0) Categories.EXPENSE_CATEGORIES else Categories.INCOME_CATEGORIES

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryList.forEach { meta ->
                        val isSelected = selectedCategory == meta.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) meta.color else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedCategory = meta.name }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("cat_chip_${meta.name}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = meta.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else meta.color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = meta.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Note input
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input_field"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            val canSubmit = amountDouble > 0 && selectedAccountId != 0L
            Button(
                onClick = {
                    if (canSubmit) {
                        when (selectedTabIndex) {
                            0 -> onSaveExpense(amountDouble, selectedAccountId, selectedCategory, noteText.trim())
                            1 -> onSaveIncome(amountDouble, selectedAccountId, selectedCategory, noteText.trim())
                            2 -> onSaveTransfer(amountDouble, selectedAccountId, selectedToAccountId, noteText.trim())
                        }
                        onDismiss()
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedTabIndex) {
                        0 -> Color(0xFFEF4444) // Expense: Coral Red
                        1 -> Color(0xFF10B981) // Income: Emerald
                        else -> Color(0xFF6366F1) // Transfer: Indigo
                    }
                )
            ) {
                Text(
                    text = when (selectedTabIndex) {
                        0 -> "Save Expense"
                        1 -> "Save Income"
                        else -> "Confirm Transfer"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AccountDropdown(
    label: String,
    accounts: List<AccountEntity>,
    selectedAccountId: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentAccount = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentAccount?.name ?: "Select",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = account.name,
                                fontWeight = FontWeight.SemiBold
                            )
                            val balanceText = if (account.type == AccountType.CREDIT_CARD) {
                                "Avail Limit: ${Formatters.formatCurrency(account.balance)}"
                            } else {
                                "Bal: ${Formatters.formatCurrency(account.balance)}"
                            }
                            Text(
                                text = "${account.type.displayName} • $balanceText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
