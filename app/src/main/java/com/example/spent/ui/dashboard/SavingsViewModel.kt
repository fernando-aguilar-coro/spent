package com.example.spent.ui.dashboard

import androidx.lifecycle.viewModelScope
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.data.repository.SpentRepository
import com.example.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

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
            val categories = currentState.categories
            val savingsCatId = categories.find {
                it.id == "cat_savings" || it.name.equals("Savings", ignoreCase = true)
            }?.id ?: "cat_savings"

            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                amount = amount,
                type = "EXPENSE",
                categoryId = savingsCatId,
                note = note.ifBlank { "Savings Deposit" },
                timestamp = System.currentTimeMillis()
            )
            repository.addTransaction(tx)
            sendEffect(SavingsUiEffect.ShowSnackbar("Deposited ${currentState.currencySymbol}${"%.2f".format(amount)} to savings"))
        }
    }
}
