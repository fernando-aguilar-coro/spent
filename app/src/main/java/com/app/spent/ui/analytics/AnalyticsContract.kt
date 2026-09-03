package com.app.spent.ui.analytics

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState
data class CategorySpendingBreakdown(
val category: CategoryEntity,
val totalSpent: Double,
val percentageOfTotal: Float
)

enum class ChartInterval {
    DAY,
    WEEK,
    MONTH
}

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val currencySymbol: String = "$",
    val totalIncome: Double = 0.0,
    val totalSpent: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRatePercentage: Float = 0f,
    val categoryBreakdowns: List<CategorySpendingBreakdown> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val totalBalancePoints: List<com.app.spent.ui.analytics.components.TotalBalancePoint> = emptyList(),
    val netSavingsPoints: List<com.app.spent.ui.analytics.components.NetSavingsPoint> = emptyList(),
    val selectedInterval: ChartInterval = ChartInterval.DAY
) : UiState

sealed class AnalyticsUiIntent : UiIntent {
    object RefreshData : AnalyticsUiIntent()
    data class SelectInterval(val interval: ChartInterval) : AnalyticsUiIntent()
}

sealed class AnalyticsUiEffect : UiEffect
