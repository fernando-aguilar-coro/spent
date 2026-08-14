package com.example.spent.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.ui.components.WalkthroughBanner
import com.example.spent.ui.dashboard.components.CategoryEnvelopeRow
import com.example.spent.ui.dashboard.components.DashboardHeaderCard
import com.example.spent.ui.dashboard.components.DashboardProfileHeader
import com.example.spent.ui.dashboard.components.DashboardQuickActions
import com.example.spent.ui.dashboard.components.DashboardQuickTools
import com.example.spent.ui.dashboard.components.TransactionItemRow
import com.example.spent.ui.theme.ExpenseRed
import com.example.spent.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: (type: String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTransactionForDetails by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DashboardUiEffect.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = effect.actionLabel
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        effect.onAction?.invoke()
                    }
                }
                is DashboardUiEffect.OpenTransactionSheet -> {
                    // Handled via navigation
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Profile Header Component
            item {
                DashboardProfileHeader(
                    daysRemainingInCycle = state.daysRemainingInCycle,
                    isPayCycleActive = state.isPayCycleActive
                )
            }

            // Phase 2 Walkthrough Banner Component
            if (!state.isWalkthroughCompleted) {
                item {
                    WalkthroughBanner(
                        onDismiss = { viewModel.onIntent(DashboardUiIntent.DismissWalkthrough) }
                    )
                }
            }

            // Balance Header Card Component
            item {
                DashboardHeaderCard(
                    currencySymbol = state.currencySymbol,
                    totalIncome = state.totalIncome,
                    totalSpent = state.totalSpent,
                    safeToSpendToday = state.safeToSpendToday,
                    isPayCycleActive = state.isPayCycleActive
                )
            }

            // Hero Quick Action Buttons Component
            item {
                DashboardQuickActions(
                    onAddExpenseClick = { onNavigateToAddTransaction("EXPENSE") },
                    onAddIncomeClick = { onNavigateToAddTransaction("INCOME") }
                )
            }

            // Category Envelopes Row Component
            item {
                CategoryEnvelopeRow(
                    categoriesWithProgress = state.categoriesWithProgress,
                    currencySymbol = state.currencySymbol
                )
            }

            // Extra Tools & Shortcuts Component (Savings, Loans, Lent/Debt, Recurring Rules, Export Excel)
            item {
                val generalCatId = state.allCategories.find { it.id == "cat_general" }?.id ?: state.allCategories.firstOrNull()?.id ?: ""
                val savingsCatId = state.allCategories.find { it.id == "cat_savings" || it.name.equals("Savings", ignoreCase = true) }?.id ?: generalCatId

                DashboardQuickTools(
                    transactions = state.recentTransactions,
                    categories = state.allCategories,
                    currencySymbol = state.currencySymbol,
                    onAddDebtLoanTransaction = { amount, type, note ->
                        viewModel.onIntent(DashboardUiIntent.AddTransaction(amount, type, generalCatId, note))
                    },
                    onAddSavingsTransaction = { amount, note ->
                        viewModel.onIntent(DashboardUiIntent.AddTransaction(amount, "EXPENSE", savingsCatId, note))
                    },
                    onUpdateSavingsGoal = { goal ->
                        viewModel.onIntent(DashboardUiIntent.UpdateCategoryBudget(savingsCatId, goal))
                    }
                )
            }

            // Phase 4: Recent Activity Section Header
            item {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions yet. Tap + Add Expense to start tracking!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = state.recentTransactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionItemRow(
                        transaction = transaction,
                        categories = state.allCategories,
                        currencySymbol = state.currencySymbol,
                        onClick = { selectedTransactionForDetails = transaction }
                    )
                }
            }
        }
    }

    // Transaction Details Dialog
    selectedTransactionForDetails?.let { tx ->
        val category = state.allCategories.find { it.id == tx.categoryId }
        val isExpense = tx.type == "EXPENSE"
        val formattedDate = SimpleDateFormat("EEEE, MMMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(tx.timestamp))

        AlertDialog(
            onDismissRequest = { selectedTransactionForDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isExpense) ExpenseRed.copy(alpha = 0.15f) else IncomeGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isExpense) "-" else "+",
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) ExpenseRed else IncomeGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isExpense) "Expense Details" else "Income Details",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "${if (isExpense) "-" else "+"}${state.currencySymbol}${"%.2f".format(tx.amount)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpense) ExpenseRed else IncomeGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(category?.name ?: "General", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Note / Merchant", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(tx.note.ifEmpty { "No note provided" }, style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Date & Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formattedDate, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        val toDelete = tx
                        selectedTransactionForDetails = null
                        transactionToDelete = toDelete
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTransactionForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Explicit Delete Confirmation Dialog to prevent accidental deletions
    transactionToDelete?.let { tx ->
        val catName = state.allCategories.find { it.id == tx.categoryId }?.name ?: "General"
        val isExpense = tx.type == "EXPENSE"

        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ExpenseRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(26.dp))
                }
            },
            title = { Text("Delete Transaction?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Are you sure you want to permanently delete this transaction?")
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "${if (isExpense) "-" else "+"}${state.currencySymbol}${"%.2f".format(tx.amount)} • $catName",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (tx.note.isNotEmpty()) {
                                Text(
                                    text = tx.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will update your net balance and pay-cycle budget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onIntent(DashboardUiIntent.DeleteTransaction(tx))
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete Record", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
