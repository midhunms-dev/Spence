package com.example.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AccountEntity
import com.example.data.BillingCycleHelper
import com.example.ui.Formatters

@Composable
fun EditCreditLimitDialog(
    account: AccountEntity,
    onDismiss: () -> Unit,
    onConfirm: (newLimit: Double, newBillingDay: Int) -> Unit
) {
    var limitText by remember { mutableStateOf(account.creditLimit.toInt().toString()) }
    var billingDayText by remember { mutableStateOf(account.billingDay.toString()) }

    val newLimitDouble = limitText.toDoubleOrNull() ?: 0.0
    val newBillingDay = (billingDayText.toIntOrNull() ?: 1).coerceIn(1, 31)
    val newAvailable = (newLimitDouble - account.usedCredit).coerceAtLeast(0.0)

    val previewCycle = BillingCycleHelper.getBillingCycle(newBillingDay, 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Card Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Update credit limit and billing cycle date for '${account.name}'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Total Limit (₹)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_credit_limit_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = billingDayText,
                    onValueChange = { billingDayText = it },
                    label = { Text("Billing Cycle Date (1-31)") },
                    placeholder = { Text("e.g. 20 for 20th of every month") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_billing_day_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Cycle: ${previewCycle.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Current Used: ${Formatters.formatCurrency(account.usedCredit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "New Available Limit: ${Formatters.formatCurrency(newAvailable)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newLimitDouble > 0) {
                        onConfirm(newLimitDouble, newBillingDay)
                        onDismiss()
                    }
                },
                enabled = newLimitDouble > 0,
                modifier = Modifier.testTag("confirm_set_limit_btn")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
