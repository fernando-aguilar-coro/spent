package com.example.spent.ui.analytics

import androidx.lifecycle.viewModelScope
import com.example.spent.data.repository.SpentRepository
import com.example.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AnalyticsViewModel(
    private val repository: SpentRepository
) : BaseViewModel<AnalyticsUiState, AnalyticsUiIntent, AnalyticsUiEffect>(AnalyticsUiState()) {

    init {
        observeAnalytics()
    }

    private fun observeAnalytics() {
        viewModelScope.launch {
            combine(
                repository.getTransactionsFlow(),
                repository.getCategoriesFlow(),
                repository.getCurrentPayCycleFlow(),
                repository.currencySymbolFlow
            ) { transactions, categories, payCycle, currency ->

                val isPayCycleActive = payCycle != null && payCycle.frequency != "NONE"
                val totalIncome = (if (isPayCycleActive) payCycle?.income ?: 0.0 else 0.0) + transactions
                    .filter { it.type == "INCOME" }
                    .sumOf { it.amount }

                val totalSpent = transactions
                    .filter { it.type == "EXPENSE" }
                    .sumOf { it.amount }

                val netSavings = totalIncome - totalSpent
                val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toFloat().coerceIn(0f, 100f) else 0f

                val breakdowns = categories.map { cat ->
                    val spentInCat = transactions
                        .filter { it.categoryId == cat.id && it.type == "EXPENSE" }
                        .sumOf { it.amount }
                    val pct = if (totalSpent > 0) (spentInCat / totalSpent).toFloat() else 0f

                    CategorySpendingBreakdown(
                        category = cat,
                        totalSpent = spentInCat,
                        percentageOfTotal = pct
                    )
                }.sortedByDescending { it.totalSpent }

                AnalyticsUiState(
                    isLoading = false,
                    currencySymbol = currency,
                    totalIncome = totalIncome,
                    totalSpent = totalSpent,
                    netSavings = netSavings,
                    savingsRatePercentage = savingsRate,
                    categoryBreakdowns = breakdowns,
                    recentTransactions = transactions
                )
            }.collect { newState ->
                setState { newState }
            }
        }
    }

    override fun onIntent(intent: AnalyticsUiIntent) {
        when (intent) {
            is AnalyticsUiIntent.RefreshData -> observeAnalytics()
        }
    }
}
