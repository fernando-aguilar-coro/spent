package com.example.spent.ui.dashboard

import androidx.lifecycle.viewModelScope
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.PayCycleEntity
import com.example.spent.data.local.entity.RecurringRuleEntity
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

            val coreDataFlow = combine(
                repository.getTransactionsFlow(),
                repository.getCategoriesFlow(),
                repository.getCurrentPayCycleFlow()
            ) { transactions: List<TransactionEntity>, categories: List<CategoryEntity>, payCycle: PayCycleEntity? ->
                Triple(transactions, categories, payCycle)
            }

            val metaDataFlow = combine(
                repository.getRecurringRulesFlow(),
                repository.isWalkthroughCompletedFlow,
                repository.currencySymbolFlow,
                repository.savingsMonthlyContributionFlow
            ) { recurringRules: List<RecurringRuleEntity>, walkthroughDone: Boolean, currency: String, monthlySavings: Double ->
                DashboardMetaData(recurringRules, walkthroughDone, currency, monthlySavings)
            }

            combine(coreDataFlow, metaDataFlow) { (transactions, categories, payCycle), meta ->
                val (recurringRules, walkthroughDone, currency, monthlySavings) = meta
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
                    // Structured Pay Cycle: Base Salary + extra income categorized under Salary
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

                    // Remaining Discretionary Income = Salary Income - Pending Fixed Bills (Approx) - Monthly Savings Contribution - Spent In Cycle
                    val remainingDiscretionary = cycleTotalIncome - pendingFixedBills - monthlySavings - spentInCycle
                    (remainingDiscretionary.coerceAtLeast(0.0)) / (daysRemaining + 1)
                } else {
                    // Freelance / Flexible / Unemployed Mode: Funded strictly by income transactions categorized as Salary
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

                    // Freelance Safe Daily Spend = (Salary Income - Pending Fixed Bills - Monthly Savings - Spent) / (Days Remaining + 1)
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
                    recentTransactions = transactions.take(20),
                    allCategories = categories,
                    recurringRules = recurringRules,
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
        val now = Calendar.getInstance()
        val nowMillis = now.timeInMillis

        return when (frequency) {
            "WEEKLY" -> {
                val anchorCal = Calendar.getInstance().apply { timeInMillis = startDateTimestamp }
                val anchorDayOfWeek = anchorCal.get(Calendar.DAY_OF_WEEK)

                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = anchorDayOfWeek
                    set(Calendar.DAY_OF_WEEK, anchorDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis > nowMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                }
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 7)
                val end = cal.timeInMillis - 1
                val daysRemaining = (((end - nowMillis) / (1000 * 60 * 60 * 24)).toInt()).coerceAtLeast(0)
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
                val diff = nowMillis - cal.timeInMillis
                val cycles = if (diff >= 0) diff / biweeklyMillis else (diff / biweeklyMillis) - 1
                val start = cal.timeInMillis + (cycles * biweeklyMillis)
                val end = start + biweeklyMillis - 1
                val daysRemaining = (((end - nowMillis) / (1000 * 60 * 60 * 24)).toInt()).coerceAtLeast(0)
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
                    val daysRemaining = (15 - currentDay).coerceAtLeast(0)
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
                    val daysRemaining = (maxDays - currentDay).coerceAtLeast(0)
                    CyclePeriod(start, end, daysRemaining)
                }
            }
            "MONTHLY" -> {
                val anchorCal = Calendar.getInstance().apply { timeInMillis = startDateTimestamp }
                val anchorDay = anchorCal.get(Calendar.DAY_OF_MONTH).coerceIn(1, 31)
                val currentDay = now.get(Calendar.DAY_OF_MONTH)

                val startCal = (now.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (currentDay >= anchorDay) {
                    val maxDayThisMonth = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    startCal.set(Calendar.DAY_OF_MONTH, anchorDay.coerceAtMost(maxDayThisMonth))
                } else {
                    startCal.add(Calendar.MONTH, -1)
                    val maxDayPrevMonth = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    startCal.set(Calendar.DAY_OF_MONTH, anchorDay.coerceAtMost(maxDayPrevMonth))
                }

                val endCal = (startCal.clone() as Calendar).apply {
                    add(Calendar.MONTH, 1)
                    add(Calendar.MILLISECOND, -1)
                }

                val start = startCal.timeInMillis
                val end = endCal.timeInMillis
                val daysRemaining = (((end - nowMillis) / (1000 * 60 * 60 * 24)).toInt()).coerceAtLeast(0)
                CyclePeriod(start, end, daysRemaining)
            }
            else -> { // "NONE" / Flexible Month Period
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
                val daysRemaining = (maxDays - currentDay).coerceAtLeast(0)
                CyclePeriod(start, end, daysRemaining)
            }
        }
    }

    override fun onIntent(intent: DashboardUiIntent) {
        when (intent) {
            is DashboardUiIntent.LoadData -> observeData()
            is DashboardUiIntent.AddTransaction -> addTransaction(intent.amount, intent.type, intent.categoryId, intent.note)
            is DashboardUiIntent.AddRecurringRule -> addRecurringRule(intent.amount, intent.categoryId, intent.note, intent.dueDay, intent.durationMonths)
            is DashboardUiIntent.DeleteRecurringRule -> deleteRecurringRule(intent.ruleId)
            is DashboardUiIntent.DeleteTransaction -> deleteTransaction(intent.transaction)
            is DashboardUiIntent.UndoDelete -> undoDelete(intent.transaction)
            is DashboardUiIntent.UpdateCategoryBudget -> updateCategoryBudget(intent.categoryId, intent.budgetAmount)
            is DashboardUiIntent.DismissWalkthrough -> dismissWalkthrough()
        }
    }

    private fun addRecurringRule(amount: Double, categoryId: String, note: String, dueDay: Int, durationMonths: Int?) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                val maxD = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, dueDay.coerceAtMost(maxD))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endCalMillis = if (durationMonths != null && durationMonths > 0) {
                (cal.clone() as Calendar).apply {
                    add(Calendar.MONTH, durationMonths)
                }.timeInMillis
            } else null

            val rule = RecurringRuleEntity(
                id = UUID.randomUUID().toString(),
                amount = amount,
                categoryId = categoryId,
                frequency = "MONTHLY",
                startDate = cal.timeInMillis,
                endDate = endCalMillis,
                note = note
            )
            repository.addRecurringRule(rule)
            sendEffect(DashboardUiEffect.ShowSnackbar("Recurring bill / installment scheduled!"))
        }
    }

    private fun deleteRecurringRule(ruleId: String) {
        viewModelScope.launch {
            repository.deleteRecurringRuleById(ruleId)
            sendEffect(DashboardUiEffect.ShowSnackbar("Bill removed successfully"))
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
