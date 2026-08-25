package com.app.spent.ui.loanstracker

import java.util.UUID
import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.LoanEntity
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
                repository.getLoansFlow(),
                repository.getCategoriesFlow(),
                repository.currencySymbolFlow
            ) { loans, categories, currency ->
                Triple(loans, categories, currency)
            }.collect { (loans, categories, currency) ->
                setState {
                    copy(
                        loans = loans,
                        categories = categories,
                        currencySymbol = currency,
                        isLoading = false
                    )
                }
            }
        }
    }

    override fun onIntent(intent: LoansTrackerUiIntent) {
        when (intent) {
            is LoansTrackerUiIntent.SetFilter -> {
                setState { copy(selectedFilter = intent.filter) }
            }
            is LoansTrackerUiIntent.OpenAddForm -> {
                setState {
                    copy(
                        isFormOpen = true,
                        loanTypeToAdd = intent.type,
                        editingLoan = null
                    )
                }
            }
            is LoansTrackerUiIntent.OpenEditForm -> {
                setState {
                    copy(
                        isFormOpen = true,
                        loanTypeToAdd = intent.loan.type,
                        editingLoan = intent.loan
                    )
                }
            }
            is LoansTrackerUiIntent.CloseForm -> {
                setState {
                    copy(
                        isFormOpen = false,
                        editingLoan = null
                    )
                }
            }
            is LoansTrackerUiIntent.SaveLoan -> {
                saveLoan(intent)
            }
            is LoansTrackerUiIntent.OpenPaymentDialog -> {
                setState { copy(selectedLoanForPayment = intent.loan) }
            }
            is LoansTrackerUiIntent.ClosePaymentDialog -> {
                setState { copy(selectedLoanForPayment = null) }
            }
            is LoansTrackerUiIntent.RecordPayment -> {
                recordPayment(intent.loanId, intent.amount)
            }
            is LoansTrackerUiIntent.SettleLoan -> {
                settleLoan(intent.loanId)
            }
            is LoansTrackerUiIntent.DeleteLoan -> {
                deleteLoan(intent.loanId)
            }
            is LoansTrackerUiIntent.ShowAddCategoryDialog -> {
                setState { copy(showAddCategoryDialog = intent.show) }
            }
            is LoansTrackerUiIntent.CreateCategory -> {
                createCategory(intent.name, intent.colorHex, intent.iconName)
            }
        }
    }

    private fun saveLoan(intent: LoansTrackerUiIntent.SaveLoan) {
        viewModelScope.launch {
            val existing = intent.editingId?.let { id -> currentState.loans.find { it.id == id } }
            val loanToSave = LoanEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                ownerProfileId = existing?.ownerProfileId ?: "primary_account",
                type = intent.type,
                counterpartyName = intent.counterpartyName.trim(),
                principalAmount = intent.amount,
                paidAmount = existing?.paidAmount ?: 0.0,
                categoryId = intent.categoryId,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                startDate = intent.startDate,
                endDate = intent.endDate,
                calculationMode = intent.calculationMode,
                isInstallment = intent.isInstallment,
                installmentAmount = intent.installmentAmount,
                installmentDurationMonths = intent.installmentDurationMonths,
                interestRate = intent.interestRate,
                note = intent.note.trim(),
                isSettled = (existing?.paidAmount ?: 0.0) >= intent.amount
            )

            if (existing != null) {
                repository.updateLoan(loanToSave)
                sendEffect(LoansTrackerUiEffect.ShowSnackbar("Record updated successfully"))
            } else {
                repository.addLoan(loanToSave)
                sendEffect(LoansTrackerUiEffect.ShowSnackbar("Record saved successfully"))
            }

            setState {
                copy(
                    isFormOpen = false,
                    editingLoan = null
                )
            }
        }
    }

    private fun recordPayment(loanId: String, amount: Double) {
        viewModelScope.launch {
            repository.recordLoanPayment(loanId, amount)
            setState { copy(selectedLoanForPayment = null) }
            sendEffect(LoansTrackerUiEffect.ShowSnackbar("Payment recorded successfully"))
        }
    }

    private fun settleLoan(loanId: String) {
        viewModelScope.launch {
            val loan = currentState.loans.find { it.id == loanId } ?: return@launch
            repository.updateLoan(loan.copy(paidAmount = loan.principalAmount, isSettled = true))
            sendEffect(LoansTrackerUiEffect.ShowSnackbar("Record marked as settled"))
        }
    }

    private fun deleteLoan(loanId: String) {
        viewModelScope.launch {
            repository.deleteLoanById(loanId)
            sendEffect(LoansTrackerUiEffect.ShowSnackbar("Record deleted"))
        }
    }

    private fun createCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val newCategory = CategoryEntity(
                id = "cat_${UUID.randomUUID().toString().take(8)}",
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                budgetAmount = 0.0
            )
            repository.addCategory(newCategory)
            setState { copy(showAddCategoryDialog = false) }
            sendEffect(LoansTrackerUiEffect.ShowSnackbar("Category created"))
        }
    }
}
