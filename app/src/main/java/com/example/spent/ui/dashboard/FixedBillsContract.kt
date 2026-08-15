package com.example.spent.ui.dashboard

import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.RecurringRuleEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.ui.mvi.UiEffect
import com.example.spent.ui.mvi.UiIntent
import com.example.spent.ui.mvi.UiState

data class FixedBillsUiState(
    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val currencySymbol: String = "$",
    val isLoading: Boolean = false
) : UiState

sealed class FixedBillsUiIntent : UiIntent {
    data class AddBill(
        val name: String,
        val amount: Double,
        val dueDay: Int,
        val categoryId: String,
        val arrivalTimestamp: Long
    ) : FixedBillsUiIntent()

    data class DeleteBill(val ruleId: String) : FixedBillsUiIntent()

    data class PayBill(
        val amount: Double,
        val name: String,
        val categoryId: String,
        val ruleId: String
    ) : FixedBillsUiIntent()
}

sealed class FixedBillsUiEffect : UiEffect {
    data class ShowSnackbar(val message: String) : FixedBillsUiEffect()
}
