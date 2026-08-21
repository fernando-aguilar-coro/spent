package com.app.spent.ui.loanstracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.ui.loanstracker.components.AddLoanOrPaymentFormCard
import com.app.spent.ui.loanstracker.components.LoanTransactionItemRow
import com.app.spent.ui.loanstracker.components.LoansEmptyState
import com.app.spent.ui.loanstracker.components.LoansSummaryHeroCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansTrackerScreen(
    viewModel: LoansTrackerViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val transactions = state.transactions
    val currencySymbol = state.currencySymbol

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoansTrackerUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    var isAddingNewDebt by remember { mutableStateOf(false) }

    // Filter debt transactions
    val debtTransactions = remember(transactions) {
        transactions.filter { tx ->
            tx.note.contains("Debt (", ignoreCase = true) ||
            tx.note.contains("Loan", ignoreCase = true) ||
            tx.note.contains("Préstamo", ignoreCase = true) ||
            tx.note.contains("Debt Repayment", ignoreCase = true) ||
            tx.note.contains("Debt Installment", ignoreCase = true) ||
            tx.note.contains("Pago Préstamo", ignoreCase = true)
        }
    }

    val totalLoansReceived = transactions
        .filter { it.type == "INCOME" && (it.note.contains("Loan", ignoreCase = true) || it.note.contains("Préstamo", ignoreCase = true) || it.note.contains("Debt (", ignoreCase = true)) }
        .sumOf { it.amount }
    val totalLoansPaid = transactions
        .filter { it.type == "EXPENSE" && (it.note.contains("Loan Payment", ignoreCase = true) || it.note.contains("Pago Préstamo", ignoreCase = true) || it.note.contains("Debt Repayment", ignoreCase = true) || it.note.contains("Debt Installment", ignoreCase = true)) }
        .sumOf { it.amount }
    val netLoanRemaining = (totalLoansReceived - totalLoansPaid).coerceAtLeast(0.0)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(
                        text = stringResource(R.string.loans_tracker_screen_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isAddingNewDebt) {
                        IconButton(onClick = { isAddingNewDebt = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.register_loan),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // New Debt / Loan Action Card (When Activated)
            item {
                AnimatedVisibility(visible = isAddingNewDebt) {
                    AddLoanOrPaymentFormCard(
                        currencySymbol = currencySymbol,
                        onCloseForm = { isAddingNewDebt = false },
                        onSaveNewDebt = { lenderName, amount, typeLabel, isInstallment, installmentMonthly, durationMonths ->
                            val note = "Debt ($typeLabel): $lenderName"
                            viewModel.onIntent(LoansTrackerUiIntent.AddDebtLoanTransaction(amount, "INCOME", note))

                            if (isInstallment && installmentMonthly != null && installmentMonthly > 0) {
                                viewModel.onIntent(
                                    LoansTrackerUiIntent.AddDebtInstallmentPlan(
                                        installmentAmount = installmentMonthly,
                                        durationMonths = durationMonths,
                                        note = "Debt Installment ($typeLabel): $lenderName"
                                    )
                                )
                            }
                            isAddingNewDebt = false
                        },
                        onSavePayment = { lenderName, amount ->
                            val note = "Debt Repayment: $lenderName"
                            viewModel.onIntent(LoansTrackerUiIntent.AddDebtLoanTransaction(amount, "EXPENSE", note))
                            isAddingNewDebt = false
                        }
                    )
                }
            }

            // Summary Card
            item {
                LoansSummaryHeroCard(
                    totalLoansReceived = totalLoansReceived,
                    totalLoansPaid = totalLoansPaid,
                    netLoanRemaining = netLoanRemaining,
                    currencySymbol = currencySymbol
                )
            }

            // Loan History Section Header
            item {
                Text(
                    text = stringResource(R.string.recent_activity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (debtTransactions.isEmpty()) {
                item {
                    LoansEmptyState()
                }
            } else {
                items(debtTransactions, key = { it.id }) { tx ->
                    LoanTransactionItemRow(tx = tx, currencySymbol = currencySymbol)
                }
            }
        }
    }
}
