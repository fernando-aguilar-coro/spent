package com.app.spent.ui.loanstracker

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState

enum class LoanFilter {
    ALL,
    I_OWE,
    OWED_TO_ME
}

data class LoansTrackerUiState(
    val loans: List<LoanEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val currencySymbol: String = "$",
    val selectedFilter: LoanFilter = LoanFilter.ALL,
    val isFormOpen: Boolean = false,
    val loanTypeToAdd: String = "I_OWE", // "I_OWE" or "OWED_TO_ME"
    val editingLoan: LoanEntity? = null,
    val selectedLoanForPayment: LoanEntity? = null,
    val showAddCategoryDialog: Boolean = false,
    val isLoading: Boolean = false
) : UiState {
    val totalIOwe: Double
        get() = loans.filter { it.type == "I_OWE" && !it.isSettled }.sumOf { it.remainingAmount }

    val totalOwedToMe: Double
        get() = loans.filter { it.type == "OWED_TO_ME" && !it.isSettled }.sumOf { it.remainingAmount }

    val netBalance: Double
        get() = totalOwedToMe - totalIOwe

    val filteredLoans: List<LoanEntity>
        get() = when (selectedFilter) {
            LoanFilter.ALL -> loans
            LoanFilter.I_OWE -> loans.filter { it.type == "I_OWE" }
            LoanFilter.OWED_TO_ME -> loans.filter { it.type == "OWED_TO_ME" }
        }
}

sealed class LoansTrackerUiIntent : UiIntent {
    data class SetFilter(val filter: LoanFilter) : LoansTrackerUiIntent()
    data class OpenAddForm(val type: String) : LoansTrackerUiIntent()
    data class OpenEditForm(val loan: LoanEntity) : LoansTrackerUiIntent()
    data object CloseForm : LoansTrackerUiIntent()

    data class SaveLoan(
        val type: String,
        val counterpartyName: String = "",
        val amount: Double,
        val categoryId: String,
        val calculationMode: String = "TOTAL_PRINCIPAL",
        val isInstallment: Boolean,
        val installmentAmount: Double?,
        val installmentDurationMonths: Int?,
        val interestRate: Double,
        val startDate: Long = System.currentTimeMillis(),
        val endDate: Long? = null,
        val note: String,
        val editingId: String? = null
    ) : LoansTrackerUiIntent()

    data class OpenPaymentDialog(val loan: LoanEntity) : LoansTrackerUiIntent()
    data object ClosePaymentDialog : LoansTrackerUiIntent()
    data class RecordPayment(val loanId: String, val amount: Double) : LoansTrackerUiIntent()

    data class SettleLoan(val loanId: String) : LoansTrackerUiIntent()
    data class DeleteLoan(val loanId: String) : LoansTrackerUiIntent()

    data class ShowAddCategoryDialog(val show: Boolean) : LoansTrackerUiIntent()
    data class CreateCategory(val name: String, val colorHex: String, val iconName: String) : LoansTrackerUiIntent()
}

sealed class LoansTrackerUiEffect : UiEffect {
    data class ShowSnackbar(val message: String) : LoansTrackerUiEffect()
}
