package com.app.spent.ui.savings

import java.util.UUID
import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SavingsViewModel(
    private val repository: SpentRepository
) : BaseViewModel<SavingsUiState, SavingsUiIntent, SavingsUiEffect>(SavingsUiState()) {

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            val goalDataFlow = combine(
                repository.savingsGoalNameFlow,
                repository.savingsGoalTotalFlow,
                repository.savingsMonthlyContributionFlow
            ) { name, total, monthly ->
                Triple(name, total, monthly)
            }

            val listDataFlow = combine(
                repository.getTransactionsFlow(),
                repository.getCategoriesFlow(),
                repository.currencySymbolFlow
            ) { transactions, categories, currency ->
                Triple(transactions, categories, currency)
            }

            combine(goalDataFlow, listDataFlow) { (name, total, monthly), (transactions, categories, currency) ->
                SavingsUiState(
                    savingsGoalName = name,
                    savingsGoalTotal = total,
                    savingsMonthlyContribution = monthly,
                    transactions = transactions,
                    categories = categories,
                    currencySymbol = currency,
                    isLoading = false
                )
            }.collect { newState ->
                setState { newState }
            }
        }
    }

    override fun onIntent(intent: SavingsUiIntent) {
        when (intent) {
            is SavingsUiIntent.SetSavingsGoal -> {
                setSavingsGoal(intent.name, intent.totalGoal, intent.monthlyContribution)
            }
            is SavingsUiIntent.ClearSavingsGoal -> {
                clearSavingsGoal()
            }
            is SavingsUiIntent.DepositFunds -> {
                depositFunds(intent.amount, intent.note)
            }
        }
    }

    private fun setSavingsGoal(name: String, totalGoal: Double, monthlyContribution: Double) {
        viewModelScope.launch {
            repository.setSavingsGoal(name, totalGoal, monthlyContribution)

            // When establishing an automatic monthly contribution, contribute automatically up to the goal max
            if (monthlyContribution > 0.0 && totalGoal > 0.0) {
                val currentSaved = currentState.transactions.filter { it.type == "SAVING" }.sumOf { it.amount }
                val remaining = (totalGoal - currentSaved).coerceAtLeast(0.0)
                val initialContribution = minOf(monthlyContribution, remaining)

                if (initialContribution > 0.0) {
                    val tx = TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        amount = initialContribution,
                        type = "SAVING",
                        categoryId = "cat_savings",
                        note = "Monthly Savings: $name",
                        timestamp = System.currentTimeMillis()
                    )
                    repository.addTransaction(tx)
                }
            }

            sendEffect(SavingsUiEffect.ShowSnackbar("Savings goal saved: $name"))
        }
    }

    private fun clearSavingsGoal() {
        viewModelScope.launch {
            repository.clearSavingsGoal()
            sendEffect(SavingsUiEffect.ShowSnackbar("Savings goal cleared"))
        }
    }

    private fun depositFunds(amount: Double, note: String) {
        viewModelScope.launch {
            if (amount <= 0.0) return@launch
            val totalGoal = currentState.savingsGoalTotal
            val currentSaved = currentState.transactions.filter { it.type == "SAVING" }.sumOf { it.amount }

            if (totalGoal > 0.0 && currentSaved >= totalGoal) {
                sendEffect(SavingsUiEffect.ShowSnackbar("Savings goal is already completed"))
                return@launch
            }

            val remaining = if (totalGoal > 0.0) (totalGoal - currentSaved).coerceAtLeast(0.0) else amount
            val actualDeposit = if (totalGoal > 0.0) minOf(amount, remaining) else amount

            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                amount = actualDeposit,
                type = "SAVING",
                categoryId = "cat_savings",
                note = note.ifBlank { "Savings Deposit: ${currentState.savingsGoalName.ifBlank { "Goal" }}" },
                timestamp = System.currentTimeMillis()
            )
            repository.addTransaction(tx)
            sendEffect(SavingsUiEffect.ShowSnackbar("Deposited ${currentState.currencySymbol}${"%.2f".format(actualDeposit)} to savings"))
        }
    }
}
