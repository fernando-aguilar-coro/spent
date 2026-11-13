package com.app.spent.dashboard

import com.app.spent.data.local.entity.AssetHoldingEntity
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.FamilyMemberEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.data.local.entity.MarketPriceEntity
import com.app.spent.data.local.entity.ParentalControlConfigEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import com.app.spent.data.repository.AssetRepository
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.sync.DriveConnectResult
import com.app.spent.data.sync.SharedMemberInfo
import com.app.spent.domain.asset.model.MarketQuote
import com.app.spent.domain.asset.model.PortfolioSummary
import com.app.spent.domain.asset.model.ValuedAsset
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardVariableAssetsIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSpentRepo: FakeSpentRepo
    private lateinit var fakeAssetRepo: FakeAssetRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSpentRepo = FakeSpentRepo()
        fakeAssetRepo = FakeAssetRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDashboardIncorporatesMultiCurrencyHoldingsIntoTotalBalance() = runTest {
        // Transactions: +$3,000 Income, -$1,000 Expense -> Net from ledger = +$2,000
        fakeSpentRepo.transactionsFlow.value = listOf(
            TransactionEntity(id = "tx1", amount = 3000.0, type = "INCOME", categoryId = "cat_salary", note = "Salary"),
            TransactionEntity(id = "tx2", amount = 1000.0, type = "EXPENSE", categoryId = "cat_general", note = "Rent")
        )

        // Asset holdings:
        // 1. 500 EUR (at 1.085 USD/EUR = $542.50 USD)
        // 2. 10 AAPL (at $225.00 USD = $2,250.00 USD)
        // Total variable asset valuation = $2,792.50
        val eurHolding = AssetHoldingEntity(id = "h_eur", symbol = "EUR", name = "Euro Cash", type = "FIAT", quantity = 500.0)
        val aaplHolding = AssetHoldingEntity(id = "h_aapl", symbol = "AAPL", name = "Apple", type = "STOCK", quantity = 10.0)

        fakeAssetRepo.setPortfolio(
            PortfolioSummary(
                totalValuation = 2792.50,
                totalCostBasis = 2500.0,
                totalUnrealizedPnL = 292.50,
                totalPnLPct = 11.7,
                totalDayChangeAmount = 30.0,
                totalDayChangePct = 1.08,
                assets = listOf(
                    ValuedAsset(eurHolding, 1.085, 542.50, 500.0, 42.50, 8.5, 0.0, false),
                    ValuedAsset(aaplHolding, 225.0, 2250.0, 2000.0, 250.0, 12.5, 1.2, false)
                )
            )
        )

        val viewModel = DashboardViewModel(
            repository = fakeSpentRepo,
            assetRepository = fakeAssetRepo
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3000.0, state.totalIncome, 0.001)
        assertEquals(1000.0, state.totalSpent, 0.001)

        // Variable assets valuation in state
        assertEquals(2792.50, state.totalVariableAssetsValuation, 0.001)
        assertEquals(2, state.variableAssetsCount)

        // Total Net Balance = (3000 - 1000 + 0) + 2792.50 = 4,792.50
        val expectedNet = (state.totalIncome - state.totalSpent + state.totalSavings) + state.totalVariableAssetsValuation
        assertEquals(4792.50, expectedNet, 0.001)
    }

    private class FakeAssetRepo : AssetRepository {
        private val summaryFlow = MutableStateFlow(
            PortfolioSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList())
        )

        fun setPortfolio(summary: PortfolioSummary) {
            summaryFlow.value = summary
        }

        override fun getHoldingsFlow(): Flow<List<AssetHoldingEntity>> = MutableStateFlow(emptyList())
        override fun getMarketPricesFlow(): Flow<List<MarketPriceEntity>> = MutableStateFlow(emptyList())
        override fun getPortfolioSummaryFlow(baseCurrency: String): Flow<PortfolioSummary> = summaryFlow.asStateFlow()
        override suspend fun getQuote(symbol: String, baseCurrency: String): MarketQuote? = null
        override suspend fun refreshPrices(baseCurrency: String): Result<Unit> = Result.success(Unit)
        override suspend fun simulateMarketTick(baseCurrency: String): Result<Unit> = Result.success(Unit)
        override suspend fun addAsset(holding: AssetHoldingEntity) {}
        override suspend fun updateAssetQuantity(id: String, newQuantity: Double) {}
        override suspend fun setManualPriceOverride(id: String, overridePrice: Double?, isManual: Boolean) {}
        override suspend fun deleteAsset(id: String) {}
    }

    private class FakeSpentRepo : SpentRepository {
        val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())
        val payCycleFlow = MutableStateFlow<PayCycleEntity?>(null)

        override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = transactionsFlow.asStateFlow()
        override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = transactionsFlow.asStateFlow()
        override fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>> = transactionsFlow.asStateFlow()
        override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = categoriesFlow.asStateFlow()
        override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = payCycleFlow.asStateFlow()
        override fun getUserAccountFlow(): Flow<UserAccountEntity?> = MutableStateFlow(null)
        override fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>> = MutableStateFlow(emptyList())
        override fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?> = MutableStateFlow(null)
        override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = MutableStateFlow(emptyList())
        override fun getLoansFlow(): Flow<List<LoanEntity>> = MutableStateFlow(emptyList())

        override val isWalkthroughCompletedFlow: Flow<Boolean> = MutableStateFlow(true)
        override val isDarkThemeFlow: Flow<Boolean?> = MutableStateFlow(null)
        override val currencySymbolFlow: Flow<String> = MutableStateFlow("$")
        override val appLanguageFlow: Flow<String?> = MutableStateFlow("en")
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
        override val imageStorageLocationFlow: Flow<String> = MutableStateFlow("LOCAL")
        override val isNetSavingsHiddenFlow: Flow<Boolean> = MutableStateFlow(false)

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
        override suspend fun updateRecurringRuleAndSyncTransactions(rule: RecurringRuleEntity, syncAmount: Boolean) {}
        override suspend fun getTransactionCountForRecurringRule(ruleId: String): Int = 0
        override suspend fun getTransactionsByRecurringRuleId(ruleId: String): List<TransactionEntity> = emptyList()
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
        override suspend fun setSavingsGoal(name: String, totalGoal: Double, monthlyContribution: Double) {}
        override suspend fun clearSavingsGoal() {}
        override suspend fun setImageStorageLocation(location: String) {}
        override suspend fun setNetSavingsHidden(hidden: Boolean) {}
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

