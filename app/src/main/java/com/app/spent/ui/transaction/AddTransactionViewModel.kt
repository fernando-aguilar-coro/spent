package com.app.spent.ui.transaction

import java.util.UUID
import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.ui.mvi.BaseViewModel
import com.app.spent.util.calculator.EvaluationResult
import com.app.spent.util.calculator.MathExpressionEvaluator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    private val repository: SpentRepository,
    initialType: String = "EXPENSE",
    private val transactionId: String? = null
) : BaseViewModel<AddTransactionUiState, AddTransactionUiIntent, AddTransactionUiEffect>(
    AddTransactionUiState(
        selectedType = initialType,
        isEditing = transactionId != null,
        editingTransactionId = transactionId
    )
) {

    init {
        observeData()
        if (transactionId != null) {
            loadTransaction(transactionId)
        }
    }

    private fun loadTransaction(id: String) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(id)
            if (tx != null) {
                val formattedAmount = if (tx.amount % 1.0 == 0.0) {
                    "%.0f".format(tx.amount)
                } else {
                    "%.2f".format(tx.amount)
                }
                var isRecurring = false
                var frequency = "MONTHLY"
                var ruleId: String? = null
                if (tx.recurringRuleId != null) {
                    val rules = repository.getRecurringRulesFlow().firstOrNull() ?: emptyList()
                    val rule = rules.find { it.id == tx.recurringRuleId }
                    if (rule != null) {
                        isRecurring = true
                        frequency = rule.frequency
                        ruleId = rule.id
                    }
                }
                setState {
                    copy(
                        selectedType = tx.type,
                        amountExpression = formattedAmount,
                        selectedCategoryId = tx.categoryId,
                        noteText = tx.note,
                        selectedTimestamp = tx.timestamp,
                        selectedImageUri = tx.imageUri,
                        isRecurring = isRecurring,
                        selectedFrequency = frequency,
                        isEditing = true,
                        editingTransactionId = tx.id,
                        editingRecurringRuleId = ruleId
                    )
                }
                sendEffect(AddTransactionUiEffect.SyncCursor(formattedAmount, formattedAmount.length))
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.getCategoriesFlow(),
                repository.currencySymbolFlow,
                repository.imageStorageLocationFlow
            ) { categories, currency, imageStorage ->
                Triple(categories, currency, imageStorage)
            }.collect { (categories, currency, imageStorage) ->
                setState {
                    copy(
                        categories = categories,
                        currencySymbol = currency,
                        imageStorageLocation = imageStorage
                    )
                }
            }
        }
    }

    override fun onIntent(intent: AddTransactionUiIntent) {
        when (intent) {
            is AddTransactionUiIntent.SetInitialType -> {
                setState { copy(selectedType = intent.type) }
            }
            is AddTransactionUiIntent.SelectType -> {
                setState { copy(selectedType = intent.type) }
            }
            is AddTransactionUiIntent.UpdateAmount -> {
                setState { copy(amountExpression = intent.expression) }
            }
            is AddTransactionUiIntent.SetInputMode -> {
                setState { copy(inputMode = intent.mode) }
            }
            is AddTransactionUiIntent.HandleKeypadKey -> {
                handleKeypadKey(intent.key, intent.selectionStart, intent.selectionEnd)
            }
            is AddTransactionUiIntent.EvaluateAmount -> {
                evaluateAndCommitAmount()
            }
            is AddTransactionUiIntent.UpdateNote -> {
                setState { copy(noteText = intent.note) }
            }
            is AddTransactionUiIntent.SelectCategory -> {
                setState { copy(selectedCategoryId = intent.categoryId) }
            }
            is AddTransactionUiIntent.UpdateTimestamp -> {
                setState { copy(selectedTimestamp = intent.timestamp) }
            }
            is AddTransactionUiIntent.ToggleRecurring -> {
                setState { copy(isRecurring = intent.isRecurring) }
            }
            is AddTransactionUiIntent.SelectFrequency -> {
                setState { copy(selectedFrequency = intent.frequency) }
            }
            is AddTransactionUiIntent.ToggleKeypad -> {
                val nextMode = if (intent.show) InputMode.CUSTOM_KEYPAD else InputMode.IDLE
                setState { copy(inputMode = nextMode) }
            }
            is AddTransactionUiIntent.ShowAddCategoryDialog -> {
                setState { copy(showAddCategoryDialog = intent.show) }
            }
            is AddTransactionUiIntent.CreateCategory -> {
                createCategory(intent.name, intent.colorHex, intent.iconName)
            }
            is AddTransactionUiIntent.AttachImage -> {
                processAttachedImage(intent.uri)
            }
            is AddTransactionUiIntent.RemoveImage -> {
                setState { copy(selectedImageUri = null) }
            }
            is AddTransactionUiIntent.SaveTransaction -> {
                saveTransaction()
            }
        }
    }

    private fun handleKeypadKey(key: String, selStart: Int, selEnd: Int) {
        val current = currentState.amountExpression
        when (key) {
            "AC" -> {
                setState { copy(amountExpression = "") }
                sendEffect(AddTransactionUiEffect.SyncCursor("", 0))
            }
            "DEL" -> {
                val res = InputModeStateMachine.deleteAtCursor(current, selStart, selEnd)
                setState { copy(amountExpression = res.text) }
                sendEffect(AddTransactionUiEffect.SyncCursor(res.text, res.cursorPosition))
            }
            "=" -> {
                evaluateAndCommitAmount()
            }
            "(", ")" -> {
                val res = InputModeStateMachine.insertParenthesis(current, selStart, selEnd, isClosing = key == ")")
                setState { copy(amountExpression = res.text) }
                sendEffect(AddTransactionUiEffect.SyncCursor(res.text, res.cursorPosition))
            }
            "+", "-", "×", "÷" -> {
                val res = InputModeStateMachine.insertOperator(current, selStart, selEnd, key)
                setState { copy(amountExpression = res.text) }
                sendEffect(AddTransactionUiEffect.SyncCursor(res.text, res.cursorPosition))
            }
            "." -> {
                val res = InputModeStateMachine.insertDot(current, selStart, selEnd)
                setState { copy(amountExpression = res.text) }
                sendEffect(AddTransactionUiEffect.SyncCursor(res.text, res.cursorPosition))
            }
            else -> {
                // Digits 0-9
                val res = InputModeStateMachine.insertAtCursor(current, selStart, selEnd, key)
                setState { copy(amountExpression = res.text) }
                sendEffect(AddTransactionUiEffect.SyncCursor(res.text, res.cursorPosition))
            }
        }
    }

    private fun evaluateAndCommitAmount() {
        val current = currentState.amountExpression
        if (current.isBlank()) return

        when (val res = MathExpressionEvaluator.evaluate(current)) {
            is EvaluationResult.Success -> {
                val absFormatted = MathExpressionEvaluator.formatResult(kotlin.math.abs(res.value))
                setState { copy(amountExpression = absFormatted) }
                sendEffect(AddTransactionUiEffect.SyncCursor(absFormatted, absFormatted.length))
            }
            is EvaluationResult.Error -> {
                sendEffect(AddTransactionUiEffect.ShowSnackbar("Invalid calculation: ${res.message}"))
            }
        }
    }

    private fun processAttachedImage(uri: android.net.Uri) {
        viewModelScope.launch {
            setState { copy(isProcessingImage = true) }
            val result = repository.processAndSaveImage(
                sourceUri = uri,
                destinationType = currentState.imageStorageLocation
            )
            if (result.isSuccess) {
                val savedUri = result.getOrNull()
                setState { copy(selectedImageUri = savedUri, isProcessingImage = false) }
                val isDriveUpload = savedUri != null && !savedUri.startsWith("file:") && !savedUri.contains("receipt_images")
                val msg = if (isDriveUpload) {
                    "Image compressed and uploaded to Google Drive"
                } else {
                    "Image saved securely in app storage"
                }
                sendEffect(AddTransactionUiEffect.ShowSnackbar(msg))
            } else {
                setState { copy(isProcessingImage = false) }
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to process image"
                sendEffect(AddTransactionUiEffect.ShowSnackbar(errorMsg))
            }
        }
    }

    private fun createCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val newCat = CategoryEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                budgetAmount = 0.0,
                displayOrder = currentState.categories.size + 1
            )
            repository.addCategory(newCat)
            setState {
                copy(
                    selectedCategoryId = newCat.id,
                    showAddCategoryDialog = false
                )
            }
            sendEffect(AddTransactionUiEffect.ShowSnackbar("Category created"))
        }
    }

    private fun saveTransaction() {
        val state = currentState
        val finalAmount = state.parsedAmount
        if (finalAmount <= 0) {
            sendEffect(AddTransactionUiEffect.ShowSnackbar("Please enter a valid amount greater than 0"))
            return
        }
        if (state.isSaving) return

        viewModelScope.launch {
            setState { copy(isSaving = true) }
            try {
                // Ensure default categories exist if categories table was empty
                if (state.categories.isEmpty()) {
                    repository.seedStarterDataIfEmpty()
                }

                val targetCatId = state.selectedCategoryId.ifEmpty {
                    state.categories.find { it.id == "cat_general" }?.id
                        ?: state.categories.firstOrNull()?.id
                        ?: "cat_general"
                }

                var ruleId: String? = state.editingRecurringRuleId
                if (state.isRecurring) {
                    if (ruleId != null) {
                        val existingRule = repository.getRecurringRulesFlow().firstOrNull()?.find { it.id == ruleId }
                        val updatedRule = RecurringRuleEntity(
                            id = ruleId,
                            ownerProfileId = existingRule?.ownerProfileId ?: "primary_account",
                            amount = finalAmount,
                            categoryId = targetCatId,
                            frequency = state.selectedFrequency,
                            startDate = existingRule?.startDate ?: state.selectedTimestamp,
                            endDate = existingRule?.endDate,
                            lastExecuted = existingRule?.lastExecuted ?: state.selectedTimestamp,
                            note = state.noteText,
                            type = state.selectedType
                        )
                        repository.addRecurringRule(updatedRule)
                    } else {
                        val newRuleId = UUID.randomUUID().toString()
                        ruleId = newRuleId
                        val rule = RecurringRuleEntity(
                            id = newRuleId,
                            amount = finalAmount,
                            categoryId = targetCatId,
                            frequency = state.selectedFrequency,
                            startDate = state.selectedTimestamp,
                            lastExecuted = state.selectedTimestamp,
                            note = state.noteText,
                            type = state.selectedType
                        )
                        repository.addRecurringRule(rule)
                    }
                } else {
                    if (ruleId != null) {
                        repository.deleteRecurringRuleById(ruleId)
                        ruleId = null
                    }
                }

                val txId = if (state.isEditing && !state.editingTransactionId.isNullOrEmpty()) {
                    state.editingTransactionId
                } else {
                    UUID.randomUUID().toString()
                }

                val tx = TransactionEntity(
                    id = txId,
                    amount = finalAmount,
                    type = state.selectedType,
                    categoryId = targetCatId,
                    note = state.noteText,
                    timestamp = state.selectedTimestamp,
                    imageUri = state.selectedImageUri,
                    recurringRuleId = ruleId
                )

                if (state.isEditing) {
                    repository.updateTransaction(tx)
                } else {
                    repository.addTransaction(tx)
                }

                sendEffect(AddTransactionUiEffect.NavigateBack)
            } catch (e: Exception) {
                setState { copy(isSaving = false) }
                sendEffect(AddTransactionUiEffect.ShowSnackbar(e.message ?: "Error saving transaction"))
            }
        }
    }
}
