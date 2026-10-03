package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun AddEmiDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        monthlyAmount: Double,
        totalInstallments: Int,
        paidInstallments: Int,
        dueDayOfMonth: Int,
        linkedAccountId: Long,
        linkedLoanAccountId: Long?,
        autoDeduct: Boolean,
        notes: String
    ) -> Unit
) {
    val usableAccounts = accounts.filter { it.type == AccountType.CREDIT_CARD || it.type == AccountType.BANK }
    val loanAccounts = accounts.filter { it.type == AccountType.LOAN }

    var title by remember { mutableStateOf("") }
    var monthlyAmountText by remember { mutableStateOf("") }
    var totalInstallmentsText by remember { mutableStateOf("12") }
    var paidInstallmentsText by remember { mutableStateOf("0") }
    var dueDayText by remember { mutableStateOf("5") }

    var selectedLinkedAccountId by remember {
        mutableLongStateOf(usableAccounts.firstOrNull()?.id ?: 0L)
    }

    var selectedLoanAccountId by remember {
        mutableStateOf<Long?>(null)
    }

    var autoDeduct by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    val selectedAccount = accounts.firstOrNull { it.id == selectedLinkedAccountId }
    val isCreditCard = selectedAccount?.type == AccountType.CREDIT_CARD

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add EMI / Installment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("EMI Item / Title") },
                    placeholder = { Text("e.g. iPhone 15, Washing Machine, Car EMI") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emi_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = monthlyAmountText,
                    onValueChange = { monthlyAmountText = it },
                    label = { Text("Monthly EMI Amount (₹)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emi_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalInstallmentsText,
                        onValueChange = { totalInstallmentsText = it },
                        label = { Text("Total Months") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = paidInstallmentsText,
                        onValueChange = { paidInstallmentsText = it },
                        label = { Text("Paid Months") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it },
                    label = { Text("Due Day of Month (1-31)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Linked Account Selection (Credit Card or Bank)
                Text(
                    text = "Club EMI with Account (Cut from):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                AccountDropdown(
                    label = "Deduct From",
                    accounts = usableAccounts,
                    selectedAccountId = selectedLinkedAccountId,
                    onSelect = { selectedLinkedAccountId = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Info banner about CC limit reduction
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isCreditCard) Color(0xFF6366F1).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = if (isCreditCard) Icons.Filled.CreditCard else Icons.Filled.Info,
                            contentDescription = null,
                            tint = if (isCreditCard) Color(0xFF6366F1) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCreditCard)
                                "When this EMI cuts, it will automatically deduct and reduce your '${selectedAccount?.name}' available credit limit."
                            else
                                "When this EMI cuts, it will debit directly from your '${selectedAccount?.name}' bank balance.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Optional Loan Account link
                if (loanAccounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Repaying a Loan Account? (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var loanDropdownExpanded by remember { mutableStateOf(false) }
                    val currentLoan = loanAccounts.firstOrNull { it.id == selectedLoanAccountId }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = currentLoan?.name ?: "None (Standalone EMI)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Loan") },
                            trailingIcon = {
                                TextButton(onClick = { loanDropdownExpanded = true }) {
                                    Text("Change")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { loanDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )

                        DropdownMenu(
                            expanded = loanDropdownExpanded,
                            onDismissRequest = { loanDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Standalone EMI)") },
                                onClick = {
                                    selectedLoanAccountId = null
                                    loanDropdownExpanded = false
                                }
                            )
                            loanAccounts.forEach { loan ->
                                DropdownMenuItem(
                                    text = { Text("${loan.name} (Balance: ₹${loan.balance})") },
                                    onClick = {
                                        selectedLoanAccountId = loan.id
                                        loanDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto-cut switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic EMI Cut",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Automatically deduct on due date every month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoDeduct,
                        onCheckedChange = { autoDeduct = it },
                        modifier = Modifier.testTag("auto_deduct_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            val amountDouble = monthlyAmountText.toDoubleOrNull() ?: 0.0
            val totalMonths = totalInstallmentsText.toIntOrNull() ?: 12
            val paidMonths = paidInstallmentsText.toIntOrNull() ?: 0
            val dueDay = (dueDayText.toIntOrNull() ?: 5).coerceIn(1, 31)

            val canSubmit = title.isNotBlank() && amountDouble > 0 && selectedLinkedAccountId != 0L

            Button(
                onClick = {
                    if (canSubmit) {
                        onConfirm(
                            title.trim(),
                            amountDouble,
                            totalMonths,
                            paidMonths,
                            dueDay,
                            selectedLinkedAccountId,
                            selectedLoanAccountId,
                            autoDeduct,
                            notes.trim()
                        )
                        onDismiss()
                    }
                },
                enabled = canSubmit,
                modifier = Modifier.testTag("confirm_add_emi_btn")
            ) {
                Text("Add EMI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
