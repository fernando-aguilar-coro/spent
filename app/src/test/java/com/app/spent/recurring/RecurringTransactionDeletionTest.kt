package com.app.spent.recurring

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.FamilyMemberEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.data.local.entity.ParentalControlConfigEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.sync.DriveConnectResult
import com.app.spent.data.sync.SharedMemberInfo
import com.app.spent.ui.dashboard.DashboardUiIntent
import com.app.spent.ui.dashboard.DashboardViewModel
import com.app.spent.ui.dashboard.RecurringDeleteMode
import com.app.spent.ui.history.TransactionHistoryUiIntent
import com.app.spent.ui.history.TransactionHistoryViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringTransactionDeletionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: TestSpentRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = TestSpentRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDashboardDeleteOnlyThisOccurrenceLeavesRuleActive() = runTest {
        val rule = RecurringRuleEntity(
            id = "rule-netflix",
            amount = 15.99,
            categoryId = "cat_general",
            frequency = "MONTHLY",
            note = "Bill: Netflix",
            isActive = true
        )
        val tx = TransactionEntity(
            id = "tx-netflix-1",
            amount = 15.99,
            type = "EXPENSE",
            categoryId = "cat_general",
            note = "Bill: Netflix",
            recurringRuleId = "rule-netflix"
        )
        fakeRepo.rulesMap[rule.id] = rule
        fakeRepo.txMap[tx.id] = tx
        fakeRepo.refreshFlows()

        val viewModel = DashboardViewModel(fakeRepo)
        advanceUntilIdle()

        // User deletes ONLY this transaction occurrence
        viewModel.onIntent(DashboardUiIntent.DeleteTransaction(tx, RecurringDeleteMode.ONLY_THIS_OCCURRENCE))
        advanceUntilIdle()

        // Transaction is gone
        assertNull(fakeRepo.txMap[tx.id])
        // Rule remains intact and active!
        assertTrue(fakeRepo.rulesMap[rule.id]?.isActive == true)
    }

    @Test
    fun testDashboardDeleteAndStopFutureStopsRule() = runTest {
        val rule = RecurringRuleEntity(
            id = "rule-gym",
            amount = 30.0,
            categoryId = "cat_general",
            frequency = "MONTHLY",
            note = "Bill: Gym Membership",
            isActive = true
        )
        val tx = TransactionEntity(
            id = "tx-gym-1",
            amount = 30.0,
            type = "EXPENSE",
            categoryId = "cat_general",
            note = "Bill: Gym Membership",
            recurringRuleId = "rule-gym"
        )
        fakeRepo.rulesMap[rule.id] = rule
        fakeRepo.txMap[tx.id] = tx
        fakeRepo.refreshFlows()

        val viewModel = DashboardViewModel(fakeRepo)
        advanceUntilIdle()

        // User deletes transaction AND stops future recurring bills
        viewModel.onIntent(DashboardUiIntent.DeleteTransaction(tx, RecurringDeleteMode.DELETE_AND_STOP_FUTURE))
        advanceUntilIdle()

        // Transaction is deleted
        assertNull(fakeRepo.txMap[tx.id])
        // Rule is stopped!
        assertEquals(false, fakeRepo.rulesMap[rule.id]?.isActive)
    }

    @Test
    fun testDashboardDeleteAllHistoryWipesRuleAndTransactions() = runTest {
        val rule = RecurringRuleEntity(
            id = "rule-wifi",
            amount = 50.0,
            categoryId = "cat_general",
            frequency = "MONTHLY",
            note = "Bill: Fiber Wifi",
            isActive = true
        )
        val tx1 = TransactionEntity(
            id = "tx-wifi-1",
            amount = 50.0,
            type = "EXPENSE",
            categoryId = "cat_general",
            note = "Bill: Fiber Wifi",
            recurringRuleId = "rule-wifi"
        )
        val tx2 = TransactionEntity(
            id = "tx-wifi-2",
            amount = 50.0,
            type = "EXPENSE",
            categoryId = "cat_general",
            note = "Bill: Fiber Wifi",
            recurringRuleId = "rule-wifi"
        )
        fakeRepo.rulesMap[rule.id] = rule
        fakeRepo.txMap[tx1.id] = tx1
        fakeRepo.txMap[tx2.id] = tx2
        fakeRepo.refreshFlows()

        val viewModel = DashboardViewModel(fakeRepo)
        advanceUntilIdle()

        // User purges rule and all history
        viewModel.onIntent(DashboardUiIntent.DeleteTransaction(tx1, RecurringDeleteMode.DELETE_ALL_HISTORY))
        advanceUntilIdle()

        // Rule and all associated transactions are erased!
        assertNull(fakeRepo.rulesMap[rule.id])
        assertEquals(0, fakeRepo.txMap.size)
    }

    @Test
    fun testTransactionHistoryViewModelSupportsRecurringDeleteModes() = runTest {
        val rule = RecurringRuleEntity(
            id = "rule-spotify",
            amount = 9.99,
            categoryId = "cat_general",
            frequency = "MONTHLY",
            note = "Bill: Spotify",
            isActive = true
        )
        val tx = TransactionEntity(
            id = "tx-spot-1",
            amount = 9.99,
            type = "EXPENSE",
            categoryId = "cat_general",
            note = "Bill: Spotify",
            recurringRuleId = "rule-spotify"
        )
        fakeRepo.rulesMap[rule.id] = rule
        fakeRepo.txMap[tx.id] = tx
        fakeRepo.refreshFlows()

        val historyViewModel = TransactionHistoryViewModel(fakeRepo)
        advanceUntilIdle()

        historyViewModel.onIntent(TransactionHistoryUiIntent.DeleteTransaction(tx, RecurringDeleteMode.DELETE_AND_STOP_FUTURE))
        advanceUntilIdle()

        assertNull(fakeRepo.txMap[tx.id])
        assertEquals(false, fakeRepo.rulesMap[rule.id]?.isActive)
    }

    @Test
    fun testPendingBillsCoherenceOnDashboard() = runTest {
        val rule = RecurringRuleEntity(
            id = "rule-elec",
            amount = 75.0,
            categoryId = "cat_general",
            frequency = "MONTHLY",
            note = "Bill: Electricity",
            startDate = System.currentTimeMillis() - 100000,
            isActive = true
        )
        fakeRepo.rulesMap[rule.id] = rule
        fakeRepo.refreshFlows()

        val viewModel = DashboardViewModel(fakeRepo)
        advanceUntilIdle()

        // Bill is active and unpaid this cycle -> pendingBillsCount = 1, pendingBillsTotal = 75.0
        val stateUnpaid = viewModel.uiState.value
        assertEquals(1, stateUnpaid.pendingBillsCount)
        assertEquals(75.0, stateUnpaid.pendingBillsTotal, 0.001)

        // Now record a payment transaction for this bill in current cycle
        val paymentTx = TransactionEntity(
            id = "tx-elec-paid",
            amount = 75.0,
            type = "EXPENSE",
            categoryId = "cat_general",
            note = "Bill: Electricity",
            timestamp = System.currentTimeMillis(),
            recurringRuleId = "rule-elec"
        )
        fakeRepo.txMap[paymentTx.id] = paymentTx
        fakeRepo.refreshFlows()
        advanceUntilIdle()

        // Now pending count should be 0!
        val statePaid = viewModel.uiState.value
        assertEquals(0, statePaid.pendingBillsCount)
        assertEquals(0.0, statePaid.pendingBillsTotal, 0.001)
    }
}

