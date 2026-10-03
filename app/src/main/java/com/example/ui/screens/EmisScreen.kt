package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.EmiEntity
import com.example.ui.Formatters
import com.example.ui.components.EmiCard

@Composable
fun EmisScreen(
    emis: List<EmiEntity>,
    accounts: List<AccountEntity>,
    onAddEmiClick: () -> Unit,
    onPayEmiClick: (Long) -> Unit,
    onProcessAllDueEmisClick: () -> Unit,
    onDeleteEmiClick: (EmiEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val accountsMap = accounts.associateBy { it.id }

    val activeEmis = emis.filter { !it.isCompleted }
    val completedEmis = emis.filter { it.isCompleted }

    val totalMonthlyEmi = activeEmis.sumOf { it.monthlyAmount }
    val totalRemainingEmiDebt = activeEmis.sumOf { it.remainingAmount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("emis_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF312E81), // Deep Indigo
                                    Color(0xFF4338CA),
                                    Color(0xFF6366F1)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL MONTHLY EMI",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA5B4FC),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Button(
                                onClick = onAddEmiClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("add_emi_hero_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF312E81),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add EMI",
                                    color = Color(0xFF312E81),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${Formatters.formatCurrency(totalMonthlyEmi)} / mo",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Active EMIs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA5B4FC)
                                )
                                Text(
                                    text = "${activeEmis.size} active",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column {
                                Text(
                                    text = "Remaining Outstanding",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA5B4FC)
                                )
                                Text(
                                    text = Formatters.formatCurrency(totalRemainingEmiDebt),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDE68A)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Info explanation banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EMIs clubbed with Credit Cards automatically deduct from and lower your available credit limit each month. EMIs clubbed with Banks auto-debit your bank balance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Trigger Auto-Cut Button
        item {
            OutlinedButton(
                onClick = onProcessAllDueEmisClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("trigger_auto_cut_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Check & Auto-Cut All Due EMIs Now", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        // Active EMIs
        item {
            Text(
                text = "Active Installments (${activeEmis.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (activeEmis.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No active EMIs. Tap 'Add EMI' to club an EMI with your Credit Card or Bank.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(activeEmis, key = { it.id }) { emi ->
                EmiCard(
                    emi = emi,
                    linkedAccount = accountsMap[emi.linkedAccountId],
                    linkedLoanAccount = emi.linkedLoanAccountId?.let { accountsMap[it] },
                    onPayEmi = { onPayEmiClick(emi.id) },
                    onDeleteEmi = { onDeleteEmiClick(emi) }
                )
            }
        }

        // Completed EMIs
        if (completedEmis.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Completed EMIs (${completedEmis.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(completedEmis, key = { it.id }) { emi ->
                EmiCard(
                    emi = emi,
                    linkedAccount = accountsMap[emi.linkedAccountId],
                    linkedLoanAccount = emi.linkedLoanAccountId?.let { accountsMap[it] },
                    onPayEmi = {},
                    onDeleteEmi = { onDeleteEmiClick(emi) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
