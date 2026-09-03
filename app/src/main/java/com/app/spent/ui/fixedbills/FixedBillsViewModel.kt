package com.app.spent.ui.fixedbills

import java.util.UUID
import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FixedBillsViewModel(
    private val repository: SpentRepository
) : BaseViewModel<FixedBillsUiState, FixedBillsUiIntent, FixedBillsUiEffect>(FixedBillsUiState()) {

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            val rulesAndTxFlow = combine(
                repository.getRecurringRulesFlow(),
                repository.getTransactionsFlow()
            ) { rules, transactions ->
                Pair(rules, transactions)
            }

            val catAndCurrencyFlow = combine(
                repository.getCategoriesFlow(),
                repository.currencySymbolFlow
            ) { categories, currency ->
                Pair(categories, currency)
            }

            combine(rulesAndTxFlow, catAndCurrencyFlow) { (rules, transactions), (categories, currency) ->
                FixedBillsUiState(
                    recurringRules = rules,
                    transactions = transactions,
                    categories = categories,
                    currencySymbol = currency,
                    isLoading = false
                )
            }.collect { newState ->
                setState {
                    newState.copy(showAddCategoryDialog = currentState.showAddCategoryDialog)
                }
            }
        }
    }

    override fun onIntent(intent: FixedBillsUiIntent) {
        when (intent) {
            is FixedBillsUiIntent.AddBill -> {
                addBill(intent.name, intent.amount, intent.dueDay, intent.categoryId, intent.arrivalTimestamp, intent.frequency)
            }
            is FixedBillsUiIntent.UpdateBill -> {
                updateBill(intent.rule)
            }
            is FixedBillsUiIntent.StopBill -> {
                stopBill(intent.ruleId)
            }
            is FixedBillsUiIntent.ResumeBill -> {
                resumeBill(intent.ruleId)
            }
            is FixedBillsUiIntent.DeleteBill -> {
                deleteBill(intent.ruleId)
            }
            is FixedBillsUiIntent.DeleteBillAndTransactions -> {
                deleteBillAndTransactions(intent.ruleId)
            }
            is FixedBillsUiIntent.PayBill -> {
                payBill(intent.amount, intent.name, intent.categoryId, intent.ruleId)
            }
            is FixedBillsUiIntent.ShowAddCategoryDialog -> {
                setState { copy(showAddCategoryDialog = intent.show) }
            }
            is FixedBillsUiIntent.CreateCategory -> {
                createCategory(intent.name, intent.colorHex, intent.iconName)
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
            setState { copy(showAddCategoryDialog = false) }
            sendEffect(FixedBillsUiEffect.ShowSnackbar("Category created: $name"))
        }
    }

    private fun addBill(name: String, amount: Double, dueDay: Int, categoryId: String, arrivalTimestamp: Long, frequency: String = "MONTHLY") {
        viewModelScope.launch {
            val rule = RecurringRuleEntity(
                id = UUID.randomUUID().toString(),
                amount = amount,
                categoryId = categoryId,
                frequency = frequency,
                startDate = arrivalTimestamp,
                note = "Bill: $name"
            )
            repository.addRecurringRule(rule)
            repository.executePendingRecurringRules()
            sendEffect(FixedBillsUiEffect.ShowSnackbar("Bill scheduled: $name"))
        }
    }

    private fun updateBill(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            repository.updateRecurringRule(rule)
            repository.executePendingRecurringRules()
            sendEffect(FixedBillsUiEffect.ShowSnackbar("Bill updated successfully"))
        }
    }

    private fun stopBill(ruleId: String) {
        viewModelScope.launch {
            repository.stopRecurringRule(ruleId)
            sendEffect(FixedBillsUiEffect.ShowSnackbar("Recurring bill stopped"))
        }
    }

    private fun resumeBill(ruleId: String) {
        viewModelScope.launch {
            val rule = currentState.recurringRules.find { it.id == ruleId }
            if (rule != null) {
                repository.updateRecurringRule(rule.copy(isActive = true))
                repository.executePendingRecurringRules()
                sendEffect(FixedBillsUiEffect.ShowSnackbar("Recurring bill resumed"))
            }
        }
    }

    private fun deleteBill(ruleId: String) {
        viewModelScope.launch {
            repository.deleteRecurringRuleById(ruleId)
            sendEffect(FixedBillsUiEffect.ShowSnackbar("Bill removed successfully"))
        }
    }

    private fun deleteBillAndTransactions(ruleId: String) {
        viewModelScope.launch {
            repository.deleteRecurringRuleAndTransactions(ruleId)
            sendEffect(FixedBillsUiEffect.ShowSnackbar("Bill and all history deleted"))
        }
    }

    private fun payBill(amount: Double, name: String, categoryId: String, ruleId: String) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                amount = amount,
                type = "EXPENSE",
                categoryId = categoryId,
                note = "Bill Payment: $name",
                timestamp = System.currentTimeMillis(),
                recurringRuleId = ruleId
            )
            repository.addTransaction(tx)
            repository.executePendingRecurringRules()
            sendEffect(FixedBillsUiEffect.ShowSnackbar("Payment recorded for $name"))
        }
    }
}
