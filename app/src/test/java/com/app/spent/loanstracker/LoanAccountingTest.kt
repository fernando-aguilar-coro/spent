package com.app.spent.loanstracker

import android.content.Context
import com.app.spent.data.local.dao.SpentDao
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.FamilyMemberEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.data.local.entity.ParentalControlConfigEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import com.app.spent.data.preferences.UserPreferencesRepository
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.repository.SpentRepositoryImpl
import com.app.spent.data.sync.DriveConnectResult
import com.app.spent.data.sync.SharedMemberInfo
import com.app.spent.ui.loanstracker.LoansTrackerUiIntent
import com.app.spent.ui.loanstracker.LoansTrackerViewModel
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LoanAccountingTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeSpentDao
    private lateinit var mockPrefs: UserPreferencesRepository
    private lateinit var repository: SpentRepositoryImpl
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockContext = android.content.ContextWrapper(null)
        fakeDao = FakeSpentDao()
        mockPrefs = UserPreferencesRepository(mockContext)
        repository = SpentRepositoryImpl(mockContext, fakeDao, mockPrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRepositoryAddOwedToMeCreatesExpenseAndPaymentCreatesIncome() = runTest {
        val loan = LoanEntity(
            id = "loan-1",
            type = "OWED_TO_ME",
            principalAmount = 150.0,
            counterpartyName = "John",
            categoryId = "cat_general",
            startDate = 1000L
        )

        // Adding loan: Someone owes me (I lent 150.0) -> EXPENSE
        repository.addLoan(loan)

        val initialTx = fakeDao.transactionsMap.values.firstOrNull { it.amount == 150.0 }
        assertTrue("Expense transaction should be created when lending money", initialTx != null)
        assertEquals("EXPENSE", initialTx?.type)

        // John pays back 50.0 -> INCOME
        repository.recordLoanPayment("loan-1", 50.0)

        val repaymentTx = fakeDao.transactionsMap.values.firstOrNull { it.amount == 50.0 }
        assertTrue("Income transaction should be created when borrower repays", repaymentTx != null)
        assertEquals("INCOME", repaymentTx?.type)

        // Verify loan state
        val updated = fakeDao.loansMap["loan-1"]
        assertEquals(50.0, updated?.paidAmount ?: 0.0, 0.001)
        assertEquals(100.0, updated?.remainingAmount ?: 0.0, 0.001)
    }

    @Test
    fun testRepositoryAddIOweCreatesIncomeAndRepaymentCreatesExpense() = runTest {
        val loan = LoanEntity(
            id = "loan-2",
            type = "I_OWE",
            principalAmount = 500.0,
            counterpartyName = "Bank",
            categoryId = "cat_general",
            startDate = 2000L
        )

        // Adding loan: I owe (I borrowed 500.0) -> INCOME
        repository.addLoan(loan)

        val initialTx = fakeDao.transactionsMap.values.firstOrNull { it.amount == 500.0 }
        assertTrue("Income transaction should be created when receiving a loan", initialTx != null)
        assertEquals("INCOME", initialTx?.type)

        // I repay 200.0 -> EXPENSE
        repository.recordLoanPayment("loan-2", 200.0)

        val repaymentTx = fakeDao.transactionsMap.values.firstOrNull { it.amount == 200.0 }
        assertTrue("Expense transaction should be created when paying loan installment", repaymentTx != null)
        assertEquals("EXPENSE", repaymentTx?.type)

        // Verify loan state
        val updated = fakeDao.loansMap["loan-2"]
        assertEquals(200.0, updated?.paidAmount ?: 0.0, 0.001)
        assertEquals(300.0, updated?.remainingAmount ?: 0.0, 0.001)
    }

    @Test
    fun testLoansTrackerViewModelSettleLoanRecordsRemainingPayment() = runTest {
        val fakeRepo = TestLoansRepository()
        val loan = LoanEntity(
            id = "loan-settle-1",
            type = "I_OWE",
            principalAmount = 300.0,
            counterpartyName = "Mike",
            categoryId = "cat_general"
        )
        fakeRepo.addLoan(loan)
        // Record partial payment of 100.0
        fakeRepo.recordLoanPayment("loan-settle-1", 100.0)

        val viewModel = LoansTrackerViewModel(fakeRepo)
        advanceUntilIdle()

        // Settle remaining 200.0
        viewModel.onIntent(LoansTrackerUiIntent.SettleLoan("loan-settle-1"))
        advanceUntilIdle()

        val updated = fakeRepo.loansMap["loan-settle-1"]
        assertTrue("Loan should be marked as settled", updated?.isSettled == true)
        assertEquals(300.0, updated?.paidAmount ?: 0.0, 0.001)

        // Verify that an EXPENSE transaction for 200.0 was recorded on settlement
        val settleTx = fakeRepo.transactionsMap.values.firstOrNull { it.amount == 200.0 }
        assertTrue("Expense transaction for remaining 200.0 should be recorded", settleTx != null)
        assertEquals("EXPENSE", settleTx?.type)
    }

    private class TestLoansRepository : SpentRepository {
        val loansMap = mutableMapOf<String, LoanEntity>()
        val transactionsMap = mutableMapOf<String, TransactionEntity>()
        val loansFlow = MutableStateFlow<List<LoanEntity>>(emptyList())
        val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())

        override fun getLoansFlow(): Flow<List<LoanEntity>> = loansFlow.asStateFlow()
        override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = categoriesFlow.asStateFlow()
        override val currencySymbolFlow: Flow<String> = MutableStateFlow("$")
        override val isNetSavingsHiddenFlow: Flow<Boolean> = MutableStateFlow(false)

        override suspend fun getLoanById(id: String): LoanEntity? = loansMap[id]

        override suspend fun addLoan(loan: LoanEntity) {
            loansMap[loan.id] = loan
            val isOwedToMe = loan.type == "OWED_TO_ME"
            val txType = if (isOwedToMe) "EXPENSE" else "INCOME"
            val tx = TransactionEntity(
                amount = loan.principalAmount,
                type = txType,
                categoryId = loan.categoryId,
                note = "Loan: ${loan.counterpartyName}"
            )
            transactionsMap[tx.id] = tx
            loansFlow.value = loansMap.values.toList()
        }

        override suspend fun updateLoan(loan: LoanEntity) {
            loansMap[loan.id] = loan
            loansFlow.value = loansMap.values.toList()
        }

        override suspend fun deleteLoanById(id: String) {
            loansMap.remove(id)
            loansFlow.value = loansMap.values.toList()
        }

        override suspend fun recordLoanPayment(loanId: String, amount: Double) {
            val loan = loansMap[loanId] ?: return
            val newPaid = (loan.paidAmount + amount).coerceAtLeast(0.0)
            val isSettled = newPaid >= loan.principalAmount
            loansMap[loanId] = loan.copy(paidAmount = newPaid, isSettled = isSettled)

            val isOwedToMe = loan.type == "OWED_TO_ME"
            val txType = if (isOwedToMe) "INCOME" else "EXPENSE"
            val tx = TransactionEntity(
                amount = amount,
                type = txType,
                categoryId = loan.categoryId,
                note = "Loan payment: ${loan.counterpartyName}"
            )
            transactionsMap[tx.id] = tx
            loansFlow.value = loansMap.values.toList()
        }

        override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = MutableStateFlow(transactionsMap.values.toList())
        override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = MutableStateFlow(null)
        override fun getUserAccountFlow(): Flow<UserAccountEntity?> = MutableStateFlow(null)
        override fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>> = MutableStateFlow(emptyList())
        override fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?> = MutableStateFlow(null)
        override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = MutableStateFlow(emptyList())
        override val isWalkthroughCompletedFlow: Flow<Boolean> = MutableStateFlow(true)
        override val isDarkThemeFlow: Flow<Boolean?> = MutableStateFlow(null)
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
        override suspend fun setWalkthroughCompleted(completed: Boolean) {}
        override suspend fun setDarkThemeMode(enabled: Boolean?) {}
        override suspend fun setCurrencySymbol(symbol: String) {}
        override suspend fun setAppLanguage(languageCode: String?) {}
        override suspend fun setImageStorageLocation(location: String) {}
        override suspend fun setSavingsGoal(name: String, totalGoal: Double, monthlyContribution: Double) {}
        override suspend fun clearSavingsGoal() {}
        override suspend fun setLastDriveSyncTimestamp(timestamp: Long) {}
        override suspend fun restoreAllData(categories: List<CategoryEntity>, transactions: List<TransactionEntity>, payCycle: PayCycleEntity?, recurringRules: List<RecurringRuleEntity>, userAccount: UserAccountEntity?, loans: List<LoanEntity>) {}
        override suspend fun resetAllData(deleteDriveImages: Boolean) {}
    }

    private class FakeSpentDao : SpentDao {
        val loansMap = mutableMapOf<String, LoanEntity>()
        val transactionsMap = mutableMapOf<String, TransactionEntity>()
        private val loansFlow = MutableStateFlow<List<LoanEntity>>(emptyList())
        private val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())

        private fun refresh() {
            loansFlow.value = loansMap.values.toList()
            transactionsFlow.value = transactionsMap.values.toList()
        }

        override fun getLoansFlow(): Flow<List<LoanEntity>> = loansFlow.asStateFlow()
        override suspend fun getLoanById(id: String): LoanEntity? = loansMap[id]
        override suspend fun insertLoan(loan: LoanEntity) {
            loansMap[loan.id] = loan
            refresh()
        }
        override suspend fun insertLoans(loans: List<LoanEntity>) {
            loans.forEach { loansMap[it.id] = it }
            refresh()
        }
        override suspend fun updateLoan(loan: LoanEntity) {
            loansMap[loan.id] = loan
            refresh()
        }
        override suspend fun deleteLoanById(id: String) {
            loansMap.remove(id)
            refresh()
        }
        override suspend fun deleteAllLoans() {
            loansMap.clear()
            refresh()
        }

        override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = transactionsFlow.asStateFlow()
        override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = MutableStateFlow(transactionsMap.values.take(limit))
        override fun getTransactionsSinceFlow(startTime: Long): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override fun getTotalAmountByTypeFlow(type: String): Flow<Double> = MutableStateFlow(0.0)
        override fun getTotalAmountByTypeBetweenFlow(type: String, startTime: Long, endTime: Long): Flow<Double> = MutableStateFlow(0.0)
        override fun getCategorySpentBetweenFlow(categoryId: String, startTime: Long, endTime: Long): Flow<Double> = MutableStateFlow(0.0)
        override fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override suspend fun insertTransaction(transaction: TransactionEntity) {
            transactionsMap[transaction.id] = transaction
            refresh()
        }
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
            transactions.forEach { transactionsMap[it.id] = it }
            refresh()
        }
        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            transactionsMap.remove(transaction.id)
            refresh()
        }
        override suspend fun deleteTransactionById(id: String) {
            transactionsMap.remove(id)
            refresh()
        }
        override suspend fun deleteAllTransactions() {
            transactionsMap.clear()
            refresh()
        }
        override suspend fun getTransactionById(id: String): TransactionEntity? = transactionsMap[id]

        override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
        override suspend fun getCategoryById(id: String): CategoryEntity? = null
        override suspend fun insertCategories(categories: List<CategoryEntity>) {}
        override suspend fun insertCategory(category: CategoryEntity) {}
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategoryById(id: String) {}
        override suspend fun deleteAllCategories() {}

        override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = MutableStateFlow(null)
        override suspend fun getCurrentPayCycle(): PayCycleEntity? = null
        override suspend fun insertPayCycle(payCycle: PayCycleEntity) {}
        override suspend fun deleteAllPayCycles() {}

        override fun getUserAccountFlow(): Flow<UserAccountEntity?> = MutableStateFlow(null)
        override suspend fun insertOrUpdateUserAccount(account: UserAccountEntity) {}

        override fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>> = MutableStateFlow(emptyList())
        override suspend fun insertFamilyMember(member: FamilyMemberEntity) {}

        override fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?> = MutableStateFlow(null)
        override suspend fun insertParentalConfig(config: ParentalControlConfigEntity) {}

        override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = MutableStateFlow(emptyList())
        override suspend fun getAllRecurringRules(): List<RecurringRuleEntity> = emptyList()
        override suspend fun getActiveRecurringRules(): List<RecurringRuleEntity> = emptyList()
        override suspend fun insertRecurringRule(rule: RecurringRuleEntity) {}
        override suspend fun insertRecurringRules(rules: List<RecurringRuleEntity>) {}
        override suspend fun updateRecurringRule(rule: RecurringRuleEntity) {}
        override suspend fun updateRecurringRuleActiveStatus(id: String, isActive: Boolean) {}
        override suspend fun deleteRecurringRuleById(id: String) {}
        override suspend fun deleteAllRecurringRules() {}
        override suspend fun getTransactionCountForRecurringRule(ruleId: String): Int = 0
        override suspend fun deleteTransactionsByRecurringRuleId(ruleId: String) {}
    }
}
