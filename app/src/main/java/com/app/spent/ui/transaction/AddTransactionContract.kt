package com.app.spent.ui.transaction

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState
import com.app.spent.util.calculator.EvaluationResult
import com.app.spent.util.calculator.MathExpressionEvaluator

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
    val inputMode: InputMode = InputMode.IDLE,
    val selectedImageUri: String? = null,
    val imageStorageLocation: String = "IN_APP",
    val isProcessingImage: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val editingTransactionId: String? = null,
    val editingRecurringRuleId: String? = null
) : UiState {
    val showKeypad: Boolean
        get() = inputMode == InputMode.CUSTOM_KEYPAD

    val evaluationResult: EvaluationResult
        get() = MathExpressionEvaluator.evaluate(amountExpression)

    val parsedAmount: Double
        get() = when (val res = evaluationResult) {
            is EvaluationResult.Success -> {
                val absVal = kotlin.math.abs(res.value)
                if (absVal > 0) absVal else 0.0
            }
            is EvaluationResult.Error -> 0.0
        }

    val computedPreviewFormatted: String?
        get() = when (val res = evaluationResult) {
            is EvaluationResult.Success -> {
                // Show preview if expression has calculations or differs from simple raw number
                if (amountExpression.any { it in listOf('+', '-', '×', '÷', '*', '/', '(', ')') }) {
                    MathExpressionEvaluator.formatResult(kotlin.math.abs(res.value))
                } else {
                    null
                }
            }
            is EvaluationResult.Error -> null
        }

    val hasCalculationError: Boolean
        get() = amountExpression.isNotBlank() && evaluationResult is EvaluationResult.Error

    val isValid: Boolean
        get() = parsedAmount > 0 && !hasCalculationError
}

sealed class AddTransactionUiIntent : UiIntent {
    data class SetInitialType(val type: String) : AddTransactionUiIntent()
    data class SelectType(val type: String) : AddTransactionUiIntent()
    data class UpdateAmount(val expression: String) : AddTransactionUiIntent()
    data class SetInputMode(val mode: InputMode) : AddTransactionUiIntent()
    data class HandleKeypadKey(val key: String, val selectionStart: Int, val selectionEnd: Int) : AddTransactionUiIntent()
    object EvaluateAmount : AddTransactionUiIntent()
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
    data class SyncCursor(val newText: String, val cursorPosition: Int) : AddTransactionUiEffect()
}
