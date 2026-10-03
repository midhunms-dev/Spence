package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.data.Categories
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.CategorySpendItem
import com.example.ui.FinanceOverview
import com.example.ui.Formatters

@Composable
fun AnalyticsScreen(
    overview: FinanceOverview,
    categoryBreakdown: List<CategorySpendItem>,
    accounts: List<AccountEntity> = emptyList(),
    transactions: List<TransactionEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val creditCards = accounts.filter { it.type == AccountType.CREDIT_CARD }
    var isCycleMode by remember { mutableStateOf(false) }
    var selectedCycleCardId by remember { mutableStateOf(creditCards.firstOrNull()?.id ?: 0L) }
    var cycleOffset by remember { mutableIntStateOf(0) }

    val selectedCard = creditCards.firstOrNull { it.id == selectedCycleCardId }
    val cycleRange = selectedCard?.let { BillingCycleHelper.getBillingCycle(it.billingDay, cycleOffset) }

    // If cycle mode is active, recalculate category breakdown for that card and cycle
    val displayedCategories: List<CategorySpendItem> = remember(isCycleMode, selectedCycleCardId, cycleOffset, transactions, categoryBreakdown) {
        if (isCycleMode && cycleRange != null) {
            val map = mutableMapOf<String, Double>()
            var sum = 0.0
            for (tx in transactions) {
                if ((tx.accountId == selectedCycleCardId || tx.toAccountId == selectedCycleCardId) &&
                    (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.EMI_PAYMENT) &&
                    tx.timestamp >= cycleRange.startMillis &&
                    tx.timestamp <= cycleRange.endMillis
                ) {
                    val cat = tx.category.ifBlank { "Other" }
                    map[cat] = (map[cat] ?: 0.0) + tx.amount
                    sum += tx.amount
                }
            }
            map.entries.map { (cat, amt) ->
                CategorySpendItem(
                    category = cat,
                    amount = amt,
                    percentage = if (sum > 0) ((amt / sum) * 100f).toFloat() else 0f
                )
            }.sortedByDescending { it.amount }
        } else {
            categoryBreakdown
        }
    }

    val totalExpense = if (isCycleMode && cycleRange != null) {
        displayedCategories.sumOf { it.amount }
    } else {
        overview.thisMonthExpense
    }

    val totalIncome = overview.thisMonthIncome
    val netSavings = totalIncome - totalExpense

    val ccUtilization = if (overview.totalCreditLimit > 0) {
        ((overview.totalCreditUsed / overview.totalCreditLimit) * 100f).coerceIn(0.0, 100.0).toFloat()
    } else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Switcher: Calendar Month vs Card Billing Cycle
        if (creditCards.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isCycleMode,
                        onClick = { isCycleMode = false },
                        label = { Text("Calendar Month") }
                    )

                    FilterChip(
                        selected = isCycleMode,
                        onClick = {
                            isCycleMode = true
                            if (selectedCycleCardId == 0L) {
                                selectedCycleCardId = creditCards.first().id
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Card Billing Cycle") }
                    )
                }
            }

            if (isCycleMode && selectedCard != null && cycleRange != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.12f))
                            .padding(14.dp)
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

                            Text(
                                text = "Period: ${cycleRange.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Cash Flow Overview Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isCycleMode) "CARD BILLING CYCLE SPEND" else "THIS MONTH'S CASH FLOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isCycleMode) "Cycle Spent" else "Total Spent",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatCurrency(totalExpense),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }

                        if (!isCycleMode) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Income",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatCurrency(totalIncome),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Net Saved",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatCurrency(netSavings),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netSavings >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Card Limit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatCurrency(selectedCard?.creditLimit ?: 0.0),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Credit Card Utilization Gauge
        if (overview.totalCreditLimit > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CreditCard,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Credit Card Limit Utilization",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val badgeText = when {
                                ccUtilization < 30f -> "Healthy"
                                ccUtilization < 60f -> "Moderate"
                                else -> "High Risk"
                            }
                            val badgeColor = when {
                                ccUtilization < 30f -> Color(0xFF10B981)
                                ccUtilization < 60f -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }

                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Used: ${Formatters.formatCurrency(overview.totalCreditUsed)} (${ccUtilization.toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Limit: ${Formatters.formatCurrency(overview.totalCreditLimit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (ccUtilization / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when {
                                ccUtilization < 30f -> Color(0xFF10B981)
                                ccUtilization < 60f -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            },
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Category Spend Breakdown Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCycleMode) "Cycle Spending by Category" else "Monthly Spending by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (displayedCategories.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isCycleMode) "No expenses recorded in this card billing cycle." else "No category spending recorded yet this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(displayedCategories) { item ->
                val meta = Categories.getCategoryMeta(item.category)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(meta.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = meta.icon,
                                        contentDescription = null,
                                        tint = meta.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formatters.formatCurrency(item.amount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${item.percentage.toInt()}% of total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = meta.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