private class TestSpentRepository : SpentRepository {
    val txMap = mutableMapOf<String, TransactionEntity>()
    val rulesMap = mutableMapOf<String, RecurringRuleEntity>()

    private val txFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private val rulesFlow = MutableStateFlow<List<RecurringRuleEntity>>(emptyList())
    private val catFlow = MutableStateFlow<List<CategoryEntity>>(
        listOf(CategoryEntity(id = "cat_general", name = "General", iconName = "Category", colorHex = "#64748B"))
    )

    fun refreshFlows() {
        txFlow.value = txMap.values.toList()
        rulesFlow.value = rulesMap.values.toList()
    }

    override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = txFlow.asStateFlow()
    override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = txFlow.asStateFlow()
    override fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>> = txFlow.asStateFlow()
    override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = catFlow.asStateFlow()
    override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = MutableStateFlow(null)
    override fun getUserAccountFlow(): Flow<UserAccountEntity?> = MutableStateFlow(null)
    override fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>> = MutableStateFlow(emptyList())
    override fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?> = MutableStateFlow(null)
    override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = rulesFlow.asStateFlow()
    override fun getLoansFlow(): Flow<List<LoanEntity>> = MutableStateFlow(emptyList())

    override val isWalkthroughCompletedFlow: Flow<Boolean> = MutableStateFlow(true)
    override val isDarkThemeFlow: Flow<Boolean?> = MutableStateFlow(null)
    override val currencySymbolFlow: Flow<String> = MutableStateFlow("$")
    override val appLanguageFlow: Flow<String?> = MutableStateFlow(null)
    override val savingsGoalNameFlow: Flow<String> = MutableStateFlow("")
    override val savingsGoalTotalFlow: Flow<Double> = MutableStateFlow(0.0)
    override val savingsMonthlyContributionFlow: Flow<Double> = MutableStateFlow(0.0)
    override val lastDriveSyncTimestampFlow: Flow<Long> = MutableStateFlow(0L)
    override val isDriveConnectedFlow: Flow<Boolean> = MutableStateFlow(false)
    override val driveAccountEmailFlow: Flow<String?> = MutableStateFlow(null)
    override val isSyncingDriveFlow: Flow<Boolean> = MutableStateFlow(false)
    override val partnerDriveFileIdFlow: Flow<String?> = MutableStateFlow(null)
    override val partnerNameFlow: Flow<String?> = MutableStateFlow(null)
    override val partnerEmailFlow: Flow<String?> = MutableStateFlow(null)
    override val partnerLastSyncTimestampFlow: Flow<Long> = MutableStateFlow(0L)
    override val isPartnerPairedFlow: Flow<Boolean> = MutableStateFlow(false)
    override val sharedMembersFlow: Flow<List<SharedMemberInfo>> = MutableStateFlow(emptyList())
    override val imageStorageLocationFlow: Flow<String> = MutableStateFlow("GOOGLE_DRIVE")

