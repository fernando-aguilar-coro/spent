package com.app.spent.ui.dashboard

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState

data class CategoryEnvelopeState(
    val category: CategoryEntity,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progress: Float
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val currencySymbol: String = "$",
    val totalIncome: Double = 0.0,
    val totalSpent: Double = 0.0,
    val safeToSpendToday: Double = 0.0,
    val daysRemainingInCycle: Int = 30,
    val categoriesWithProgress: List<CategoryEnvelopeState> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val currentPayCycle: PayCycleEntity? = null,
    val isWalkthroughCompleted: Boolean = true,
    val isPayCycleActive: Boolean = true,
    val activeProfileName: String = "Primary Account"
) : UiState

sealed class DashboardUiIntent : UiIntent {
    object LoadData : DashboardUiIntent()
    data class AddTransaction(
        val amount: Double,
        val type: String,
        val categoryId: String,
        val note: String
    ) : DashboardUiIntent()
    data class DeleteTransaction(val transaction: TransactionEntity) : DashboardUiIntent()
    data class UndoDelete(val transaction: TransactionEntity) : DashboardUiIntent()
    object DismissWalkthrough : DashboardUiIntent()
}

sealed class DashboardUiEffect : UiEffect {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null) : DashboardUiEffect()
    object OpenTransactionSheet : DashboardUiEffect()
}
