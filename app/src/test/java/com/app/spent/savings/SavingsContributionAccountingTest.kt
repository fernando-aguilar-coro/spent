package com.app.spent.savings

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
import com.app.spent.ui.dashboard.DashboardViewModel
import com.app.spent.ui.savings.SavingsUiIntent
import com.app.spent.ui.savings.SavingsViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsContributionAccountingTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeSavingsRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeSavingsRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSettingMonthlyContributionAutomaticallyCreatesInitialContributionCappedAtGoal() = runTest {
        val savingsViewModel = SavingsViewModel(fakeRepo)
        advanceUntilIdle()

        // Set a goal of 300 with 100 monthly contribution
        savingsViewModel.onIntent(SavingsUiIntent.SetSavingsGoal("Vacation", 300.0, 100.0))
        advanceUntilIdle()

        // A contribution of 100 should be automatically created upon establishing the monthly contribution
        val savingTxs = fakeRepo.transactionsMap.values.filter { it.type == "SAVING" }
        assertEquals(1, savingTxs.size)
        assertEquals(100.0, savingTxs.first().amount, 0.001)
        assertEquals("cat_savings", savingTxs.first().categoryId)

        // Now set another goal where remaining is smaller than monthly contribution (e.g. goal = 150, current saved = 100)
        savingsViewModel.onIntent(SavingsUiIntent.SetSavingsGoal("Vacation", 150.0, 100.0))
        advanceUntilIdle()

        // Only 50 remaining, so only 50 should be contributed
        val allSavingTxs = fakeRepo.transactionsMap.values.filter { it.type == "SAVING" }
        val totalSaved = allSavingTxs.sumOf { it.amount }
        assertEquals(150.0, totalSaved, 0.001)
    }

    @Test
    fun testManualDepositCappedAtGoalMax() = runTest {
        val savingsViewModel = SavingsViewModel(fakeRepo)
        advanceUntilIdle()

        // Set goal to 200 without auto contribution
        fakeRepo.setSavingsGoal("New Car", 200.0, 0.0)
        advanceUntilIdle()

        // Deposit 150
        savingsViewModel.onIntent(SavingsUiIntent.DepositFunds(150.0, "Bonus money"))
        advanceUntilIdle()

        var savingTxs = fakeRepo.transactionsMap.values.filter { it.type == "SAVING" }
        assertEquals(1, savingTxs.size)
        assertEquals(150.0, savingTxs.first().amount, 0.001)

        // Deposit another 100 (which exceeds remaining 50) -> should be capped at 50
        savingsViewModel.onIntent(SavingsUiIntent.DepositFunds(100.0, "Extra cash"))
        advanceUntilIdle()

        savingTxs = fakeRepo.transactionsMap.values.filter { it.type == "SAVING" }
        assertEquals(2, savingTxs.size)
        val totalSaved = savingTxs.sumOf { it.amount }
        assertEquals(200.0, totalSaved, 0.001)
    }

    @Test
    fun testDashboardReflectsSavingsContributionsAndDoesNotCountUnearnedGoal() = runTest {
        // Goal is 10,000 (money not yet earned/held)
        fakeRepo.setSavingsGoal("House Downpayment", 10000.0, 500.0)
        
        // Cash flow: Income 2000, Expense 500
        fakeRepo.addTransaction(TransactionEntity(amount = 2000.0, type = "INCOME", categoryId = "cat_salary"))
        fakeRepo.addTransaction(TransactionEntity(amount = 500.0, type = "EXPENSE", categoryId = "cat_general"))
        
        // Manual savings deposit: 300
        fakeRepo.addTransaction(TransactionEntity(amount = 300.0, type = "SAVING", categoryId = "cat_savings", note = "Savings Deposit"))

        val dashboardViewModel = DashboardViewModel(fakeRepo)
        advanceUntilIdle()

        val state = dashboardViewModel.uiState.value
        assertEquals(2000.0, state.totalIncome, 0.001)
        assertEquals(500.0, state.totalSpent, 0.001)
        assertEquals(300.0, state.totalSavings, 0.001)
        
        // The goal of 10,000 does NOT affect totalIncome or totalSpent
        assertTrue(state.totalIncome < 10000.0)

        // Net balance on header card: totalIncome - totalSpent + totalSavings = 2000 - 500 + 300 = 1800
        val netBalance = state.totalIncome - state.totalSpent + state.totalSavings
        assertEquals(1800.0, netBalance, 0.001)

        // Recent transactions on dashboard includes the savings transaction
        assertTrue(state.recentTransactions.any { it.type == "SAVING" && it.amount == 300.0 })
    }

    private class FakeSavingsRepo : SpentRepository {
        val transactionsMap = mutableMapOf<String, TransactionEntity>()
        private val txFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        private val _goalName = MutableStateFlow("")
        private val _goalTotal = MutableStateFlow(0.0)
        private val _monthly = MutableStateFlow(0.0)

        private fun refresh() {
            txFlow.value = transactionsMap.values.toList()
        }

        override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = txFlow.asStateFlow()
        override suspend fun addTransaction(transaction: TransactionEntity) {
            transactionsMap[transaction.id] = transaction
            refresh()
        }

        override suspend fun setSavingsGoal(name: String, totalGoal: Double, monthlyContribution: Double) {
            _goalName.value = name
            _goalTotal.value = totalGoal
            _monthly.value = monthlyContribution
        }

        override suspend fun clearSavingsGoal() {
            _goalName.value = ""
            _goalTotal.value = 0.0
            _monthly.value = 0.0
        }

        override val savingsGoalNameFlow: Flow<String> = _goalName.asStateFlow()
        override val savingsGoalTotalFlow: Flow<Double> = _goalTotal.asStateFlow()
        override val savingsMonthlyContributionFlow: Flow<Double> = _monthly.asStateFlow()
        override val currencySymbolFlow: Flow<String> = MutableStateFlow("$")
        override val isNetSavingsHiddenFlow: Flow<Boolean> = MutableStateFlow(false)

        override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
        override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = MutableStateFlow(null)
        override fun getUserAccountFlow(): Flow<UserAccountEntity?> = MutableStateFlow(null)
        override fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>> = MutableStateFlow(emptyList())
        override fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?> = MutableStateFlow(null)
        override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = MutableStateFlow(emptyList())
        override fun getLoansFlow(): Flow<List<LoanEntity>> = MutableStateFlow(emptyList())
        override val isWalkthroughCompletedFlow: Flow<Boolean> = MutableStateFlow(true)
        override val isDarkThemeFlow: Flow<Boolean?> = MutableStateFlow(null)
        override val appLanguageFlow: Flow<String?> = MutableStateFlow(null)
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
        override val imageStorageLocationFlow: Flow<String> = MutableStateFlow("IN_APP")
        override suspend fun setNetSavingsHidden(hidden: Boolean) {}
        override suspend fun connectGoogleDrive(account: GoogleSignInAccount): DriveConnectResult = DriveConnectResult.ConnectedNew
        override suspend fun resolveDriveConflict(account: GoogleSignInAccount, choice: com.app.spent.data.sync.SyncConflictChoice, cloudBackupJson: String): DriveConnectResult = DriveConnectResult.ConnectedNew
        override suspend fun cancelDriveConflict() {}
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
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun getTransactionById(id: String): TransactionEntity? = null
        override suspend fun deleteTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransactionById(id: String) {}
        override suspend fun addCategory(category: CategoryEntity) {}
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategoryById(id: String) {}
        override suspend fun setPayCycle(payCycle: PayCycleEntity) {}
        override suspend fun addRecurringRule(rule: RecurringRuleEntity) {}
        override suspend fun updateRecurringRule(rule: RecurringRuleEntity) {}
        override suspend fun stopRecurringRule(id: String) {}
        override suspend fun deleteRecurringRuleById(id: String) {}
        override suspend fun deleteRecurringRuleAndTransactions(id: String) {}
        override suspend fun addLoan(loan: LoanEntity) {}
        override suspend fun updateLoan(loan: LoanEntity) {}
        override suspend fun deleteLoanById(id: String) {}
        override suspend fun getLoanById(id: String): LoanEntity? = null
        override suspend fun recordLoanPayment(loanId: String, amount: Double) {}
        override suspend fun executePendingRecurringRules() {}
        override suspend fun seedStarterDataIfEmpty() {}
        override suspend fun setWalkthroughCompleted(completed: Boolean) {}
        override suspend fun setDarkThemeMode(enabled: Boolean?) {}
        override suspend fun setCurrencySymbol(symbol: String) {}
        override suspend fun setAppLanguage(languageCode: String?) {}
        override suspend fun setImageStorageLocation(location: String) {}
        override suspend fun setLastDriveSyncTimestamp(timestamp: Long) {}
        override suspend fun restoreAllData(categories: List<CategoryEntity>, transactions: List<TransactionEntity>, payCycle: PayCycleEntity?, recurringRules: List<RecurringRuleEntity>, userAccount: UserAccountEntity?, loans: List<LoanEntity>) {}
        override suspend fun resetAllData(deleteDriveImages: Boolean) {}
    }
}
