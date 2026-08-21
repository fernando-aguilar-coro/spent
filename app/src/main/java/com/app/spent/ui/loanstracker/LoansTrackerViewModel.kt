package com.app.spent.ui.loanstracker

import java.util.Calendar
import java.util.UUID
import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LoansTrackerViewModel(
    private val repository: SpentRepository
) : BaseViewModel<LoansTrackerUiState, LoansTrackerUiIntent, LoansTrackerUiEffect>(LoansTrackerUiState()) {

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.getTransactionsFlow(),
                repository.getCategoriesFlow(),
                repository.currencySymbolFlow
            ) { transactions, categories, currency ->
                LoansTrackerUiState(
                    transactions = transactions,
                    categories = categories,
                    currencySymbol = currency,
                    isLoading = false
                )
            }.collect { newState ->
                setState { newState }
            }
        }
    }

    override fun onIntent(intent: LoansTrackerUiIntent) {
        when (intent) {
            is LoansTrackerUiIntent.AddDebtLoanTransaction -> {
                addDebtLoanTransaction(intent.amount, intent.type, intent.note)
            }
            is LoansTrackerUiIntent.AddDebtInstallmentPlan -> {
                addDebtInstallmentPlan(intent.installmentAmount, intent.durationMonths, intent.note)
            }
        }
    }

    private fun getGeneralCategoryId(): String {
        val categories = currentState.categories
        return categories.find { it.id == "cat_general" }?.id
            ?: categories.firstOrNull()?.id
            ?: "cat_general"
    }

    private fun addDebtLoanTransaction(amount: Double, type: String, note: String) {
        viewModelScope.launch {
            val generalCatId = getGeneralCategoryId()
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                amount = amount,
                type = type,
                categoryId = generalCatId,
                note = note,
                timestamp = System.currentTimeMillis()
            )
            repository.addTransaction(tx)
            sendEffect(LoansTrackerUiEffect.ShowSnackbar("Loan transaction recorded"))
        }
    }

    private fun addDebtInstallmentPlan(installmentAmount: Double, durationMonths: Int, note: String) {
        viewModelScope.launch {
            val generalCatId = getGeneralCategoryId()
            val cal = Calendar.getInstance()
            val startDate = cal.timeInMillis
            cal.add(Calendar.MONTH, durationMonths)
            val endDate = cal.timeInMillis

            val rule = RecurringRuleEntity(
                id = UUID.randomUUID().toString(),
                amount = installmentAmount,
                categoryId = generalCatId,
                frequency = "MONTHLY",
                startDate = startDate,
                endDate = endDate,
                note = note
            )
            repository.addRecurringRule(rule)
            sendEffect(LoansTrackerUiEffect.ShowSnackbar("Debt installment plan scheduled ($durationMonths months)"))
        }
    }
}
