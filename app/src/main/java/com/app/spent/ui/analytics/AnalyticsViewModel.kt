package com.app.spent.ui.analytics

import androidx.lifecycle.viewModelScope
import com.app.spent.data.repository.SpentRepository
import com.app.spent.ui.analytics.components.ChartTimelineHelper
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                withContext(Dispatchers.Default) {
                    val isPayCycleActive = payCycle != null && payCycle.frequency != "NONE"
                    val baseIncome = if (isPayCycleActive) payCycle?.income ?: 0.0 else 0.0

                    var manualIncome = 0.0
                    var totalSpent = 0.0
                    val expensesByCategory = mutableMapOf<String, Double>()

                    for (tx in transactions) {
                        if (tx.type == "INCOME") {
                            manualIncome += tx.amount
                        } else if (tx.type == "EXPENSE") {
                            totalSpent += tx.amount
                            expensesByCategory[tx.categoryId] = (expensesByCategory[tx.categoryId] ?: 0.0) + tx.amount
                        }
                    }

                    val totalIncome = baseIncome + manualIncome
                    val netSavings = totalIncome - totalSpent
                    val savingsRate = if (totalIncome > 0) {
                        ((netSavings / totalIncome) * 100).toFloat().coerceIn(0f, 100f)
                    } else 0f

                    val breakdowns = categories.mapNotNull { cat ->
                        val spentInCat = expensesByCategory[cat.id] ?: 0.0
                        if (spentInCat > 0) {
                            val pct = if (totalSpent > 0) (spentInCat / totalSpent).toFloat() else 0f
                            CategorySpendingBreakdown(
                                category = cat,
                                totalSpent = spentInCat,
                                percentageOfTotal = pct
                            )
                        } else null
                    }.sortedByDescending { it.totalSpent }

                    val interval = currentState.selectedInterval
                    val nonSavingTransactions = transactions.filter { it.type != "SAVING" }
                    val balancePoints = ChartTimelineHelper.computeTotalBalancePoints(nonSavingTransactions, interval)
                    val netSavingsPoints = ChartTimelineHelper.computeNetSavingsPoints(nonSavingTransactions, interval)

                    AnalyticsUiState(
                        isLoading = false,
                        currencySymbol = currency,
                        totalIncome = totalIncome,
                        totalSpent = totalSpent,
                        netSavings = netSavings,
                        savingsRatePercentage = savingsRate,
                        categoryBreakdowns = breakdowns,
                        recentTransactions = nonSavingTransactions,
                        totalBalancePoints = balancePoints,
                        netSavingsPoints = netSavingsPoints,
                        selectedInterval = interval
                    )
                }
            }.collect { newState ->
                setState { newState }
            }
        }
    }

    override fun onIntent(intent: AnalyticsUiIntent) {
        when (intent) {
            is AnalyticsUiIntent.RefreshData -> observeAnalytics()
            is AnalyticsUiIntent.SelectInterval -> {
                setState { copy(selectedInterval = intent.interval) }
                viewModelScope.launch(Dispatchers.Default) {
                    val nonSavingTransactions = currentState.recentTransactions
                    val balancePoints = ChartTimelineHelper.computeTotalBalancePoints(nonSavingTransactions, intent.interval)
                    val netSavingsPoints = ChartTimelineHelper.computeNetSavingsPoints(nonSavingTransactions, intent.interval)
                    setState {
                        copy(
                            selectedInterval = intent.interval,
                            totalBalancePoints = balancePoints,
                            netSavingsPoints = netSavingsPoints
                        )
                    }
                }
            }
        }
    }
}
