package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.BillingCycleHelper
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.Formatters
import com.example.ui.components.TransactionListItem

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val accountsMap = accounts.associateBy { it.id }
    val creditCards = accounts.filter { it.type == AccountType.CREDIT_CARD }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedAccountIdFilter by remember { mutableStateOf<Long?>(null) }

    // Billing cycle filtering states
    var isBillingCycleMode by remember { mutableStateOf(false) }
    var selectedCycleCardId by remember { mutableStateOf(creditCards.firstOrNull()?.id ?: 0L) }
    var cycleOffset by remember { mutableIntStateOf(0) } // 0 = current, -1 = last

    val selectedCard = creditCards.firstOrNull { it.id == selectedCycleCardId }
    val currentCycleRange = selectedCard?.let {
        BillingCycleHelper.getBillingCycle(it.billingDay, cycleOffset)
    }

    val categories = remember(transactions) {
        transactions.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredTransactions = transactions.filter { tx ->
        val matchesQuery = searchQuery.isBlank() ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true) ||
                (accountsMap[tx.accountId]?.name?.contains(searchQuery, ignoreCase = true) == true)

        val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
        val matchesCategory = selectedCategoryFilter == null || tx.category.equals(selectedCategoryFilter, ignoreCase = true)

        val matchesAccountAndCycle = if (isBillingCycleMode && currentCycleRange != null) {
            // Must belong to this credit card AND fall within the exact billing cycle dates!
            (tx.accountId == selectedCycleCardId || tx.toAccountId == selectedCycleCardId) &&
                    tx.timestamp >= currentCycleRange.startMillis &&
                    tx.timestamp <= currentCycleRange.endMillis
        } else {
            selectedAccountIdFilter == null || tx.accountId == selectedAccountIdFilter || tx.toAccountId == selectedAccountIdFilter
        }

        matchesQuery && matchesType && matchesCategory && matchesAccountAndCycle
    }

    val cycleTotalSpent = if (isBillingCycleMode && currentCycleRange != null) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.EMI_PAYMENT }
            .sumOf { it.amount }
    } else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen")
    ) {
        // Search and filter headers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search category, note, account...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_search_field"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Mode Selector: Standard vs Credit Card Billing Cycle
            if (creditCards.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isBillingCycleMode,
                        onClick = { isBillingCycleMode = false },
                        label = { Text("Calendar View") }
                    )

                    FilterChip(
                        selected = isBillingCycleMode,
                        onClick = {
                            isBillingCycleMode = true
                            if (selectedCycleCardId == 0L) {
                                selectedCycleCardId = creditCards.first().id
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Card Billing Cycle View") },
                        modifier = Modifier.testTag("billing_cycle_filter_chip")
                    )
                }
            }

            // Billing Cycle Banner & Cycle Selector if in cycle mode
            if (isBillingCycleMode && selectedCard != null && currentCycleRange != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF6366F1).copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedCard.name} (Bill Day: ${selectedCard.billingDay}th)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1)
                            )

                            // Cycle switch: Current vs Previous
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (cycleOffset == 0) Color(0xFF6366F1) else Color.Transparent)
                                        .clickable { cycleOffset = 0 }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Current",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cycleOffset == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (cycleOffset == -1) Color(0xFF6366F1) else Color.Transparent)
                                        .clickable { cycleOffset = -1 }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Last Cycle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cycleOffset == -1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentCycleRange.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Cycle Spent: ${Formatters.formatCurrency(cycleTotalSpent)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Standard Filter Chips Scroll row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isBillingCycleMode) {
                    FilterChip(
                        selected = selectedTypeFilter == null && selectedCategoryFilter == null && selectedAccountIdFilter == null,
                        onClick = {
                            selectedTypeFilter = null
                            selectedCategoryFilter = null
                            selectedAccountIdFilter = null
                        },
                        label = { Text("All") }
                    )

                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.EXPENSE,
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                        },
                        label = { Text("Expenses") }
                    )

                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.INCOME,
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME
                        },
                        label = { Text("Income") }
                    )

                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.EMI_PAYMENT,
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == TransactionType.EMI_PAYMENT) null else TransactionType.EMI_PAYMENT
                        },
                        label = { Text("EMIs") }
                    )

                    // Account filter chips
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountIdFilter == acc.id,
                            onClick = {
                                selectedAccountIdFilter = if (selectedAccountIdFilter == acc.id) null else acc.id
                            },
                            label = { Text(acc.name) }
                        )
                    }
                }

                // Category chips
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                        },
                        label = { Text(cat) }
                    )
                }
            }
        }

        // Transactions List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isBillingCycleMode) "No purchases in this billing cycle" else "No transactions found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your filters or switch cycles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionListItem(
                        transaction = tx,
                        accountsMap = accountsMap,
                        onDelete = { onDeleteTransaction(tx) }
                    )
                }
            }
        }
    }
}
