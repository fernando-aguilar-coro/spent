package com.app.spent.ui.history

import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionHistoryViewModel(
    private val repository: SpentRepository
) : BaseViewModel<TransactionHistoryUiState, TransactionHistoryUiIntent, TransactionHistoryUiEffect>(
    TransactionHistoryUiState()
) {

    private val searchQueryFlow = MutableStateFlow("")
    private val typeFilterFlow = MutableStateFlow<String?>(null)
    private val categoryFilterFlow = MutableStateFlow<String?>(null)

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            // Combine filter parameters to trigger reactive Room search flow
            val filterParamsFlow = combine(
                searchQueryFlow,
                typeFilterFlow,
                categoryFilterFlow
            ) { query, type, categoryId ->
                Triple(query, type, categoryId)
            }

            val searchResultFlow = filterParamsFlow.flatMapLatest { (query, type, categoryId) ->
                repository.searchTransactionsFlow(query.trim(), type, categoryId)
            }

            val coreDataFlow = combine(
                searchResultFlow,
                repository.getCategoriesFlow(),
                repository.getRecurringRulesFlow(),
                repository.currencySymbolFlow
            ) { transactions, categories, recurringRules, currency ->
                HistoryCoreBundle(transactions, categories, recurringRules, currency)
            }

            combine(coreDataFlow, filterParamsFlow) { (rawTransactions, categories, recurringRules, currency), (query, type, categoryId) ->
                val transactions = rawTransactions.filter { it.type != "SAVING" }
                val totalAmount = transactions.sumOf {
                    if (it.type == "INCOME") it.amount else -it.amount
                }
                TransactionHistoryUiState(
                    isLoading = false,
                    searchQuery = query,
                    selectedTypeFilter = type,
                    selectedCategoryId = categoryId,
                    transactions = transactions,
                    allCategories = categories,
                    recurringRules = recurringRules,
                    currencySymbol = currency,
                    filteredCount = transactions.size,
                    filteredTotalAmount = totalAmount
                )
            }.collect { newState ->
                setState { newState }
            }
        }
    }

    private data class HistoryCoreBundle(
        val transactions: List<TransactionEntity>,
        val categories: List<com.app.spent.data.local.entity.CategoryEntity>,
        val recurringRules: List<com.app.spent.data.local.entity.RecurringRuleEntity>,
        val currency: String
    )

    override fun onIntent(intent: TransactionHistoryUiIntent) {
        when (intent) {
            is TransactionHistoryUiIntent.UpdateSearchQuery -> {
                searchQueryFlow.value = intent.query
            }
            is TransactionHistoryUiIntent.SelectTypeFilter -> {
                typeFilterFlow.value = intent.type
            }
            is TransactionHistoryUiIntent.SelectCategoryFilter -> {
                categoryFilterFlow.value = intent.categoryId
            }
            is TransactionHistoryUiIntent.DeleteTransaction -> {
                deleteTransaction(intent.transaction, intent.recurringDeleteMode)
            }
            is TransactionHistoryUiIntent.UndoDelete -> {
                undoDelete(intent.transaction)
            }
        }
    }

    private fun deleteTransaction(
        transaction: TransactionEntity,
        mode: com.app.spent.ui.dashboard.RecurringDeleteMode = com.app.spent.ui.dashboard.RecurringDeleteMode.ONLY_THIS_OCCURRENCE
    ) {
        viewModelScope.launch {
            val ruleId = transaction.recurringRuleId
            when (mode) {
                com.app.spent.ui.dashboard.RecurringDeleteMode.ONLY_THIS_OCCURRENCE -> {
                    repository.deleteTransaction(transaction)
                    sendEffect(
                        TransactionHistoryUiEffect.ShowSnackbar(
                            message = "Transaction deleted",
                            actionLabel = "Undo",
                            onAction = { onIntent(TransactionHistoryUiIntent.UndoDelete(transaction)) }
                        )
                    )
                }
                com.app.spent.ui.dashboard.RecurringDeleteMode.DELETE_AND_STOP_FUTURE -> {
                    repository.deleteTransaction(transaction)
                    if (ruleId != null) {
                        repository.stopRecurringRule(ruleId)
                    }
                    sendEffect(
                        TransactionHistoryUiEffect.ShowSnackbar(
                            message = "Transaction deleted & recurring bill stopped"
                        )
                    )
                }
                com.app.spent.ui.dashboard.RecurringDeleteMode.DELETE_ALL_HISTORY -> {
                    if (ruleId != null) {
                        repository.deleteRecurringRuleAndTransactions(ruleId)
                    } else {
                        repository.deleteTransaction(transaction)
                    }
                    sendEffect(
                        TransactionHistoryUiEffect.ShowSnackbar(
                            message = "Recurring bill and all transactions deleted"
                        )
                    )
                }
            }
        }
    }

    private fun undoDelete(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
            sendEffect(TransactionHistoryUiEffect.ShowSnackbar("Transaction restored"))
        }
    }
}
