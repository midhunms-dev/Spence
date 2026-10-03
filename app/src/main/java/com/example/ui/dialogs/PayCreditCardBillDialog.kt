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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.ui.Formatters

@Composable
fun PayCreditCardBillDialog(
    creditCards: List<AccountEntity>,
    sourceAccounts: List<AccountEntity>,
    initialCreditCardId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (creditCardAccountId: Long, sourceAccountId: Long, amount: Double) -> Unit
) {
    var selectedCcId by remember {
        mutableLongStateOf(initialCreditCardId ?: creditCards.firstOrNull()?.id ?: 0L)
    }

    var selectedSourceId by remember {
        mutableLongStateOf(sourceAccounts.firstOrNull()?.id ?: 0L)
    }

    val selectedCc = creditCards.firstOrNull { it.id == selectedCcId }
    val selectedSource = sourceAccounts.firstOrNull { it.id == selectedSourceId }

    var amountText by remember {
        mutableStateOf(selectedCc?.usedCredit?.let { if (it > 0) it.toInt().toString() else "" } ?: "")
    }

    val amountDouble = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CreditCard,
                    contentDescription = null,
                    tint = Color(0xFF6366F1)
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = "Pay Credit Card Bill",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // CC Selector if multiple cards
                if (creditCards.size > 1) {
                    Text(
                        text = "Select Credit Card",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AccountDropdown(
                        label = "Card",
                        accounts = creditCards,
                        selectedAccountId = selectedCcId,
                        onSelect = {
                            selectedCcId = it
                            val cc = creditCards.firstOrNull { c -> c.id == it }
                            if (cc != null && cc.usedCredit > 0) {
                                amountText = cc.usedCredit.toInt().toString()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Card summary banner
                if (selectedCc != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = selectedCc.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Outstanding / Used:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = Formatters.formatCurrency(selectedCc.usedCredit),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Available Limit:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = Formatters.formatCurrency(selectedCc.balance),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Payment Source (Bank or Cash)
                Text(
                    text = "Pay From Account",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                AccountDropdown(
                    label = "Source",
                    accounts = sourceAccounts,
                    selectedAccountId = selectedSourceId,
                    onSelect = { selectedSourceId = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount (₹)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cc_payment_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (selectedCc != null && selectedCc.usedCredit > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    amountText = selectedCc.usedCredit.toInt().toString()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Full Due: ${Formatters.formatCurrency(selectedCc.usedCredit)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val canPay = amountDouble > 0 && selectedCcId != 0L && selectedSourceId != 0L
            Button(
                onClick = {
                    if (canPay) {
                        onConfirm(selectedCcId, selectedSourceId, amountDouble)
                        onDismiss()
                    }
                },
                enabled = canPay,
                modifier = Modifier.testTag("confirm_pay_cc_bill_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text("Pay Bill & Restore Limit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
