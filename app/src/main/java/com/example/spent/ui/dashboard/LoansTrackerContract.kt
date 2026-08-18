package com.app.spent.ui.dashboard

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState
data class LoansTrackerUiState(
val transactions: List<TransactionEntity> = emptyList(),
val categories: List<CategoryEntity> = emptyList(),
val currencySymbol: String = "$",
val isLoading: Boolean = false
) : UiState

sealed class LoansTrackerUiIntent : UiIntent {
  data class AddDebtLoanTransaction(
  val amount: Double,
  val type: String,
  val note: String
  ) : LoansTrackerUiIntent()

  data class AddDebtInstallmentPlan(
  val installmentAmount: Double,
  val durationMonths: Int,
  val note: String
  ) : LoansTrackerUiIntent()
}

sealed class LoansTrackerUiEffect : UiEffect {
  data class ShowSnackbar(val message: String) : LoansTrackerUiEffect()
}
