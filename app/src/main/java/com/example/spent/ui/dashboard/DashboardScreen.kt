package com.example.spent.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spent.R
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.ui.components.WalkthroughBanner
import com.example.spent.ui.dashboard.components.CategoryEnvelopeRow
import com.example.spent.ui.dashboard.components.DashboardHeaderCard
import com.example.spent.ui.dashboard.components.DashboardProfileHeader
import com.example.spent.ui.dashboard.components.DashboardQuickActions
import com.example.spent.ui.dashboard.components.DashboardQuickTools
import com.example.spent.ui.dashboard.components.TransactionItemRow
import com.example.spent.ui.dashboard.components.dialogs.DeleteTransactionDialog
import com.example.spent.ui.dashboard.components.dialogs.TransactionDetailsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: (type: String) -> Unit = {},
    onNavigateToSavingsTracker: () -> Unit = {},
    onNavigateToFixedBills: () -> Unit = {},
    onNavigateToLoansTracker: () -> Unit = {}
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
                    recurringRules = state.recurringRules,
                    currencySymbol = state.currencySymbol,
                    onNavigateToSavingsTracker = onNavigateToSavingsTracker,
                    onNavigateToFixedBills = onNavigateToFixedBills,
                    onNavigateToLoansTracker = onNavigateToLoansTracker,
                    onAddDebtLoanTransaction = { amount, type, note ->
                        viewModel.onIntent(DashboardUiIntent.AddTransaction(amount, type, generalCatId, note))
                    },
                    onAddDebtInstallmentPlan = { installmentAmount, durationMonths, note ->
                        viewModel.onIntent(
                            DashboardUiIntent.AddRecurringRule(
                                amount = installmentAmount,
                                categoryId = generalCatId,
                                note = note,
                                dueDay = 1,
                                durationMonths = durationMonths
                            )
                        )
                    },
                    onAddFixedBill = { name, amount, dueDay, categoryId ->
                        viewModel.onIntent(
                            DashboardUiIntent.AddRecurringRule(
                                amount = amount,
                                categoryId = categoryId,
                                note = "Bill: $name",
                                dueDay = dueDay,
                                durationMonths = null
                            )
                        )
                    },
                    onDeleteFixedBill = { ruleId ->
                        viewModel.onIntent(DashboardUiIntent.DeleteRecurringRule(ruleId))
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
                    text = stringResource(R.string.recent_activity),
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
                            text = stringResource(R.string.no_transactions_yet),
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
        TransactionDetailsDialog(
            transaction = tx,
            categories = state.allCategories,
            currencySymbol = state.currencySymbol,
            onDismiss = { selectedTransactionForDetails = null },
            onRequestDelete = { transactionToDelete = it }
        )
    }

    // Explicit Delete Confirmation Dialog to prevent accidental deletions
    transactionToDelete?.let { tx ->
        DeleteTransactionDialog(
            transaction = tx,
            categories = state.allCategories,
            currencySymbol = state.currencySymbol,
            onDismiss = { transactionToDelete = null },
            onConfirmDelete = { viewModel.onIntent(DashboardUiIntent.DeleteTransaction(it)) }
        )
    }
}
