package com.app.spent.ui.transaction

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState
data class AddTransactionUiState(
val selectedType: String = "EXPENSE",
val amountExpression: String = "",
val noteText: String = "",
val selectedCategoryId: String = "",
val selectedTimestamp: Long = System.currentTimeMillis(),
val isRecurring: Boolean = false,
val selectedFrequency: String = "MONTHLY",
val currencySymbol: String = "$",
val categories: List<CategoryEntity> = emptyList(),
val showAddCategoryDialog: Boolean = false,
val showKeypad: Boolean = false,
val selectedImageUri: String? = null,
val imageStorageLocation: String = "GOOGLE_DRIVE",
val isProcessingImage: Boolean = false,
val isSaving: Boolean = false
) : UiState {
  val parsedAmount: Double
  get() = amountExpression.toDoubleOrNull() ?: 0.0

  val isValid: Boolean
  get() = parsedAmount > 0
}

sealed class AddTransactionUiIntent : UiIntent {
  data class SetInitialType(val type: String) : AddTransactionUiIntent()
  data class SelectType(val type: String) : AddTransactionUiIntent()
  data class UpdateAmount(val expression: String) : AddTransactionUiIntent()
  data class UpdateNote(val note: String) : AddTransactionUiIntent()
  data class SelectCategory(val categoryId: String) : AddTransactionUiIntent()
  data class UpdateTimestamp(val timestamp: Long) : AddTransactionUiIntent()
  data class ToggleRecurring(val isRecurring: Boolean) : AddTransactionUiIntent()
  data class SelectFrequency(val frequency: String) : AddTransactionUiIntent()
  data class ToggleKeypad(val show: Boolean) : AddTransactionUiIntent()
  data class ShowAddCategoryDialog(val show: Boolean) : AddTransactionUiIntent()
  data class CreateCategory(val name: String, val colorHex: String, val iconName: String) : AddTransactionUiIntent()
  data class AttachImage(val uri: android.net.Uri) : AddTransactionUiIntent()
  object RemoveImage : AddTransactionUiIntent()
  object SaveTransaction : AddTransactionUiIntent()
}

sealed class AddTransactionUiEffect : UiEffect {
  object NavigateBack : AddTransactionUiEffect()
  data class ShowSnackbar(val message: String) : AddTransactionUiEffect()
}
