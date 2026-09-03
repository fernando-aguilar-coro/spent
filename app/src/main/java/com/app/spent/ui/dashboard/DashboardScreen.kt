package com.app.spent.ui.dashboard

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
import com.app.spent.R
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.components.WalkthroughBanner
import com.app.spent.ui.dashboard.components.CategoryEnvelopeRow
import com.app.spent.ui.dashboard.components.DashboardHeaderCard
import com.app.spent.ui.dashboard.components.DashboardProfileHeader
import com.app.spent.ui.dashboard.components.DashboardQuickActions
import com.app.spent.ui.dashboard.components.DashboardQuickTools
import com.app.spent.ui.dashboard.components.TransactionItemRow
import com.app.spent.ui.dashboard.components.dialogs.DeleteTransactionDialog
import com.app.spent.ui.dashboard.components.dialogs.TransactionDetailsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: (type: String) -> Unit = {},
    onNavigateToEditTransaction: (type: String, transactionId: String) -> Unit = { _, _ -> },
    onNavigateToSavingsTracker: () -> Unit = {},
    onNavigateToFixedBills: () -> Unit = {},
    onNavigateToLoansTracker: () -> Unit = {},
    onNavigateToSharedLedgers: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {}
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

            // Extra Tools & Shortcuts Component
            item {
                DashboardQuickTools(
                    transactions = state.allTransactions,
                    categories = state.allCategories,
                    recurringRules = state.recurringRules,
                    loans = state.loans,
                    currencySymbol = state.currencySymbol,
                    onNavigateToSavingsTracker = onNavigateToSavingsTracker,
                    onNavigateToFixedBills = onNavigateToFixedBills,
                    onNavigateToLoansTracker = onNavigateToLoansTracker,
                    onNavigateToSharedLedgers = onNavigateToSharedLedgers
                )
            }

            // Phase 4: Recent Activity Section Header with See All button
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_activity),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    androidx.compose.material3.TextButton(
                        onClick = onNavigateToHistory
                    ) {
                        Text(
                            text = "${stringResource(R.string.see_all_transactions)} >",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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

            // Walkthrough Banner at bottom if active
            if (!state.isWalkthroughCompleted) {
                item {
                    WalkthroughBanner(
                        onDismiss = { viewModel.onIntent(DashboardUiIntent.DismissWalkthrough) }
                    )
                }
            }
        }
    }

    selectedTransactionForDetails?.let { tx ->
        TransactionDetailsDialog(
            transaction = tx,
            categories = state.allCategories,
            currencySymbol = state.currencySymbol,
            onDismiss = { selectedTransactionForDetails = null },
            onRequestDelete = { transactionToDelete = tx },
            onEdit = { transactionToEdit ->
                selectedTransactionForDetails = null
                onNavigateToEditTransaction(transactionToEdit.type, transactionToEdit.id)
            }
        )
    }

    transactionToDelete?.let { tx ->
        DeleteTransactionDialog(
            transaction = tx,
            categories = state.allCategories,
            currencySymbol = state.currencySymbol,
            onDismiss = { transactionToDelete = null },
            onConfirmDelete = {
                viewModel.onIntent(DashboardUiIntent.DeleteTransaction(tx))
                transactionToDelete = null
            }
        )
    }
}
