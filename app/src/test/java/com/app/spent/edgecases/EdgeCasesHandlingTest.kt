package com.app.spent.edgecases

import android.content.ContextWrapper
import com.app.spent.data.local.dao.AssetDao
import com.app.spent.data.local.dao.SpentDao
import com.app.spent.data.local.entity.AssetHoldingEntity
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.data.local.entity.MarketPriceEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import com.app.spent.data.preferences.UserPreferencesRepository
import com.app.spent.data.repository.AssetRepositoryImpl
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.repository.SpentRepositoryImpl
import com.app.spent.data.sync.DriveBackupManager
import com.app.spent.domain.asset.MarketPriceProvider
import com.app.spent.domain.asset.model.MarketQuote
import com.app.spent.util.ImageUriResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeCasesHandlingTest {

    // --- Edge Case 3: Safe Category Deletion Preventing Transaction Wipes ---
    @Test
    fun deleteCategoryById_reassignsTransactionsAndRulesToGeneral() = runBlocking {
        val mockContext = ContextWrapper(null)
        val mockPrefs = UserPreferencesRepository(mockContext)
        val fakeDao = FakeEdgeCasesSpentDao()

        // Setup custom category and transactions
        val customCatId = "cat_custom_electronics"
        fakeDao.insertCategory(CategoryEntity(id = customCatId, name = "Electronics", iconName = "tech", colorHex = "#000"))
        fakeDao.insertTransaction(
            TransactionEntity(id = "tx1", categoryId = customCatId, amount = 150.0, type = "EXPENSE")
        )
        fakeDao.insertRecurringRule(
            RecurringRuleEntity(id = "rr1", categoryId = customCatId, amount = 50.0, frequency = "MONTHLY")
        )

        val repository = SpentRepositoryImpl(
            context = mockContext,
            dao = fakeDao,
            preferencesRepository = mockPrefs
        )

        // Delete custom category
        repository.deleteCategoryById(customCatId)

        // Verify transactions and recurring rules were reassigned to cat_general instead of deleted
        val tx = fakeDao.getTransactionById("tx1")
        assertNotNull(tx)
        assertEquals("cat_general", tx?.categoryId)

        val rules = fakeDao.getAllRecurringRules()
        assertEquals(1, rules.size)
        assertEquals("cat_general", rules[0].categoryId)

        // Verify cat_general cannot be deleted
        fakeDao.insertCategory(CategoryEntity(id = "cat_general", name = "General", iconName = "general", colorHex = "#000"))
        repository.deleteCategoryById("cat_general")
        assertTrue(fakeDao.categories.any { it.id == "cat_general" })
    }

    // --- Edge Case 4: Math Division by Zero in AssetRepository ---
    @Test
    fun assetRepository_calculatesSummaryWithoutNaNOrInfinityWhenPriceDrops100Percent() = runBlocking {
        val fakeAssetDao = FakeEdgeCasesAssetDao()
        val holding = AssetHoldingEntity(
            id = "h1",
            symbol = "FAILCOIN",
            name = "Fail Coin",
            type = "CRYPTO",
            quantity = 10.0,
            averageBuyPrice = 100.0,
            currency = "USD"
        )
        fakeAssetDao.insertOrUpdateHolding(holding)

        val priceProvider = object : MarketPriceProvider {
            override suspend fun getQuote(symbol: String, baseCurrency: String): MarketQuote {
                // Extreme -100% 24h change
                return MarketQuote(
                    symbol = symbol,
                    priceInBaseCurrency = 0.0,
                    change24hPct = -100.0
                )
            }
            override suspend fun getQuotes(symbols: List<String>, baseCurrency: String): Map<String, MarketQuote> {
                return symbols.associateWith { getQuote(it, baseCurrency) }
            }
        }

        val assetRepo = AssetRepositoryImpl(
            assetDao = fakeAssetDao,
            priceProvider = priceProvider
        )

        assetRepo.refreshPrices("USD")
        val summary = assetRepo.getPortfolioSummaryFlow().first()

        // Verify no NaN or Infinite values
        assertFalse(summary.totalValuation.isNaN())
        assertFalse(summary.totalValuation.isInfinite())
        assertFalse(summary.totalPnLPct.isNaN())
        assertFalse(summary.totalPnLPct.isInfinite())
        assertFalse(summary.totalDayChangeAmount.isNaN())
        assertFalse(summary.totalDayChangeAmount.isInfinite())
        assertFalse(summary.totalDayChangePct.isNaN())
        assertFalse(summary.totalDayChangePct.isInfinite())
        assertEquals(0.0, summary.totalValuation, 0.001)
    }

    // --- Edge Case 2: Variable Assets Cloud Backup and Restore ---
    @Test
    fun driveBackupManager_serializesAndParsesAssetHoldings() = runBlocking {
        val testHolding = AssetHoldingEntity(
            id = "asset_btc_1",
            symbol = "BTC",
            name = "Bitcoin",
            type = "CRYPTO",
            quantity = 1.25,
            averageBuyPrice = 60000.0,
            currency = "USD",
            notes = "Cold storage"
        )

        val testRepo = object : FakeEdgeCasesSpentRepo() {
            override fun getAssetHoldingsFlow(): Flow<List<AssetHoldingEntity>> {
                return MutableStateFlow(listOf(testHolding))
            }
        }

        val json = DriveBackupManager.generateBackupJson(testRepo)
        assertTrue(json.contains("\"assetHoldings\""))
        assertTrue(json.contains("BTC"))
        assertTrue(json.contains("60000"))

        val parsed = DriveBackupManager.parseBackupJson(org.json.JSONObject(json))
        assertEquals(1, parsed.assetHoldings.size)
        assertEquals("BTC", parsed.assetHoldings[0].symbol)
        assertEquals(1.25, parsed.assetHoldings[0].quantity, 0.001)
        assertEquals(60000.0, parsed.assetHoldings[0].averageBuyPrice ?: 0.0, 0.001)
        assertEquals("Cold storage", parsed.assetHoldings[0].notes)
    }

    // --- Edge Case 6: Missing Local Receipt Images Fallback ---
    @Test
    fun imageUriResolver_returnsNullForNonExistentLocalFilePath() {
        val mockContext = ContextWrapper(null)

        // Non-existent file path
        val missingFilePath = "file:///data/user/0/com.app.spent/files/receipt_images/spent_receipt_non_existent.jpg"
        val resolved = ImageUriResolver.resolve(mockContext, missingFilePath)
        assertNull(resolved)

        // Web URL should still be resolved to String
        val webUrl = "https://lh3.googleusercontent.com/d/12345"
        val resolvedWeb = ImageUriResolver.resolve(mockContext, webUrl)
        assertEquals(webUrl, resolvedWeb)
    }

    // --- Helper Fake DAOs and Repositories ---

    private class FakeEdgeCasesSpentDao : SpentDao {
        val categories = mutableListOf<CategoryEntity>()
        val transactions = mutableListOf<TransactionEntity>()
        val recurringRules = mutableListOf<RecurringRuleEntity>()

        override suspend fun insertCategory(category: CategoryEntity) {
            categories.removeAll { it.id == category.id }
            categories.add(category)
        }
        override suspend fun insertCategories(categories: List<CategoryEntity>) {
            this.categories.addAll(categories)
        }
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategoryById(id: String) {
            categories.removeAll { it.id == id }
        }
        override suspend fun reassignTransactionsToGeneral(categoryId: String) {
            for (i in transactions.indices) {
                if (transactions[i].categoryId == categoryId) {
                    transactions[i] = transactions[i].copy(categoryId = "cat_general")
                }
            }
        }
        override suspend fun reassignRecurringRulesToGeneral(categoryId: String) {
            for (i in recurringRules.indices) {
                if (recurringRules[i].categoryId == categoryId) {
                    recurringRules[i] = recurringRules[i].copy(categoryId = "cat_general")
                }
            }
        }
        override suspend fun deleteAllCategories() { categories.clear() }
        override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = MutableStateFlow(categories)
        override suspend fun getCategoryById(id: String): CategoryEntity? = categories.find { it.id == id }

        override suspend fun insertTransaction(transaction: TransactionEntity) { transactions.add(transaction) }
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) { this.transactions.addAll(transactions) }
        override suspend fun getTransactionById(id: String): TransactionEntity? = transactions.find { it.id == id }
        override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = MutableStateFlow(transactions)
        override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = MutableStateFlow(transactions.take(limit))
        override fun getTransactionsSinceFlow(startTime: Long): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun getTotalAmountByTypeFlow(type: String): Flow<Double> = MutableStateFlow(0.0)
        override fun getTotalAmountByTypeBetweenFlow(type: String, startTime: Long, endTime: Long): Flow<Double> = MutableStateFlow(0.0)
        override fun getCategorySpentBetweenFlow(categoryId: String, startTime: Long, endTime: Long): Flow<Double> = MutableStateFlow(0.0)
        override fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override suspend fun deleteTransaction(transaction: TransactionEntity) { transactions.remove(transaction) }
        override suspend fun deleteTransactionById(id: String) { transactions.removeAll { it.id == id } }
        override suspend fun deleteAllTransactions() { transactions.clear() }

        override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = MutableStateFlow(recurringRules)
        override suspend fun getAllRecurringRules(): List<RecurringRuleEntity> = recurringRules
        override suspend fun getActiveRecurringRules(): List<RecurringRuleEntity> = recurringRules.filter { it.isActive }
        override suspend fun insertRecurringRule(rule: RecurringRuleEntity) { recurringRules.add(rule) }
        override suspend fun insertRecurringRules(rules: List<RecurringRuleEntity>) { recurringRules.addAll(rules) }
        override suspend fun updateRecurringRule(rule: RecurringRuleEntity) {
            recurringRules.removeAll { it.id == rule.id }
            recurringRules.add(rule)
        }
        override suspend fun updateRecurringRuleActiveStatus(id: String, isActive: Boolean) {}
        override suspend fun deleteRecurringRuleById(id: String) { recurringRules.removeAll { it.id == id } }
        override suspend fun deleteAllRecurringRules() { recurringRules.clear() }
        override suspend fun getTransactionCountForRecurringRule(ruleId: String): Int = 0
        override suspend fun getTransactionsByRecurringRuleId(ruleId: String): List<TransactionEntity> = emptyList()
        override suspend fun updateTransactionsMetadataForRecurringRule(ruleId: String, note: String, categoryId: String, type: String) {}
        override suspend fun updateTransactionsForRecurringRule(ruleId: String, note: String, categoryId: String, type: String, amount: Double) {}
        override suspend fun deleteTransactionsByRecurringRuleId(ruleId: String) {}

        override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = MutableStateFlow(null)
        override suspend fun getCurrentPayCycle(): PayCycleEntity? = null
        override suspend fun insertPayCycle(payCycle: PayCycleEntity) {}
        override suspend fun deleteAllPayCycles() {}

        override fun getUserAccountFlow() = MutableStateFlow(null)
        override suspend fun insertOrUpdateUserAccount(account: com.app.spent.data.local.entity.UserAccountEntity) {}
        override fun getFamilyMembersFlow() = MutableStateFlow(emptyList<com.app.spent.data.local.entity.FamilyMemberEntity>())
        override suspend fun insertFamilyMember(member: com.app.spent.data.local.entity.FamilyMemberEntity) {}
        override fun getParentalConfigFlow() = MutableStateFlow(null)
        override suspend fun insertParentalConfig(config: com.app.spent.data.local.entity.ParentalControlConfigEntity) {}

        override fun getLoansFlow(): Flow<List<LoanEntity>> = MutableStateFlow(emptyList())
        override suspend fun getLoanById(id: String): LoanEntity? = null
        override suspend fun insertLoan(loan: LoanEntity) {}
        override suspend fun insertLoans(loans: List<LoanEntity>) {}
        override suspend fun updateLoan(loan: LoanEntity) {}
        override suspend fun deleteLoanById(id: String) {}
        override suspend fun deleteAllLoans() {}
    }

    private class FakeEdgeCasesAssetDao : AssetDao {
        val holdings = mutableMapOf<String, AssetHoldingEntity>()
        val prices = mutableMapOf<String, MarketPriceEntity>()
        val holdingsFlow = MutableStateFlow<List<AssetHoldingEntity>>(emptyList())
        val pricesFlow = MutableStateFlow<List<MarketPriceEntity>>(emptyList())

        fun refresh() {
            holdingsFlow.value = holdings.values.toList()
            pricesFlow.value = prices.values.toList()
        }

        override fun getHoldingsFlow(): Flow<List<AssetHoldingEntity>> = holdingsFlow
        override suspend fun getHoldingById(id: String): AssetHoldingEntity? = holdings[id]
        override suspend fun getAllSymbols(): List<String> = holdings.values.map { it.symbol }.distinct()
        override suspend fun insertOrUpdateHolding(holding: AssetHoldingEntity) {
            holdings[holding.id] = holding
            refresh()
        }
        override suspend fun insertHoldings(holdings: List<AssetHoldingEntity>) {
            holdings.forEach { this.holdings[it.id] = it }
            refresh()
        }
        override suspend fun updateHolding(holding: AssetHoldingEntity) {
            holdings[holding.id] = holding
            refresh()
        }
        override suspend fun deleteHoldingById(id: String) {
            holdings.remove(id)
            refresh()
        }
        override suspend fun deleteAllHoldings() {
            holdings.clear()
            refresh()
        }
        override fun getMarketPricesFlow(): Flow<List<MarketPriceEntity>> = pricesFlow
        override suspend fun getMarketPrice(symbol: String): MarketPriceEntity? = prices[symbol.uppercase()]
        override suspend fun insertOrUpdatePrices(prices: List<MarketPriceEntity>) {
            prices.forEach { this.prices[it.symbol.uppercase()] = it }
            refresh()
        }
        override suspend fun insertOrUpdatePrice(price: MarketPriceEntity) {
            prices[price.symbol.uppercase()] = price
            refresh()
        }
        override suspend fun deletePrice(symbol: String) {
            prices.remove(symbol.uppercase())
            refresh()
        }
        override suspend fun deleteAllPrices() {
            prices.clear()
            refresh()
        }
    }

    private open class FakeEdgeCasesSpentRepo : SpentRepository {
        override val isWalkthroughCompletedFlow = MutableStateFlow(true)
        override val isDarkThemeFlow = MutableStateFlow<Boolean?>(null)
        override val currencySymbolFlow = MutableStateFlow("$")
        override val appLanguageFlow = MutableStateFlow<String?>(null)
        override val savingsGoalNameFlow = MutableStateFlow("")
        override val savingsGoalTotalFlow = MutableStateFlow(0.0)
        override val savingsMonthlyContributionFlow = MutableStateFlow(0.0)
        override val lastDriveSyncTimestampFlow = MutableStateFlow(0L)
        override val isDriveConnectedFlow = MutableStateFlow(false)
        override val driveAccountEmailFlow = MutableStateFlow<String?>(null)
        override val isSyncingDriveFlow = MutableStateFlow(false)
        override val partnerDriveFileIdFlow = MutableStateFlow<String?>(null)
        override val partnerNameFlow = MutableStateFlow<String?>(null)
        override val partnerEmailFlow = MutableStateFlow<String?>(null)
        override val partnerLastSyncTimestampFlow = MutableStateFlow(0L)
        override val isPartnerPairedFlow = MutableStateFlow(false)
        override val sharedMembersFlow = MutableStateFlow(emptyList<com.app.spent.data.sync.SharedMemberInfo>())
        override val imageStorageLocationFlow = MutableStateFlow("DEVICE")
        override val isNetSavingsHiddenFlow = MutableStateFlow(false)

        override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
        override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = MutableStateFlow(null)
        override fun getUserAccountFlow() = MutableStateFlow(null)
        override fun getFamilyMembersFlow() = MutableStateFlow(emptyList<com.app.spent.data.local.entity.FamilyMemberEntity>())
        override fun getParentalConfigFlow() = MutableStateFlow(null)
        override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = MutableStateFlow(emptyList())
        override fun getLoansFlow(): Flow<List<LoanEntity>> = MutableStateFlow(emptyList())

        override suspend fun connectGoogleDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) = com.app.spent.data.sync.DriveConnectResult.ConnectedNew
        override suspend fun resolveDriveConflict(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, choice: com.app.spent.data.sync.SyncConflictChoice, cloudBackupJson: String) = com.app.spent.data.sync.DriveConnectResult.ConnectedNew
        override suspend fun cancelDriveConflict() {}
        override suspend fun disconnectGoogleDrive() {}
        override suspend fun syncToGoogleDrive() = Result.success(true)
        override fun triggerAutoSync() {}
        override suspend fun getOwnBackupFileId() = Result.success(null)
        override suspend fun enablePublicLinkSharing(fileId: String) = Result.success("")
        override suspend fun processAndSaveImage(sourceUri: android.net.Uri, destinationType: String) = Result.success("")
        override suspend fun addOrUpdateSharedMember(member: com.app.spent.data.sync.SharedMemberInfo) {}
        override suspend fun updateSharedMemberName(fileId: String, newName: String) {}
        override suspend fun updateUserProfileName(newName: String) {}
        override suspend fun removeSharedMember(fileId: String) {}
        override suspend fun clearSharedMembers() {}
        override suspend fun savePartnerInfo(fileId: String, name: String, email: String?) {}
        override suspend fun setPartnerLastSyncTimestamp(timestamp: Long) {}
        override suspend fun clearPartnerInfo() {}
        override suspend fun getTransactionById(id: String): TransactionEntity? = null
        override suspend fun addTransaction(transaction: TransactionEntity) {}
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
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
        override suspend fun setImageStorageLocation(location: String) {}
        override suspend fun setSavingsGoal(name: String, totalGoal: Double, monthlyContribution: Double) {}
        override suspend fun clearSavingsGoal() {}
        override suspend fun setNetSavingsHidden(hidden: Boolean) {}
        override suspend fun setLastDriveSyncTimestamp(timestamp: Long) {}
        override suspend fun restoreAllData(categories: List<CategoryEntity>, transactions: List<TransactionEntity>, payCycle: PayCycleEntity?, recurringRules: List<RecurringRuleEntity>, userAccount: UserAccountEntity?, loans: List<LoanEntity>) {}
        override suspend fun resetAllData(deleteDriveImages: Boolean) {}
    }
}
