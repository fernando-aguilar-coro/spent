package com.app.spent.dashboard

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
import com.app.spent.ui.analytics.AnalyticsUiIntent
import com.app.spent.ui.analytics.AnalyticsViewModel
import com.app.spent.ui.dashboard.DashboardUiIntent
import com.app.spent.ui.dashboard.DashboardViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetSavingsVisibilityTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeNetSavingsRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeNetSavingsRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDashboardToggleNetSavingsVisibilityUpdatesStateAndRepository() = runTest {
        val viewModel = DashboardViewModel(fakeRepo)
        advanceUntilIdle()

        // Initial state should not be hidden
        assertFalse(viewModel.uiState.value.isNetSavingsHidden)
        assertFalse(fakeRepo.isNetSavingsHiddenFlow.value)

        // Toggle to hidden
        viewModel.onIntent(DashboardUiIntent.ToggleNetSavingsVisibility)
        advanceUntilIdle()

        assertTrue(fakeRepo.isNetSavingsHiddenFlow.value)
        assertTrue(viewModel.uiState.value.isNetSavingsHidden)

        // Toggle back to visible
        viewModel.onIntent(DashboardUiIntent.ToggleNetSavingsVisibility)
        advanceUntilIdle()

        assertFalse(fakeRepo.isNetSavingsHiddenFlow.value)
        assertFalse(viewModel.uiState.value.isNetSavingsHidden)
    }

    @Test
    fun testAnalyticsToggleNetSavingsVisibilityUpdatesStateAndRepository() = runTest {
        val viewModel = AnalyticsViewModel(fakeRepo)
        advanceUntilIdle()

        // Initial state should not be hidden
        assertFalse(viewModel.uiState.value.isNetSavingsHidden)

        // Toggle to hidden
        viewModel.onIntent(AnalyticsUiIntent.ToggleNetSavingsVisibility)
        advanceUntilIdle()

        assertTrue(fakeRepo.isNetSavingsHiddenFlow.value)
        assertTrue(viewModel.uiState.value.isNetSavingsHidden)

        // Toggle back
        viewModel.onIntent(AnalyticsUiIntent.ToggleNetSavingsVisibility)
        advanceUntilIdle()

        assertFalse(fakeRepo.isNetSavingsHiddenFlow.value)
        assertFalse(viewModel.uiState.value.isNetSavingsHidden)
    }

    private class FakeNetSavingsRepo : SpentRepository {
        val netSavingsHiddenFlow = MutableStateFlow(false)

        override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
        override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = MutableStateFlow(null)
        override fun getUserAccountFlow(): Flow<UserAccountEntity?> = MutableStateFlow(null)
        override fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>> = MutableStateFlow(emptyList())
        override fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?> = MutableStateFlow(null)
        override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = MutableStateFlow(emptyList())
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
        override val imageStorageLocationFlow: Flow<String> = MutableStateFlow("IN_APP")
        override val isNetSavingsHiddenFlow: MutableStateFlow<Boolean> get() = netSavingsHiddenFlow

        override suspend fun setNetSavingsHidden(hidden: Boolean) {
            netSavingsHiddenFlow.value = hidden
        }

        override suspend fun connectGoogleDrive(account: GoogleSignInAccount): DriveConnectResult = DriveConnectResult.ConnectedNew
        override suspend fun resolveDriveConflict(
            account: GoogleSignInAccount,
            choice: com.app.spent.data.sync.SyncConflictChoice,
            cloudBackupJson: String
        ): DriveConnectResult = DriveConnectResult.ConnectedNew
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
        override suspend fun addTransaction(transaction: TransactionEntity) {}
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
}
