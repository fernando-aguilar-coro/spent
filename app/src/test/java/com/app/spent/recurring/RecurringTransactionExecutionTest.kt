package com.app.spent.recurring

import com.app.spent.data.local.dao.SpentDao
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.FamilyMemberEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.data.local.entity.ParentalControlConfigEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import com.app.spent.data.repository.SpentRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurringTransactionExecutionTest {

    private lateinit var fakeDao: FakeSpentDao
    private lateinit var repository: SpentRepositoryImpl
    private val zone = ZoneId.systemDefault()

    @Before
    fun setUp() {
        fakeDao = FakeSpentDao()
        val mockContext = android.content.ContextWrapper(null)
        val mockPrefs = com.app.spent.data.preferences.UserPreferencesRepository(mockContext)

        repository = SpentRepositoryImpl(
            context = mockContext,
            dao = fakeDao,
            preferencesRepository = mockPrefs
        )
    }

    @Test
    fun testOptionBDateCalculationsDirectly() {
        val anchorDay = 31

        // Jan 31 -> Feb 28
        val janDate = LocalDate.of(2026, 1, 31)
        val febYearMonth = java.time.YearMonth.from(janDate).plusMonths(1)
        val febDay = minOf(anchorDay, febYearMonth.lengthOfMonth())
        val febDate = febYearMonth.atDay(febDay)
        assertEquals(LocalDate.of(2026, 2, 28), febDate)

        // Feb 28 -> Mar 31 (Snaps back to 31!)
        val marYearMonth = java.time.YearMonth.from(febDate).plusMonths(1)
        val marDay = minOf(anchorDay, marYearMonth.lengthOfMonth())
        val marDate = marYearMonth.atDay(marDay)
        assertEquals(LocalDate.of(2026, 3, 31), marDate)

        // Mar 31 -> Apr 30 (Clamps to 30)
        val aprYearMonth = java.time.YearMonth.from(marDate).plusMonths(1)
        val aprDay = minOf(anchorDay, aprYearMonth.lengthOfMonth())
        val aprDate = aprYearMonth.atDay(aprDay)
        assertEquals(LocalDate.of(2026, 4, 30), aprDate)

        // Apr 30 -> May 31 (Snaps back to 31!)
        val mayYearMonth = java.time.YearMonth.from(aprDate).plusMonths(1)
        val mayDay = minOf(anchorDay, mayYearMonth.lengthOfMonth())
        val mayDate = mayYearMonth.atDay(mayDay)
        assertEquals(LocalDate.of(2026, 5, 31), mayDate)
    }

    @Test
    fun testCatchUpLoopExecutesMissedPeriods() = runTest {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val nowZdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone)

        // Set start date to exactly 3 months ago
        val threeMonthsAgo = nowZdt.minusMonths(3)
        val startTs = threeMonthsAgo.toInstant().toEpochMilli()

        val rule = RecurringRuleEntity(
            id = "rule-catchup",
            amount = 50.0,
            categoryId = "cat_subs",
            frequency = "MONTHLY",
            startDate = startTs,
            lastExecuted = startTs, // Day 0 was executed 3 months ago
            note = "Streaming Subscription",
            type = "EXPENSE"
        )
        fakeDao.recurringRules[rule.id] = rule

        repository.executePendingRecurringRules()

        // Should have created 3 transactions (1 month ago, 2 months ago, and today/this month)
        assertEquals(3, fakeDao.transactions.size)
        assertTrue(fakeDao.transactions.all { it.amount == 50.0 })
        assertTrue(fakeDao.transactions.all { it.type == "EXPENSE" })
        assertTrue(fakeDao.transactions.all { it.recurringRuleId == "rule-catchup" })

        // Check updated rule lastExecuted
        val updatedRule = fakeDao.recurringRules["rule-catchup"]!!
        assertTrue(updatedRule.lastExecuted > startTs)
    }

    @Test
    fun testRecurringIncomeCreatesIncomeTransactions() = runTest {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val twoWeeksAgo = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone).minusWeeks(2)
        val startTs = twoWeeksAgo.toInstant().toEpochMilli()

        val rule = RecurringRuleEntity(
            id = "rule-salary",
            amount = 2500.0,
            categoryId = "cat_salary",
            frequency = "WEEKLY",
            startDate = startTs,
            lastExecuted = startTs,
            note = "Weekly Salary",
            type = "INCOME"
        )
        fakeDao.recurringRules[rule.id] = rule

        repository.executePendingRecurringRules()

        // Should catch up 2 weekly payments
        assertEquals(2, fakeDao.transactions.size)
        assertTrue(fakeDao.transactions.all { it.type == "INCOME" })
        assertTrue(fakeDao.transactions.all { it.amount == 2500.0 })
    }

    @Test
    fun testNoDuplicateOnInitialCreation() = runTest {
        val now = System.currentTimeMillis()
        val rule = RecurringRuleEntity(
            id = "rule-today",
            amount = 15.0,
            categoryId = "cat_coffee",
            frequency = "DAILY",
            startDate = now,
            lastExecuted = now, // Created today with day 0 already executed
            note = "Daily coffee",
            type = "EXPENSE"
        )
        fakeDao.recurringRules[rule.id] = rule

        repository.executePendingRecurringRules()

        // Should NOT create any duplicate transaction today
        assertEquals(0, fakeDao.transactions.size)
    }

    @Test
    fun testStopRecurringRulePreservesPastTransactions() = runTest {
        val now = System.currentTimeMillis()
        val rule = RecurringRuleEntity(
            id = "rule-netflix",
            amount = 15.99,
            categoryId = "cat_subs",
            frequency = "MONTHLY",
            startDate = now,
            lastExecuted = now,
            isActive = true
        )
        fakeDao.recurringRules[rule.id] = rule
        fakeDao.transactions.add(
            TransactionEntity(
                id = "tx-1",
                amount = 15.99,
                type = "EXPENSE",
                categoryId = "cat_subs",
                timestamp = now,
                recurringRuleId = "rule-netflix"
            )
        )

        // User stops the recurring rule
        repository.stopRecurringRule("rule-netflix")

        // Rule is now inactive
        assertEquals(false, fakeDao.recurringRules["rule-netflix"]?.isActive)
        // Past transaction is preserved!
        assertEquals(1, fakeDao.transactions.size)
        assertEquals("tx-1", fakeDao.transactions[0].id)
    }

    @Test
    fun testDeleteRecurringRuleAndTransactionsWipesHistory() = runTest {
        val now = System.currentTimeMillis()
        val rule = RecurringRuleEntity(
            id = "rule-mistake",
            amount = 99.0,
            categoryId = "cat_general",
            frequency = "DAILY",
            startDate = now,
            lastExecuted = now,
            isActive = true
        )
        fakeDao.recurringRules[rule.id] = rule
        fakeDao.transactions.add(
            TransactionEntity(
                id = "tx-mistake-1",
                amount = 99.0,
                type = "EXPENSE",
                categoryId = "cat_general",
                timestamp = now,
                recurringRuleId = "rule-mistake"
            )
        )

        // User deletes rule AND all history
        repository.deleteRecurringRuleAndTransactions("rule-mistake")

        // Rule is gone and transactions are wiped!
        assertEquals(null, fakeDao.recurringRules["rule-mistake"])
        assertEquals(0, fakeDao.transactions.size)
    }

    @Test
    fun testMultipleRulesExecuteReliably() = runTest {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val twoDaysAgo = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone).minusDays(2)
        val startTs = twoDaysAgo.toInstant().toEpochMilli()

        val rule1 = RecurringRuleEntity(
            id = "rule-multi-1",
            amount = 10.0,
            categoryId = "cat_1",
            frequency = "DAILY",
            startDate = startTs,
            lastExecuted = startTs,
            isActive = true
        )
        val rule2 = RecurringRuleEntity(
            id = "rule-multi-2",
            amount = 20.0,
            categoryId = "cat_2",
            frequency = "DAILY",
            startDate = startTs,
            lastExecuted = startTs,
            isActive = true
        )

        fakeDao.recurringRules[rule1.id] = rule1
        fakeDao.recurringRules[rule2.id] = rule2

        repository.executePendingRecurringRules()

        // Rule 1 should generate 2 occurrences (yesterday and today)
        // Rule 2 should generate 2 occurrences (yesterday and today)
        assertEquals(4, fakeDao.transactions.size)
        assertEquals(2, fakeDao.transactions.count { it.recurringRuleId == "rule-multi-1" })
        assertEquals(2, fakeDao.transactions.count { it.recurringRuleId == "rule-multi-2" })
    }
}

