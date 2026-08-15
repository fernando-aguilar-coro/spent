package com.example.spent.ui.dashboard.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
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
import com.example.spent.data.local.entity.RecurringRuleEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.ui.dashboard.components.dialogs.FixedBillsDialog
import com.example.spent.ui.dashboard.components.dialogs.LoansTrackerDialog
import com.example.spent.ui.dashboard.components.dialogs.SavingsTrackerDialog
import com.example.spent.ui.theme.IncomeGreen
import com.example.spent.util.DataExportHelper

@Composable
fun DashboardQuickTools(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    recurringRules: List<RecurringRuleEntity> = emptyList(),
    currencySymbol: String = "$",
    onAddDebtLoanTransaction: (amount: Double, type: String, note: String) -> Unit,
    onAddDebtInstallmentPlan: (installmentAmount: Double, durationMonths: Int, note: String) -> Unit = { _, _, _ -> },
    onAddFixedBill: (name: String, amount: Double, dueDay: Int, categoryId: String) -> Unit = { _, _, _, _ -> },
    onDeleteFixedBill: (ruleId: String) -> Unit = {},
    onAddSavingsTransaction: (amount: Double, note: String) -> Unit = { _, _ -> },
    onUpdateSavingsGoal: (goal: Double) -> Unit = {}
) {
    val context = LocalContext.current

    var showSavingsDialog by remember { mutableStateOf(false) }
    var showDebtDialog by remember { mutableStateOf(false) }
    var showFixedBillsDialog by remember { mutableStateOf(false) }

    // Savings computations
    val savingsCat = categories.find { it.id == "cat_savings" || it.name.equals("Savings", ignoreCase = true) }
    val totalSaved = transactions
        .filter { it.categoryId == savingsCat?.id && it.type == "EXPENSE" }
        .sumOf { it.amount }
    val savingsGoal = savingsCat?.budgetAmount ?: 0.0

    // Loans & Debts computations
    val totalLoansReceived = transactions
        .filter { it.type == "INCOME" && (it.note.contains("Loan", ignoreCase = true) || it.note.contains("Préstamo", ignoreCase = true) || it.note.contains("Debt (", ignoreCase = true)) }
        .sumOf { it.amount }
    val totalLoansPaid = transactions
        .filter { it.type == "EXPENSE" && (it.note.contains("Loan Payment", ignoreCase = true) || it.note.contains("Pago Préstamo", ignoreCase = true) || it.note.contains("Debt Repayment", ignoreCase = true) || it.note.contains("Debt Installment", ignoreCase = true)) }
        .sumOf { it.amount }
    val netLoanRemaining = (totalLoansReceived - totalLoansPaid).coerceAtLeast(0.0)

    // Active bills count
    val activeBillsCount = recurringRules.count { it.endDate == null || it.endDate >= System.currentTimeMillis() }

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.extra_tools_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Row 1: Savings Tracker & Loans/Debts
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

            // 2. Loans & Debts Tool (Direct, Card, Bank, Interest)
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

        // Row 2: Fixed Bills & Utilities and Export to Excel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3. Fixed Bills & Utilities Tool
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showFixedBillsDialog = true },
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
                            imageVector = Icons.Default.Bolt,
                            contentDescription = stringResource(R.string.tool_fixed_bills_title),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.tool_fixed_bills_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (activeBillsCount > 0) "$activeBillsCount active bills" else stringResource(R.string.tool_fixed_bills_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // 4. Export to Excel (.xlsx) Tool
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { DataExportHelper.exportTransactionsToExcel(context, transactions, categories) },
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
                            imageVector = Icons.Default.Description,
                            contentDescription = "Excel Export",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.tool_export_excel_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.tool_export_excel_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
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

    // 2. Enhanced Debt & Loans Dialog
    if (showDebtDialog) {
        LoansTrackerDialog(
            netLoanRemaining = netLoanRemaining,
            totalLoansReceived = totalLoansReceived,
            totalLoansPaid = totalLoansPaid,
            currencySymbol = currencySymbol,
            onDismiss = { showDebtDialog = false },
            onAddDebtLoanTransaction = onAddDebtLoanTransaction,
            onAddDebtInstallmentPlan = onAddDebtInstallmentPlan
        )
    }

    // 3. Fixed Bills & Utilities Dialog
    if (showFixedBillsDialog) {
        FixedBillsDialog(
            recurringRules = recurringRules,
            transactions = transactions,
            categories = categories,
            currencySymbol = currencySymbol,
            onDismiss = { showFixedBillsDialog = false },
            onAddBill = { name, amount, dueDay, categoryId ->
                onAddFixedBill(name, amount, dueDay, categoryId)
            },
            onDeleteBill = { ruleId ->
                onDeleteFixedBill(ruleId)
            },
            onPayBill = { amount, name, categoryId ->
                onAddDebtLoanTransaction(amount, "EXPENSE", "Bill Payment: $name")
            }
        )
    }
}
