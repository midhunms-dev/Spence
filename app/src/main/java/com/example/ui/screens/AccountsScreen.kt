package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.ui.Formatters
import com.example.ui.components.AccountCard
import com.example.ui.components.CreditCardItem

@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    onAddAccountClick: () -> Unit,
    onPayCcBillClick: (Long?) -> Unit,
    onEditLimitClick: (AccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val creditCards = accounts.filter { it.type == AccountType.CREDIT_CARD }
    val bankAccounts = accounts.filter { it.type == AccountType.BANK }
    val cashAccounts = accounts.filter { it.type == AccountType.CASH }
    val loanAccounts = accounts.filter { it.type == AccountType.LOAN }

    val totalBankBalance = bankAccounts.sumOf { it.balance }
    val totalCashBalance = cashAccounts.sumOf { it.balance }
    val totalCcLimit = creditCards.sumOf { it.creditLimit }
    val totalCcAvailable = creditCards.sumOf { it.balance }
    val totalCcUsed = creditCards.sumOf { it.usedCredit }
    val totalLoanOutstanding = loanAccounts.sumOf { it.balance }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("accounts_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Action bar: Add Account
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wallets & Accounts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onAddAccountClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_account_btn")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Account", fontSize = 13.sp)
                }
            }
        }

        // 1. Credit Cards Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Credit Cards (${creditCards.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Available: ${Formatters.formatCurrency(totalCcAvailable)} / ${Formatters.formatCurrency(totalCcLimit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (creditCards.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onPayCcBillClick(creditCards.firstOrNull()?.id) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay Bill", fontSize = 12.sp)
                    }
                }
            }
        }

        if (creditCards.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No credit cards added yet. Add your card to track credit limits and linked EMIs.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(creditCards) { card ->
                CreditCardItem(
                    account = card,
                    onPayBill = { onPayCcBillClick(card.id) },
                    onEditLimit = { onEditLimitClick(card) }
                )
            }
        }

        // 2. Bank Accounts
        item {
            Column {
                Text(
                    text = "Bank Accounts (${bankAccounts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Balance: ${Formatters.formatCurrency(totalBankBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(bankAccounts) { bank ->
            AccountCard(account = bank)
        }

        // 3. Cash in Hand
        item {
            Column {
                Text(
                    text = "Cash in Hand",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Cash: ${Formatters.formatCurrency(totalCashBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(cashAccounts) { cash ->
            AccountCard(account = cash)
        }

        // 4. Loans
        if (loanAccounts.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Loans & Borrowings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Outstanding: ${Formatters.formatCurrency(totalLoanOutstanding)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(loanAccounts) { loan ->
                AccountCard(account = loan)
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
