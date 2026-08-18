package com.app.spent.ui.dashboard.components

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
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
import com.app.spent.R
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.dashboard.components.dialogs.FixedBillsDialog
import com.app.spent.ui.dashboard.components.dialogs.LoansTrackerDialog
import com.app.spent.ui.dashboard.components.dialogs.SavingsTrackerDialog
import com.app.spent.ui.theme.IncomeGreen
import com.app.spent.util.DataExportHelper
@Composable
fun DashboardQuickTools(
transactions: List<TransactionEntity>,
categories: List<CategoryEntity>,
recurringRules: List<RecurringRuleEntity> = emptyList(),
currencySymbol: String = "$",
onNavigateToSavingsTracker: () -> Unit = {},
onNavigateToFixedBills: () -> Unit = {},
onNavigateToLoansTracker: () -> Unit = {},
onNavigateToSharedLedgers: () -> Unit = {},
onAddDebtLoanTransaction: (amount: Double, type: String, note: String) -> Unit,
onAddDebtInstallmentPlan: (installmentAmount: Double, durationMonths: Int, note: String) -> Unit = { _, _, _ -> },
onAddFixedBill: (name: String, amount: Double, dueDay: Int, categoryId: String) -> Unit = { _, _, _, _ -> },
onDeleteFixedBill: (ruleId: String) -> Unit = {},
onAddSavingsTransaction: (amount: Double, note: String) -> Unit = { _, _ -> },
onUpdateSavingsGoal: (goal: Double) -> Unit = {}
) {
  val context = LocalContext.current

  var showSavingsDialog by remember { mutableStateOf(false) }

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
      .clickable { onNavigateToSavingsTracker() },
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
      .clickable { onNavigateToLoansTracker() },
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
      .clickable { onNavigateToFixedBills() },
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

    Spacer(modifier = Modifier.height(10.dp))

    // Row 3: Shared Ledgers (Finanzas Compartidas)
    Row(
    modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 20.dp)
    ) {
      Card(
      modifier = Modifier
      .fillMaxWidth()
      .clickable { onNavigateToSharedLedgers() },
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
          .size(40.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
          ) {
            Icon(
            imageVector = Icons.Default.Group,
            contentDescription = stringResource(R.string.tool_shared_ledgers_title),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
            text = stringResource(R.string.tool_shared_ledgers_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
            text = stringResource(R.string.tool_shared_ledgers_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}
