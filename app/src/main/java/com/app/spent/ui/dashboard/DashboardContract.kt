package com.app.spent.ui.dashboard

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState

enum class RecurringDeleteMode {
    ONLY_THIS_OCCURRENCE,
    DELETE_AND_STOP_FUTURE,
    DELETE_ALL_HISTORY
}

data class CategoryEnvelopeState(
    val category: CategoryEntity,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progress: Float
)

sealed interface DashboardActivityItem {
    val id: String
    val timestamp: Long

    data class TransactionRecord(
        val transaction: TransactionEntity,
        val category: CategoryEntity? = null
    ) : DashboardActivityItem {
        override val id: String get() = transaction.id
        override val timestamp: Long get() = transaction.timestamp
    }

    data class AssetHoldingRecord(
        val asset: com.app.spent.domain.asset.model.ValuedAsset
    ) : DashboardActivityItem {
        override val id: String get() = "asset_${asset.holding.id}"
        override val timestamp: Long get() = asset.holding.createdAt
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val currencySymbol: String = "$",
    val currencyCode: String = "USD",
    val totalIncome: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalSavings: Double = 0.0,
    val safeToSpendToday: Double = 0.0,
    val daysRemainingInCycle: Int = 30,
    val pendingBillsCount: Int = 0,
    val pendingBillsTotal: Double = 0.0,
    val categoriesWithProgress: List<CategoryEnvelopeState> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val recentActivityItems: List<DashboardActivityItem> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val currentPayCycle: PayCycleEntity? = null,
    val isWalkthroughCompleted: Boolean = true,
    val isPayCycleActive: Boolean = true,
    val activeProfileName: String = "Primary Account",
    val isNetSavingsHidden: Boolean = false,
    val totalVariableAssetsValuation: Double = 0.0,
    val variableAssetsCount: Int = 0,
    val assetHoldings: List<com.app.spent.data.local.entity.AssetHoldingEntity> = emptyList()
) : UiState


sealed class DashboardUiIntent : UiIntent {
    object LoadData : DashboardUiIntent()
    object ToggleNetSavingsVisibility : DashboardUiIntent()
    data class AddTransaction(
        val amount: Double,
        val type: String,
        val categoryId: String,
        val note: String
    ) : DashboardUiIntent()
    data class DeleteTransaction(
        val transaction: TransactionEntity,
        val recurringDeleteMode: RecurringDeleteMode = RecurringDeleteMode.ONLY_THIS_OCCURRENCE
    ) : DashboardUiIntent()
    data class UndoDelete(val transaction: TransactionEntity) : DashboardUiIntent()
    object DismissWalkthrough : DashboardUiIntent()
}

sealed class DashboardUiEffect : UiEffect {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null) : DashboardUiEffect()
    object OpenTransactionSheet : DashboardUiEffect()
}
