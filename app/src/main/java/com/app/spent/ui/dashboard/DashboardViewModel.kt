package com.app.spent.ui.dashboard

import java.util.Calendar
import java.util.UUID
import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: SpentRepository
) : BaseViewModel<DashboardUiState, DashboardUiIntent, DashboardUiEffect>(DashboardUiState()) {

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.seedStarterDataIfEmpty()
            repository.executePendingRecurringRules()

            val coreDataFlow = combine(
                repository.getTransactionsFlow(),
                repository.getCategoriesFlow(),
                repository.getCurrentPayCycleFlow()
            ) { transactions: List<TransactionEntity>, categories: List<CategoryEntity>, payCycle: PayCycleEntity? ->
                Triple(transactions, categories, payCycle)
            }

            val metaDataFlow = combine(
                repository.getRecurringRulesFlow(),
                repository.getLoansFlow(),
                repository.isWalkthroughCompletedFlow,
                repository.currencySymbolFlow,
                repository.savingsMonthlyContributionFlow
            ) { recurringRules, loans, walkthroughDone, currency, monthlySavings ->
                DashboardMetaData(recurringRules, loans, walkthroughDone, currency, monthlySavings)
            }

            combine(coreDataFlow, metaDataFlow) { (transactions, categories, payCycle), meta ->
                val (recurringRules, loans, walkthroughDone, currency, monthlySavings) = meta
                val isPayCycleActive = payCycle != null && payCycle.frequency != "NONE"

                val cyclePeriod = if (isPayCycleActive) {
                    calculateCyclePeriod(payCycle?.frequency ?: "MONTHLY", payCycle?.startDate ?: System.currentTimeMillis())
                } else {
                    calculateCyclePeriod("NONE", System.currentTimeMillis())
                }

                val daysRemaining = cyclePeriod.daysRemaining

                // Calculate Previous Cycle Window for variable bill approximations (Water, Electricity, Gas, Internet)
                val previousCycleDuration = (cyclePeriod.endTimestamp - cyclePeriod.startTimestamp).coerceAtLeast(1L)
                val prevCycleStart = cyclePeriod.startTimestamp - previousCycleDuration
                val prevCycleEnd = cyclePeriod.startTimestamp - 1

                // Smart Fixed Bills calculation with dynamic approximation for pending bills
                val nowMillis = System.currentTimeMillis()
                val activeRules = recurringRules.filter { it.endDate == null || it.endDate >= nowMillis }

                var pendingFixedBills = 0.0

                activeRules.forEach { rule ->
                    val cleanRuleName = rule.note.removePrefix("Bill: ").removePrefix("Factura: ").removePrefix("Debt Installment: ").trim()

                    // Check if this bill/installment has already been paid/logged in the CURRENT cycle
                    val hasBeenPaidThisCycle = transactions.any { tx ->
                        tx.type == "EXPENSE" &&
                        (tx.recurringRuleId == rule.id || (cleanRuleName.isNotBlank() && tx.note.contains(cleanRuleName, ignoreCase = true))) &&
                        tx.timestamp in cyclePeriod.startTimestamp..cyclePeriod.endTimestamp
                    }

                    if (!hasBeenPaidThisCycle) {
                        // Not paid yet: approximate from previous cycle transaction if available, otherwise baseline rule amount
                        val prevCyclePayment = transactions.firstOrNull { tx ->
                            tx.type == "EXPENSE" &&
                            (tx.recurringRuleId == rule.id || (cleanRuleName.isNotBlank() && tx.note.contains(cleanRuleName, ignoreCase = true))) &&
                            tx.timestamp in prevCycleStart..prevCycleEnd
                        }
                        val estimatedAmount = prevCyclePayment?.amount ?: rule.amount
                        pendingFixedBills += estimatedAmount
                    }
                }

                // Overall total income & spent across all time for ledger cards
                val totalIncome = transactions
                    .filter { it.type == "INCOME" }
                    .sumOf { it.amount }

                val totalSpent = transactions
                    .filter { it.type == "EXPENSE" }
                    .sumOf { it.amount }

                // Safe to Spend Today: funded strictly by Base Salary + Salary-categorized income, reserving monthly savings contribution
                val safeToSpend = if (isPayCycleActive) {
                    val baseIncome = payCycle?.income ?: 0.0
                    val extraSalaryIncome = transactions
                        .filter {
                            it.type == "INCOME" &&
                            it.timestamp in cyclePeriod.startTimestamp..cyclePeriod.endTimestamp &&
                            (it.categoryId == "cat_salary" || it.note.contains("Salary", ignoreCase = true) || it.note.contains("Sueldo", ignoreCase = true)) &&
                            !it.note.contains("Payday Base Salary", ignoreCase = true)
                        }
                        .sumOf { it.amount }
                    val cycleTotalIncome = baseIncome + extraSalaryIncome

                    val spentInCycle = transactions
                        .filter { it.type == "EXPENSE" && it.timestamp in cyclePeriod.startTimestamp..cyclePeriod.endTimestamp }
                        .sumOf { it.amount }

                    val remainingDiscretionary = cycleTotalIncome - pendingFixedBills - monthlySavings - spentInCycle
                    (remainingDiscretionary.coerceAtLeast(0.0)) / (daysRemaining + 1)
                } else {
                    val salaryIncomeThisMonth = transactions
                        .filter {
                            it.type == "INCOME" &&
                            (it.categoryId == "cat_salary" || it.note.contains("Salary", ignoreCase = true) || it.note.contains("Sueldo", ignoreCase = true)) &&
                            it.timestamp in cyclePeriod.startTimestamp..cyclePeriod.endTimestamp
                        }
                        .sumOf { it.amount }

                    val spentThisMonth = transactions
                        .filter { it.type == "EXPENSE" && it.timestamp in cyclePeriod.startTimestamp..cyclePeriod.endTimestamp }
                        .sumOf { it.amount }

                    val remainingDiscretionary = salaryIncomeThisMonth - pendingFixedBills - monthlySavings - spentThisMonth
                    (remainingDiscretionary.coerceAtLeast(0.0)) / (daysRemaining + 1)
                }

                // Envelope calculations (scoped to current cycle period)
                val envelopes = categories.map { cat ->
                    val spentInCat = transactions
                        .filter {
                            it.categoryId == cat.id &&
                            it.type == "EXPENSE" &&
                            it.timestamp in cyclePeriod.startTimestamp..cyclePeriod.endTimestamp
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
                    recentTransactions = transactions.filter { it.type != "SAVING" }.take(20),
                    allCategories = categories,
                    recurringRules = recurringRules,
                    loans = loans,
                    currentPayCycle = payCycle,
                    isWalkthroughCompleted = walkthroughDone
                )
            }.collect { newState ->
                setState { newState }
            }
        }
    }

    private data class DashboardMetaData(
        val recurringRules: List<RecurringRuleEntity>,
        val loans: List<com.app.spent.data.local.entity.LoanEntity>,
        val walkthroughDone: Boolean,
        val currency: String,
        val monthlySavings: Double
    )

    private data class CyclePeriod(
        val startTimestamp: Long,
        val endTimestamp: Long,
        val daysRemaining: Int
    )

    private fun calculateCyclePeriod(frequency: String, startDateTimestamp: Long): CyclePeriod {
        val zone = java.time.ZoneId.systemDefault()
        val now = java.time.LocalDate.now(zone)
        val nowMillis = System.currentTimeMillis()
        val anchorDate = java.time.Instant.ofEpochMilli(startDateTimestamp).atZone(zone).toLocalDate()

        return when (frequency) {
            "WEEKLY" -> {
                val anchorDayOfWeek = anchorDate.dayOfWeek
                val start = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(anchorDayOfWeek))
                val end = start.plusDays(6)
                val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = end.atTime(java.time.LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, end).toInt().coerceAtLeast(0)
                CyclePeriod(startMillis, endMillis, daysRemaining)
            }
            "BIWEEKLY" -> {
                val biweeklyMillis = 14L * 24 * 60 * 60 * 1000
                val anchorMillis = anchorDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val diff = nowMillis - anchorMillis
                val cycles = if (diff >= 0) diff / biweeklyMillis else (diff / biweeklyMillis) - 1
                val start = anchorMillis + (cycles * biweeklyMillis)
                val end = start + biweeklyMillis - 1
                val daysRemaining = (((end - nowMillis) / (1000 * 60 * 60 * 24)).toInt()).coerceAtLeast(0)
                CyclePeriod(start, end, daysRemaining)
            }
            "SEMIMONTHLY" -> {
                val semimonthlyMillis = 15L * 24 * 60 * 60 * 1000
                val anchorMillis = anchorDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val diff = nowMillis - anchorMillis
                val cycles = if (diff >= 0) diff / semimonthlyMillis else (diff / semimonthlyMillis) - 1
                val start = anchorMillis + (cycles * semimonthlyMillis)
                val end = start + semimonthlyMillis - 1
                val daysRemaining = (((end - nowMillis) / (1000 * 60 * 60 * 24)).toInt()).coerceAtLeast(0)
                CyclePeriod(start, end, daysRemaining)
            }
            "MONTHLY" -> {
                val anchorDay = anchorDate.dayOfMonth.coerceIn(1, 31)
                val currentDay = now.dayOfMonth

                val start = if (currentDay >= anchorDay) {
                    val maxDayThisMonth = now.lengthOfMonth()
                    now.withDayOfMonth(anchorDay.coerceAtMost(maxDayThisMonth))
                } else {
                    val prevMonth = now.minusMonths(1)
                    val maxDayPrevMonth = prevMonth.lengthOfMonth()
                    prevMonth.withDayOfMonth(anchorDay.coerceAtMost(maxDayPrevMonth))
                }

                val nextMonth = start.plusMonths(1)
                val maxDayNextMonth = nextMonth.lengthOfMonth()
                val end = nextMonth.withDayOfMonth(anchorDay.coerceAtMost(maxDayNextMonth)).minusDays(1)

                val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = end.atTime(java.time.LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, end).toInt().coerceAtLeast(0)
                CyclePeriod(startMillis, endMillis, daysRemaining)
            }
            else -> { // "NONE" / Flexible Month Period
                val start = now.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth())
                val end = now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
                val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = end.atTime(java.time.LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, end).toInt().coerceAtLeast(0)
                CyclePeriod(startMillis, endMillis, daysRemaining)
            }
        }
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
                    actionLabel = "Undo",
                    onAction = { onIntent(DashboardUiIntent.UndoDelete(transaction)) }
                )
            )
        }
    }

    private fun undoDelete(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
            sendEffect(DashboardUiEffect.ShowSnackbar("Transaction restored"))
        }
    }

    private fun dismissWalkthrough() {
        viewModelScope.launch {
            repository.setWalkthroughCompleted(true)
        }
    }
}
