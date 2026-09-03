package com.app.spent.sync

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.FamilyMemberEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.data.local.entity.ParentalControlConfigEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.sync.DriveBackupManager
import com.app.spent.data.sync.DriveConnectResult
import com.app.spent.data.sync.SharedMemberInfo
import com.app.spent.data.sync.SyncConflictChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSyncConflictTest {

    @Test
    fun testHasCloudUserData_emptyOrBlank() {
        assertFalse(DriveBackupManager.hasCloudUserData(""))
        assertFalse(DriveBackupManager.hasCloudUserData("   "))
        assertFalse(DriveBackupManager.hasCloudUserData("{}"))
    }

    @Test
    fun testHasCloudUserData_onlyDefaultCategories() {
        val root = JSONObject().apply {
            put("app", "Spent")
            put("version", 2)
            val cats = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "cat_general")
                    put("name", "General")
                    put("budgetAmount", 0.0)
                })
            }
            put("categories", cats)
            put("transactions", JSONArray())
            put("loans", JSONArray())
        }
        assertFalse(DriveBackupManager.hasCloudUserData(root.toString()))
    }

    @Test
    fun testHasCloudUserData_withTransactions() {
        val root = JSONObject().apply {
            put("app", "Spent")
            put("version", 2)
            val txs = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "tx_1")
                    put("amount", 25.50)
                    put("type", "EXPENSE")
                    put("categoryId", "cat_general")
                    put("timestamp", 1000L)
                })
            }
            put("transactions", txs)
        }
        assertTrue(DriveBackupManager.hasCloudUserData(root.toString()))
    }

    @Test
    fun testHasCloudUserData_withLoans() {
        val root = JSONObject().apply {
            put("app", "Spent")
            val loans = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "loan_1")
                    put("counterpartyName", "Alice")
                    put("principalAmount", 500.0)
                })
            }
            put("loans", loans)
        }
        assertTrue(DriveBackupManager.hasCloudUserData(root.toString()))
    }

    @Test
    fun testExtractCloudSummary() {
        val root = JSONObject().apply {
            put("exportTimestamp", 1700000000L)
            val txs = JSONArray().apply {
                put(JSONObject().apply { put("id", "tx_1"); put("amount", 10.0) })
                put(JSONObject().apply { put("id", "tx_2"); put("amount", 20.0) })
            }
            put("transactions", txs)
            val loans = JSONArray().apply {
                put(JSONObject().apply { put("id", "loan_1"); put("principalAmount", 100.0) })
            }
            put("loans", loans)
            val cats = JSONArray().apply {
                put(JSONObject().apply { put("id", "cat_1") })
            }
            put("categories", cats)
        }

        val summary = DriveBackupManager.extractCloudSummary(root.toString())
        assertEquals(2, summary.transactionCount)
        assertEquals(1, summary.loanCount)
        assertEquals(1, summary.categoryCount)
        assertEquals(1700000000L, summary.lastModifiedTimestamp)
    }

    @Test
    fun testHasLocalUserData_and_mergeCloudAndLocal() = runBlocking {
        val fakeRepo = FakeSpentRepository()

        // Initially no user data (only default categories)
        fakeRepo.categories = listOf(
            CategoryEntity(id = "cat_general", name = "General", iconName = "Category", colorHex = "#64748B", budgetAmount = 0.0, displayOrder = 0)
        )
        assertFalse(DriveBackupManager.hasLocalUserData(fakeRepo))

        // Add a local transaction
        fakeRepo.transactions = listOf(
            TransactionEntity(id = "local_tx_1", amount = 15.0, type = "EXPENSE", categoryId = "cat_general", timestamp = 2000L)
        )
        assertTrue(DriveBackupManager.hasLocalUserData(fakeRepo))

        // Cloud JSON has a different transaction
        val cloudJson = JSONObject().apply {
            val txs = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "cloud_tx_1")
                    put("amount", 40.0)
                    put("type", "INCOME")
                    put("categoryId", "cat_general")
                    put("timestamp", 3000L)
                })
            }
            put("transactions", txs)
            val cats = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "cat_general")
                    put("name", "General")
                    put("budgetAmount", 0.0)
                })
            }
            put("categories", cats)
        }.toString()

        // Execute merge
        val mergeResult = DriveBackupManager.mergeCloudAndLocal(cloudJson, fakeRepo)
        assertTrue(mergeResult.isSuccess)

        // After merge, both local and cloud transactions must be present
        val mergedIds = fakeRepo.transactions.map { it.id }.toSet()
        assertTrue(mergedIds.contains("local_tx_1"))
        assertTrue(mergedIds.contains("cloud_tx_1"))
        assertEquals(2, fakeRepo.transactions.size)
    }

    private class FakeSpentRepository : SpentRepository {
        var transactions: List<TransactionEntity> = emptyList()
        var categories: List<CategoryEntity> = emptyList()
        var loans: List<LoanEntity> = emptyList()
        var payCycle: PayCycleEntity? = null
        var recurringRules: List<RecurringRuleEntity> = emptyList()
        var userAccount: UserAccountEntity? = null
        var savingsGoalName: String = ""
        var savingsGoalTotal: Double = 0.0
        var savingsMonthlyContribution: Double = 0.0

        override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = flowOf(transactions)
        override fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>> = flowOf(transactions)
        override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = flowOf(categories)
        override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = flowOf(payCycle)
        override fun getUserAccountFlow(): Flow<UserAccountEntity?> = flowOf(userAccount)
        override fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>> = flowOf(emptyList())
        override fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?> = flowOf(null)
        override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = flowOf(recurringRules)
        override fun getLoansFlow(): Flow<List<LoanEntity>> = flowOf(loans)

        override val isWalkthroughCompletedFlow: Flow<Boolean> = flowOf(true)
        override val isDarkThemeFlow: Flow<Boolean?> = flowOf(false)
        override val currencySymbolFlow: Flow<String> = flowOf("$")
        override val appLanguageFlow: Flow<String?> = flowOf("en")
        override val savingsGoalNameFlow: Flow<String> = flowOf(savingsGoalName)
        override val savingsGoalTotalFlow: Flow<Double> = flowOf(savingsGoalTotal)
        override val savingsMonthlyContributionFlow: Flow<Double> = flowOf(savingsMonthlyContribution)
        override val lastDriveSyncTimestampFlow: Flow<Long> = flowOf(0L)
        override val isDriveConnectedFlow: Flow<Boolean> = flowOf(false)
        override val driveAccountEmailFlow: Flow<String?> = flowOf(null)
        override val isSyncingDriveFlow: Flow<Boolean> = flowOf(false)
        override val partnerDriveFileIdFlow: Flow<String?> = flowOf(null)
        override val partnerNameFlow: Flow<String?> = flowOf(null)
        override val partnerEmailFlow: Flow<String?> = flowOf(null)
        override val partnerLastSyncTimestampFlow: Flow<Long> = flowOf(0L)
        override val isPartnerPairedFlow: Flow<Boolean> = flowOf(false)
        override val sharedMembersFlow: Flow<List<SharedMemberInfo>> = flowOf(emptyList())
        override val imageStorageLocationFlow: Flow<String> = flowOf("IN_APP")

        override suspend fun connectGoogleDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount): DriveConnectResult = DriveConnectResult.ConnectedNew
        override suspend fun resolveDriveConflict(
            account: com.google.android.gms.auth.api.signin.GoogleSignInAccount,
            choice: SyncConflictChoice,
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
        override suspend fun getTransactionById(id: String): TransactionEntity? = transactions.find { it.id == id }
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
        override suspend fun addLoan(loan: LoanEntity) {}
        override suspend fun updateLoan(loan: LoanEntity) {}
        override suspend fun deleteLoanById(id: String) {}
        override suspend fun recordLoanPayment(loanId: String, amount: Double) {}
        override suspend fun getLoanById(id: String): LoanEntity? = loans.find { it.id == id }
        override suspend fun seedStarterDataIfEmpty() {}
        override suspend fun setWalkthroughCompleted(completed: Boolean) {}
        override suspend fun setDarkThemeMode(enabled: Boolean?) {}
        override suspend fun setCurrencySymbol(symbol: String) {}
        override suspend fun setAppLanguage(languageCode: String?) {}
        override suspend fun setImageStorageLocation(location: String) {}
        override suspend fun setSavingsGoal(name: String, totalGoal: Double, monthlyContribution: Double) {
            savingsGoalName = name
            savingsGoalTotal = totalGoal
            savingsMonthlyContribution = monthlyContribution
        }
        override suspend fun clearSavingsGoal() {}
        override suspend fun setLastDriveSyncTimestamp(timestamp: Long) {}
        override suspend fun restoreAllData(
            categories: List<CategoryEntity>,
            transactions: List<TransactionEntity>,
            payCycle: PayCycleEntity?,
            recurringRules: List<RecurringRuleEntity>,
            userAccount: UserAccountEntity?,
            loans: List<LoanEntity>
        ) {
            this.categories = categories
            this.transactions = transactions
            this.payCycle = payCycle
            this.recurringRules = recurringRules
            this.userAccount = userAccount
            this.loans = loans
        }
        override suspend fun resetAllData(deleteDriveImages: Boolean) {}
    }
}
