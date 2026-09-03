package com.app.spent.ui.history

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.dashboard.RecurringDeleteMode
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState

data class TransactionHistoryUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedTypeFilter: String? = null, // null for All, "EXPENSE", "INCOME"
    val selectedCategoryId: String? = null, // null for All
    val transactions: List<TransactionEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val currencySymbol: String = "$",
    val filteredCount: Int = 0,
    val filteredTotalAmount: Double = 0.0
) : UiState

sealed class TransactionHistoryUiIntent : UiIntent {
    data class UpdateSearchQuery(val query: String) : TransactionHistoryUiIntent()
    data class SelectTypeFilter(val type: String?) : TransactionHistoryUiIntent()
    data class SelectCategoryFilter(val categoryId: String?) : TransactionHistoryUiIntent()
    data class DeleteTransaction(
        val transaction: TransactionEntity,
        val recurringDeleteMode: RecurringDeleteMode = RecurringDeleteMode.ONLY_THIS_OCCURRENCE
    ) : TransactionHistoryUiIntent()
    data class UndoDelete(val transaction: TransactionEntity) : TransactionHistoryUiIntent()
}

sealed class TransactionHistoryUiEffect : UiEffect {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : TransactionHistoryUiEffect()
    object NavigateBack : TransactionHistoryUiEffect()
}