    override suspend fun connectGoogleDrive(account: GoogleSignInAccount): DriveConnectResult = DriveConnectResult.ConnectedNew
    override suspend fun disconnectGoogleDrive() {}
    override suspend fun syncToGoogleDrive(): Result<Boolean> = Result.success(true)
    override fun triggerAutoSync() {}
    override suspend fun getOwnBackupFileId(): Result<String?> = Result.success(null)
    override suspend fun enablePublicLinkSharing(fileId: String): Result<String> = Result.success("")
    override suspend fun processAndSaveImage(sourceUri: android.net.Uri, destinationType: String): Result<String> = Result.success("")
    override suspend fun addOrUpdateSharedMember(member: SharedMemberInfo) {}
    override suspend fun updateSharedMemberName(fileId: String, newName: String) {}
    override suspend fun updateUserProfileName(newName: String) {}
    override suspend fun removeSharedMember(fileId: String) {}
    override suspend fun clearSharedMembers() {}
    override suspend fun savePartnerInfo(fileId: String, name: String, email: String?) {}
    override suspend fun setPartnerLastSyncTimestamp(timestamp: Long) {}
    override suspend fun clearPartnerInfo() {}

    override suspend fun addTransaction(transaction: TransactionEntity) {
        txMap[transaction.id] = transaction
        refreshFlows()
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        txMap[transaction.id] = transaction
        refreshFlows()
    }

    override suspend fun getTransactionById(id: String): TransactionEntity? = txMap[id]

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        txMap.remove(transaction.id)
        refreshFlows()
    }

    override suspend fun deleteTransactionById(id: String) {
        txMap.remove(id)
        refreshFlows()
    }

    override suspend fun addCategory(category: CategoryEntity) {}
    override suspend fun updateCategory(category: CategoryEntity) {}
    override suspend fun deleteCategoryById(id: String) {}
    override suspend fun setPayCycle(payCycle: PayCycleEntity) {}

    override suspend fun addRecurringRule(rule: RecurringRuleEntity) {
        rulesMap[rule.id] = rule
        refreshFlows()
    }

    override suspend fun updateRecurringRule(rule: RecurringRuleEntity) {
        rulesMap[rule.id] = rule
        refreshFlows()
    }

    override suspend fun stopRecurringRule(id: String) {
        val rule = rulesMap[id]
        if (rule != null) {
            rulesMap[id] = rule.copy(isActive = false)
            refreshFlows()
        }
    }

    override suspend fun deleteRecurringRuleById(id: String) {
        rulesMap.remove(id)
        refreshFlows()
    }

    override suspend fun deleteRecurringRuleAndTransactions(id: String) {
        rulesMap.remove(id)
        val toRemove = txMap.values.filter { it.recurringRuleId == id }.map { it.id }
        toRemove.forEach { txMap.remove(it) }
        refreshFlows()
    }

    override suspend fun executePendingRecurringRules() {}
    override suspend fun seedStarterDataIfEmpty() {}
    override suspend fun addLoan(loan: LoanEntity) {}
    override suspend fun updateLoan(loan: LoanEntity) {}
    override suspend fun deleteLoanById(id: String) {}
    override suspend fun recordLoanPayment(loanId: String, amount: Double) {}
    override suspend fun getLoanById(id: String): LoanEntity? = null
    override suspend fun setWalkthroughCompleted(completed: Boolean) {}
    override suspend fun setDarkThemeMode(enabled: Boolean?) {}
    override suspend fun setCurrencySymbol(symbol: String) {}
    override suspend fun setAppLanguage(languageCode: String?) {}
    override suspend fun setImageStorageLocation(location: String) {}
    override suspend fun setSavingsGoal(name: String, totalGoal: Double, monthlyContribution: Double) {}
    override suspend fun clearSavingsGoal() {}
    override suspend fun setLastDriveSyncTimestamp(timestamp: Long) {}
    override suspend fun restoreAllData(
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        payCycle: PayCycleEntity?,
        recurringRules: List<RecurringRuleEntity>,
        userAccount: UserAccountEntity?,
        loans: List<LoanEntity>
    ) {}
    override suspend fun resetAllData(deleteDriveImages: Boolean) {}
}