class FakeSpentDao : SpentDao {
    val recurringRules = mutableMapOf<String, RecurringRuleEntity>()
    val transactions = mutableListOf<TransactionEntity>()

    override suspend fun getAllRecurringRules(): List<RecurringRuleEntity> = recurringRules.values.toList()
    override suspend fun getActiveRecurringRules(): List<RecurringRuleEntity> = recurringRules.values.filter { it.isActive }

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        transactions.add(transaction)
    }

    override suspend fun updateRecurringRule(rule: RecurringRuleEntity) {
        recurringRules[rule.id] = rule
    }

    override suspend fun updateRecurringRuleActiveStatus(id: String, isActive: Boolean) {
        val existing = recurringRules[id]
        if (existing != null) {
            recurringRules[id] = existing.copy(isActive = isActive)
        }
    }

    override suspend fun deleteTransactionsByRecurringRuleId(ruleId: String) {
        transactions.removeAll { it.recurringRuleId == ruleId }
    }

    override suspend fun getTransactionCountForRecurringRule(ruleId: String): Int {
        return transactions.count { it.recurringRuleId == ruleId }
    }

    override fun getUserAccountFlow(): Flow<UserAccountEntity?> = MutableStateFlow(null)
    override suspend fun insertOrUpdateUserAccount(account: UserAccountEntity) {}
    override fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>> = MutableStateFlow(emptyList())
    override suspend fun insertFamilyMember(member: FamilyMemberEntity) {}
    override fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?> = MutableStateFlow(null)
    override suspend fun insertParentalConfig(config: ParentalControlConfigEntity) {}
    override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
    override suspend fun getCategoryById(id: String): CategoryEntity? = null
    override suspend fun insertCategory(category: CategoryEntity) {}
    override suspend fun insertCategories(categories: List<CategoryEntity>) {}
    override suspend fun updateCategory(category: CategoryEntity) {}
    override suspend fun deleteCategoryById(id: String) {}
    override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = MutableStateFlow(null)
    override suspend fun getCurrentPayCycle(): PayCycleEntity? = null
    override suspend fun insertPayCycle(payCycle: PayCycleEntity) {}
    override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = MutableStateFlow(transactions)
    override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = MutableStateFlow(transactions.take(limit))
    override fun getTransactionsSinceFlow(startTime: Long): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
    override fun getTotalAmountByTypeFlow(type: String): Flow<Double> = MutableStateFlow(0.0)
    override fun getTotalAmountByTypeBetweenFlow(type: String, startTime: Long, endTime: Long): Flow<Double> = MutableStateFlow(0.0)
    override fun getCategorySpentBetweenFlow(categoryId: String, startTime: Long, endTime: Long): Flow<Double> = MutableStateFlow(0.0)
    override fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
    override suspend fun getTransactionById(id: String): TransactionEntity? = transactions.find { it.id == id }
    override suspend fun insertTransactions(transactions: List<TransactionEntity>) { this.transactions.addAll(transactions) }
    override suspend fun deleteTransaction(transaction: TransactionEntity) { transactions.remove(transaction) }
    override suspend fun deleteTransactionById(id: String) { transactions.removeAll { it.id == id } }
    override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = MutableStateFlow(recurringRules.values.toList())
    override suspend fun insertRecurringRule(rule: RecurringRuleEntity) { recurringRules[rule.id] = rule }
    override suspend fun insertRecurringRules(rules: List<RecurringRuleEntity>) { rules.forEach { recurringRules[it.id] = it } }
    override suspend fun deleteRecurringRuleById(id: String) { recurringRules.remove(id) }
    override suspend fun deleteAllRecurringRules() { recurringRules.clear() }
    override fun getLoansFlow(): Flow<List<LoanEntity>> = MutableStateFlow(emptyList())
    override suspend fun getLoanById(id: String): LoanEntity? = null
    override suspend fun insertLoan(loan: LoanEntity) {}
    override suspend fun insertLoans(loans: List<LoanEntity>) {}
    override suspend fun updateLoan(loan: LoanEntity) {}
    override suspend fun deleteLoanById(id: String) {}
    override suspend fun deleteAllTransactions() { transactions.clear() }
    override suspend fun deleteAllCategories() {}
    override suspend fun deleteAllLoans() {}
    override suspend fun deleteAllPayCycles() {}
}
