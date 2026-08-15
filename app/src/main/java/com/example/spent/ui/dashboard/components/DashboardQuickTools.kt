package com.example.spent.ui.dashboard.components

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.spent.R
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.ui.dashboard.components.dialogs.LentDebtDialog
import com.example.spent.ui.dashboard.components.dialogs.LoansTrackerDialog
import com.example.spent.ui.dashboard.components.dialogs.SavingsTrackerDialog
import com.example.spent.ui.theme.IncomeGreen
import com.example.spent.util.DataExportHelper

@Composable
fun DashboardQuickTools(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "$",
    onAddDebtLoanTransaction: (amount: Double, type: String, note: String) -> Unit,
    onAddSavingsTransaction: (amount: Double, note: String) -> Unit = { _, _ -> },
    onUpdateSavingsGoal: (goal: Double) -> Unit = {}
) {
    val context = LocalContext.current

    var showSavingsDialog by remember { mutableStateOf(false) }
    var showLoansDialog by remember { mutableStateOf(false) }
    var showDebtDialog by remember { mutableStateOf(false) }

    // Savings computations
    val savingsCat = categories.find { it.id == "cat_savings" || it.name.equals("Savings", ignoreCase = true) }
    val totalSaved = transactions
        .filter { it.categoryId == savingsCat?.id && it.type == "EXPENSE" }
        .sumOf { it.amount }
    val savingsGoal = savingsCat?.budgetAmount ?: 0.0

    // Loans computations
    val totalLoansReceived = transactions
        .filter { it.type == "INCOME" && (it.note.contains("Loan", ignoreCase = true) || it.note.contains("Préstamo", ignoreCase = true)) }
        .sumOf { it.amount }
    val totalLoansPaid = transactions
        .filter { it.type == "EXPENSE" && (it.note.contains("Loan Payment", ignoreCase = true) || it.note.contains("Pago Préstamo", ignoreCase = true)) }
        .sumOf { it.amount }
    val netLoanRemaining = (totalLoansReceived - totalLoansPaid).coerceAtLeast(0.0)

    val recurringToastTemplate = stringResource(R.string.tool_recurring_toast)

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.extra_tools_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Row 1: Savings Tracker & Loans (Préstamos)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Savings Tracker Tool
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showSavingsDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IncomeGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = stringResource(R.string.tool_savings_title),
                            tint = IncomeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.tool_savings_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (savingsGoal > 0) stringResource(R.string.tool_savings_goal, currencySymbol, savingsGoal) else stringResource(R.string.tool_savings_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // 2. Loans (Préstamos) Tool
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showLoansDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = stringResource(R.string.tool_loans_title),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.tool_loans_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (netLoanRemaining > 0) stringResource(R.string.tool_loans_debt, currencySymbol, netLoanRemaining) else stringResource(R.string.tool_loans_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Lent / Debt (IOU) & Recurring Rules
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3. Lent / Debt (IOU) Tool
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showDebtDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = stringResource(R.string.tool_lent_debt_title),
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.tool_lent_debt_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.tool_lent_debt_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // 4. Recurring Rules Tool
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val activeCount = transactions.count { it.recurringRuleId != null }
                        Toast.makeText(
                            context,
                            String.format(recurringToastTemplate, activeCount),
                            Toast.LENGTH_LONG
                        ).show()
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = stringResource(R.string.tool_recurring_title),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.tool_recurring_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.tool_recurring_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 3: Export to Excel (.xlsx) Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { DataExportHelper.exportTransactionsToExcel(context, transactions, categories) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Excel Export",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.tool_export_excel_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.tool_export_excel_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 1. Savings Tracker Dialog
    if (showSavingsDialog) {
        SavingsTrackerDialog(
            totalSaved = totalSaved,
            savingsGoal = savingsGoal,
            currencySymbol = currencySymbol,
            onDismiss = { showSavingsDialog = false },
            onDeposit = { amount, note ->
                onAddSavingsTransaction(amount, note)
            },
            onUpdateGoal = { goal ->
                onUpdateSavingsGoal(goal)
            }
        )
    }

    // 2. Loans (Préstamos) Dialog
    if (showLoansDialog) {
        LoansTrackerDialog(
            netLoanRemaining = netLoanRemaining,
            totalLoansReceived = totalLoansReceived,
            totalLoansPaid = totalLoansPaid,
            currencySymbol = currencySymbol,
            onDismiss = { showLoansDialog = false },
            onAddDebtLoanTransaction = onAddDebtLoanTransaction
        )
    }

    // 3. Lent / Debt Entry Dialog (IOU)
    if (showDebtDialog) {
        LentDebtDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showDebtDialog = false },
            onAddDebtLoanTransaction = onAddDebtLoanTransaction
        )
    }
}
