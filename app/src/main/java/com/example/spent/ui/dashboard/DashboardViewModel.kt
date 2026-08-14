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

                val isPayCycleActive = payCycle != null && payCycle.frequency != "NONE"

                val totalIncome = (if (isPayCycleActive) payCycle?.income ?: 0.0 else 0.0) + transactions
                    .filter { it.type == "INCOME" }
                    .sumOf { it.amount }

                val totalSpent = transactions
                    .filter { it.type == "EXPENSE" }
                    .sumOf { it.amount }

                // Calculate pay cycle period window and days remaining
                val cyclePeriod = if (isPayCycleActive) {
                    calculateCyclePeriod(payCycle?.frequency ?: "MONTHLY", payCycle?.startDate ?: System.currentTimeMillis())
                } else null

                val daysRemaining = cyclePeriod?.daysRemaining ?: 0

                // Safe to Spend based on current pay cycle's income & expenses
                val safeToSpend = if (isPayCycleActive && cyclePeriod != null) {
                    val cycleExpectedIncome = payCycle?.income ?: 0.0
                    val cycleExtraIncome = transactions
                        .filter { it.type == "INCOME" && it.timestamp >= cyclePeriod.startTimestamp && it.timestamp <= cyclePeriod.endTimestamp }
                        .sumOf { it.amount }
                    val cycleTotalIncome = cycleExpectedIncome + cycleExtraIncome

                    val cycleExpenses = transactions
                        .filter { it.type == "EXPENSE" && it.timestamp >= cyclePeriod.startTimestamp && it.timestamp <= cyclePeriod.endTimestamp }
                        .sumOf { it.amount }

                    val remainingInCycle = (cycleTotalIncome - cycleExpenses).coerceAtLeast(0.0)
                    if (daysRemaining > 0) remainingInCycle / daysRemaining else remainingInCycle
                } else {
                    (totalIncome - totalSpent).coerceAtLeast(0.0)
                }

                // Envelope calculations (scoped to current cycle if active)
                val envelopes = categories.map { cat ->
                    val spentInCat = transactions
                        .filter {
                            it.categoryId == cat.id &&
                            it.type == "EXPENSE" &&
                            (cyclePeriod == null || (it.timestamp >= cyclePeriod.startTimestamp && it.timestamp <= cyclePeriod.endTimestamp))
                        }
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
                    isPayCycleActive = isPayCycleActive,
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

    private data class CyclePeriod(
        val startTimestamp: Long,
        val endTimestamp: Long,
        val daysRemaining: Int
    )

    private fun calculateCyclePeriod(frequency: String, startDateTimestamp: Long): CyclePeriod {
        val now = Calendar.getInstance()
        val nowMillis = now.timeInMillis

        return when (frequency) {
            "WEEKLY" -> {
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 7)
                val end = cal.timeInMillis - 1
                val daysPassed = ((nowMillis - start) / (1000 * 60 * 60 * 24)).toInt().coerceIn(0, 6)
                val daysRemaining = (7 - daysPassed).coerceAtLeast(1)
                CyclePeriod(start, end, daysRemaining)
            }
            "BIWEEKLY" -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = startDateTimestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val biweeklyMillis = 14L * 24 * 60 * 60 * 1000
                val diff = (nowMillis - cal.timeInMillis).coerceAtLeast(0)
                val cycles = diff / biweeklyMillis
                val start = cal.timeInMillis + (cycles * biweeklyMillis)
                val end = start + biweeklyMillis - 1
                val daysPassed = ((nowMillis - start) / (1000 * 60 * 60 * 24)).toInt().coerceIn(0, 13)
                val daysRemaining = (14 - daysPassed).coerceAtLeast(1)
                CyclePeriod(start, end, daysRemaining)
            }
            "SEMIMONTHLY" -> {
                val currentDay = now.get(Calendar.DAY_OF_MONTH)
                val maxDays = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (currentDay <= 15) {
                    val start = (now.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val end = (now.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, 15)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    val daysRemaining = (15 - currentDay + 1).coerceAtLeast(1)
                    CyclePeriod(start, end, daysRemaining)
                } else {
                    val start = (now.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, 16)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val end = (now.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, maxDays)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    val daysRemaining = (maxDays - currentDay + 1).coerceAtLeast(1)
                    CyclePeriod(start, end, daysRemaining)
                }
            }
            else -> { // MONTHLY default
                val currentDay = now.get(Calendar.DAY_OF_MONTH)
                val maxDays = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                val start = (now.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = (now.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, maxDays)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                val daysRemaining = (maxDays - currentDay + 1).coerceAtLeast(1)
                CyclePeriod(start, end, daysRemaining)
            }
        }
    }

    override fun onIntent(intent: DashboardUiIntent) {
        when (intent) {
            is DashboardUiIntent.LoadData -> observeData()
            is DashboardUiIntent.AddTransaction -> addTransaction(intent.amount, intent.type, intent.categoryId, intent.note)
            is DashboardUiIntent.DeleteTransaction -> deleteTransaction(intent.transaction)
            is DashboardUiIntent.UndoDelete -> undoDelete(intent.transaction)
            is DashboardUiIntent.UpdateCategoryBudget -> updateCategoryBudget(intent.categoryId, intent.budgetAmount)
            is DashboardUiIntent.DismissWalkthrough -> dismissWalkthrough()
        }
    }

    private fun updateCategoryBudget(categoryId: String, budgetAmount: Double) {
        viewModelScope.launch {
            val cat = uiState.value.allCategories.find { it.id == categoryId }
            if (cat != null) {
                repository.updateCategory(cat.copy(budgetAmount = budgetAmount))
                sendEffect(DashboardUiEffect.ShowSnackbar("Savings goal updated to ${uiState.value.currencySymbol}${"%.2f".format(budgetAmount)}"))
            }
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
