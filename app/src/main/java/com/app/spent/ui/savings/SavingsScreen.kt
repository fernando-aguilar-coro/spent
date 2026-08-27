package com.app.spent.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.ui.savings.components.SavingsActiveGoalHeroCard
import com.app.spent.ui.savings.components.SavingsDepositHistoryItem
import com.app.spent.ui.savings.components.SavingsDepositSection
import com.app.spent.ui.savings.components.SavingsEmptyHistoryPlaceholder
import com.app.spent.ui.savings.components.SavingsGoalFormCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    viewModel: SavingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val savingsGoalName = state.savingsGoalName
    val savingsGoalTotal = state.savingsGoalTotal
    val savingsMonthlyContribution = state.savingsMonthlyContribution
    val transactions = state.transactions
    val categories = state.categories
    val currencySymbol = state.currencySymbol

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SavingsUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    val savingsCat = categories.find { it.id == "cat_savings" || it.name.equals("Savings", ignoreCase = true) }
    val savingsTransactions = transactions.filter {
        it.type == "SAVING" || (it.categoryId == savingsCat?.id && it.type == "EXPENSE")
    }
    val totalSaved = savingsTransactions.sumOf { it.amount }

    var isEditingGoal by remember { mutableStateOf(false) }
    var isDepositing by remember { mutableStateOf(false) }
    val hasActiveGoal = savingsGoalTotal > 0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(
                        text = stringResource(R.string.savings_tracker_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_close)
                        )
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
            // Goal Configuration Form or Active Goal Card
            item {
                if (isEditingGoal || !hasActiveGoal) {
                    SavingsGoalFormCard(
                        hasActiveGoal = hasActiveGoal,
                        initialName = savingsGoalName,
                        initialTotal = savingsGoalTotal,
                        initialMonthly = savingsMonthlyContribution,
                        currencySymbol = currencySymbol,
                        onCancelEdit = { isEditingGoal = false },
                        onSaveGoal = { name, total, monthly ->
                            viewModel.onIntent(SavingsUiIntent.SetSavingsGoal(name, total, monthly))
                            isEditingGoal = false
                        }
                    )
                } else {
                    SavingsActiveGoalHeroCard(
                        goalName = savingsGoalName,
                        goalTotal = savingsGoalTotal,
                        monthlyContribution = savingsMonthlyContribution,
                        totalSaved = totalSaved,
                        currencySymbol = currencySymbol,
                        onEditGoal = { isEditingGoal = true },
                        onClearGoal = {
                            viewModel.onIntent(SavingsUiIntent.ClearSavingsGoal)
                        }
                    )
                }
            }

            // Deposit Funds Section
            item {
                SavingsDepositSection(
                    isDepositing = isDepositing,
                    currencySymbol = currencySymbol,
                    onToggleDepositing = { isDepositing = it },
                    onConfirmDeposit = { amount, note ->
                        viewModel.onIntent(SavingsUiIntent.DepositFunds(amount, note))
                        isDepositing = false
                    }
                )
            }

            // History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.savings_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (savingsTransactions.isNotEmpty()) {
                        Text(
                            text = "${savingsTransactions.size} deposits",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Deposit History List
            if (savingsTransactions.isEmpty()) {
                item {
                    SavingsEmptyHistoryPlaceholder()
                }
            } else {
                items(savingsTransactions, key = { it.id }) { tx ->
                    SavingsDepositHistoryItem(transaction = tx, currencySymbol = currencySymbol)
                }
            }
        }
    }
}
