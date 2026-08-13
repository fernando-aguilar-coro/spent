package com.example.spent.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spent.ui.components.WalkthroughBanner
import com.example.spent.ui.dashboard.components.CategoryEnvelopeRow
import com.example.spent.ui.dashboard.components.DashboardHeaderCard
import com.example.spent.ui.dashboard.components.DashboardProfileHeader
import com.example.spent.ui.dashboard.components.DashboardQuickActions
import com.example.spent.ui.dashboard.components.TransactionItemRow
import com.example.spent.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: (type: String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    onNavigateToAddTransaction("EXPENSE")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    activeProfileName = state.activeProfileName,
                    daysRemainingInCycle = state.daysRemainingInCycle
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
                    safeToSpendToday = state.safeToSpendToday
                )
            }

            // Hero Quick Action Buttons Component -> Navigates to dedicated AddTransactionScreen
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
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                viewModel.onIntent(DashboardUiIntent.DeleteTransaction(transaction))
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ExpenseRed)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White
                                )
                            }
                        }
                    ) {
                        TransactionItemRow(
                            transaction = transaction,
                            categories = state.allCategories,
                            currencySymbol = state.currencySymbol
                        )
                    }
                }
            }
        }
    }
}
