package com.example.spent.ui.transaction

import androidx.lifecycle.viewModelScope
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.RecurringRuleEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.data.repository.SpentRepository
import com.example.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class AddTransactionViewModel(
    private val repository: SpentRepository,
    initialType: String = "EXPENSE"
) : BaseViewModel<AddTransactionUiState, AddTransactionUiIntent, AddTransactionUiEffect>(
    AddTransactionUiState(selectedType = initialType)
) {

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.getCategoriesFlow(),
                repository.currencySymbolFlow
            ) { categories, currency ->
                Pair(categories, currency)
            }.collect { (categories, currency) ->
                setState {
                    copy(
                        categories = categories,
                        currencySymbol = currency
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
                setState { copy(showKeypad = intent.show) }
            }
            is AddTransactionUiIntent.ShowAddCategoryDialog -> {
                setState { copy(showAddCategoryDialog = intent.show) }
            }
            is AddTransactionUiIntent.CreateCategory -> {
                createCategory(intent.name, intent.colorHex, intent.iconName)
            }
            is AddTransactionUiIntent.SaveTransaction -> {
                saveTransaction()
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
        if (!state.isValid || state.isSaving) return

        viewModelScope.launch {
            setState { copy(isSaving = true) }
            try {
                val targetCatId = state.selectedCategoryId.ifEmpty {
                    state.categories.find { it.id == "cat_general" }?.id
                        ?: state.categories.firstOrNull()?.id
                        ?: "cat_general"
                }

                var ruleId: String? = null
                if (state.isRecurring) {
                    ruleId = UUID.randomUUID().toString()
                    val rule = RecurringRuleEntity(
                        id = ruleId,
                        amount = state.parsedAmount,
                        categoryId = targetCatId,
                        frequency = state.selectedFrequency,
                        startDate = state.selectedTimestamp,
                        note = state.noteText
                    )
                    repository.addRecurringRule(rule)
                }

                val newTx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    amount = state.parsedAmount,
                    type = state.selectedType,
                    categoryId = targetCatId,
                    note = state.noteText,
                    timestamp = state.selectedTimestamp,
                    recurringRuleId = ruleId
                )
                repository.addTransaction(newTx)

                sendEffect(AddTransactionUiEffect.NavigateBack)
            } catch (e: Exception) {
                setState { copy(isSaving = false) }
                sendEffect(AddTransactionUiEffect.ShowSnackbar(e.message ?: "Error saving transaction"))
            }
        }
    }
}
