package com.app.spent.ui.analytics

import androidx.lifecycle.viewModelScope
import com.app.spent.data.repository.SpentRepository
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
                repository.getTransactionsFlow().distinctUntilChanged(),
                repository.getCategoriesFlow().distinctUntilChanged(),
                repository.getCurrentPayCycleFlow().distinctUntilChanged(),
                repository.currencySymbolFlow.distinctUntilChanged()
            ) { transactions, categories, payCycle, currency ->
                
                val isPayCycleActive = payCycle != null && payCycle.frequency != "NONE"
                val baseIncome = if (isPayCycleActive) payCycle?.income ?: 0.0 else 0.0
                
                val manualIncome = transactions
                    .filter { it.type == "INCOME" }
                    .sumOf { it.amount }
                
                val totalIncome = baseIncome + manualIncome

                val totalSpent = transactions
                    .filter { it.type == "EXPENSE" && it.categoryId != "cat_savings" }
                    .sumOf { it.amount }

                val netSavings = totalIncome - totalSpent
                val savingsRate = if (totalIncome > 0) {
                    ((netSavings / totalIncome) * 100).toFloat().coerceIn(0f, 100f)
                } else 0f

                val breakdowns = categories.map { cat ->
                    val spentInCat = transactions
                        .filter { it.categoryId == cat.id && it.type == "EXPENSE" && it.categoryId != "cat_savings" }
                        .sumOf { it.amount }
                    val pct = if (totalSpent > 0) (spentInCat / totalSpent).toFloat() else 0f

                    CategorySpendingBreakdown(
                        category = cat,
                        totalSpent = spentInCat,
                        percentageOfTotal = pct
                    )
                }.filter { it.totalSpent > 0 }.sortedByDescending { it.totalSpent }

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
