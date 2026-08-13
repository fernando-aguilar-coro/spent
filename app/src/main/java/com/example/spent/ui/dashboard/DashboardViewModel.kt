package com.example.spent.ui.dashboard

import androidx.lifecycle.viewModelScope
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.data.repository.SpentRepository
import com.example.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class DashboardViewModel(
    private val repository: SpentRepository
) : BaseViewModel<DashboardUiState, DashboardUiIntent, DashboardUiEffect>(DashboardUiState()) {

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.seedStarterDataIfEmpty()

            combine(
                repository.getTransactionsFlow(),
                repository.getCategoriesFlow(),
                repository.getCurrentPayCycleFlow(),
                repository.isWalkthroughCompletedFlow,
                repository.currencySymbolFlow
            ) { transactions, categories, payCycle, walkthroughDone, currency ->

                val totalIncome = (payCycle?.income ?: 0.0) + transactions
                    .filter { it.type == "INCOME" }
                    .sumOf { it.amount }

                val totalSpent = transactions
                    .filter { it.type == "EXPENSE" }
                    .sumOf { it.amount }

                // Safe to Spend Today calculation
                val daysRemaining = calculateDaysRemainingInCycle(payCycle?.startDate)
                val remainingBudget = (totalIncome - totalSpent).coerceAtLeast(0.0)
                val safeToSpend = if (daysRemaining > 0) remainingBudget / daysRemaining else remainingBudget

                // Envelope calculations
                val envelopes = categories.map { cat ->
                    val spentInCat = transactions
                        .filter { it.categoryId == cat.id && it.type == "EXPENSE" }
                        .sumOf { it.amount }
                    val remaining = (cat.budgetAmount - spentInCat).coerceAtLeast(0.0)
                    val progress = if (cat.budgetAmount > 0) (spentInCat / cat.budgetAmount).toFloat().coerceIn(0f, 1f) else 0f

                    CategoryEnvelopeState(
                        category = cat,
                        spentAmount = spentInCat,
                        remainingAmount = remaining,
                        progress = progress
                    )
                }

                DashboardUiState(
                    isLoading = false,
                    currencySymbol = currency,
                    totalIncome = totalIncome,
                    totalSpent = totalSpent,
                    safeToSpendToday = safeToSpend,
                    daysRemainingInCycle = daysRemaining,
                    categoriesWithProgress = envelopes,
                    recentTransactions = transactions.take(20),
                    allCategories = categories,
                    currentPayCycle = payCycle,
                    isWalkthroughCompleted = walkthroughDone
                )
            }.collect { newState ->
                setState { newState }
            }
        }
    }

    private fun calculateDaysRemainingInCycle(startDateTimestamp: Long?): Int {
        if (startDateTimestamp == null) return 30
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (maxDays - currentDay + 1).coerceAtLeast(1)
    }

    override fun onIntent(intent: DashboardUiIntent) {
        when (intent) {
            is DashboardUiIntent.LoadData -> observeData()
            is DashboardUiIntent.AddTransaction -> addTransaction(intent.amount, intent.type, intent.categoryId, intent.note)
            is DashboardUiIntent.DeleteTransaction -> deleteTransaction(intent.transaction)
            is DashboardUiIntent.UndoDelete -> undoDelete(intent.transaction)
            is DashboardUiIntent.DismissWalkthrough -> dismissWalkthrough()
        }
    }

    private fun addTransaction(amount: Double, type: String, categoryId: String, note: String) {
        viewModelScope.launch {
            val newTx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                amount = amount,
                type = type,
                categoryId = categoryId,
                note = note,
                timestamp = System.currentTimeMillis()
            )
            repository.addTransaction(newTx)
            sendEffect(DashboardUiEffect.ShowSnackbar("Transaction logged successfully!"))
        }
    }

    private fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            sendEffect(
                DashboardUiEffect.ShowSnackbar(
                    message = "Transaction deleted",
                    actionLabel = "UNDO",
                    onAction = { onIntent(DashboardUiIntent.UndoDelete(transaction)) }
                )
            )
        }
    }

    private fun undoDelete(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
        }
    }

    private fun dismissWalkthrough() {
        viewModelScope.launch {
            repository.setWalkthroughCompleted(true)
        }
    }
}
