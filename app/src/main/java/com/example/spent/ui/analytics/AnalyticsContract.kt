package com.example.spent.ui.analytics

import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.ui.mvi.UiEffect
import com.example.spent.ui.mvi.UiIntent
import com.example.spent.ui.mvi.UiState

data class CategorySpendingBreakdown(
    val category: CategoryEntity,
    val totalSpent: Double,
    val percentageOfTotal: Float
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val currencySymbol: String = "$",
    val totalIncome: Double = 0.0,
    val totalSpent: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRatePercentage: Float = 0f,
    val categoryBreakdowns: List<CategorySpendingBreakdown> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList()
) : UiState

sealed class AnalyticsUiIntent : UiIntent {
    object RefreshData : AnalyticsUiIntent()
}

sealed class AnalyticsUiEffect : UiEffect
