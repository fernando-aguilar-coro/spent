package com.example.spent.ui.dashboard

import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.ui.mvi.UiEffect
import com.example.spent.ui.mvi.UiIntent
import com.example.spent.ui.mvi.UiState

data class SavingsUiState(
    val savingsGoalName: String = "",
    val savingsGoalTotal: Double = 0.0,
    val savingsMonthlyContribution: Double = 0.0,
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val currencySymbol: String = "$",
    val isLoading: Boolean = false
) : UiState

sealed class SavingsUiIntent : UiIntent {
    data class SetSavingsGoal(
        val name: String,
        val totalGoal: Double,
        val monthlyContribution: Double
    ) : SavingsUiIntent()

    object ClearSavingsGoal : SavingsUiIntent()

    data class DepositFunds(
        val amount: Double,
        val note: String
    ) : SavingsUiIntent()
}

sealed class SavingsUiEffect : UiEffect {
    data class ShowSnackbar(val message: String) : SavingsUiEffect()
}
