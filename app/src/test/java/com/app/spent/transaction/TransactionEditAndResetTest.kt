package com.app.spent.transaction

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.FamilyMemberEntity
import com.app.spent.data.local.entity.ParentalControlConfigEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.sync.DriveConnectResult
import com.app.spent.data.sync.SharedMemberInfo
import com.app.spent.ui.transaction.AddTransactionUiIntent
import com.app.spent.ui.transaction.AddTransactionViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionEditAndResetTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSpentRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSpentRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAddTransactionModeByDefault() = runTest {
        val viewModel = AddTransactionViewModel(repository = fakeRepository, initialType = "EXPENSE")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditing)
        assertEquals(null, viewModel.uiState.value.editingTransactionId)
        assertEquals("EXPENSE", viewModel.uiState.value.selectedType)

        viewModel.onIntent(AddTransactionUiIntent.UpdateAmount("150.00"))
        viewModel.onIntent(AddTransactionUiIntent.UpdateNote("Lunch with team"))
        viewModel.onIntent(AddTransactionUiIntent.SaveTransaction)
        advanceUntilIdle()

        assertEquals(1, fakeRepository.addedTransactions.size)
        assertEquals(150.0, fakeRepository.addedTransactions[0].amount, 0.001)
        assertEquals("Lunch with team", fakeRepository.addedTransactions[0].note)
        assertEquals(0, fakeRepository.updatedTransactions.size)
    }

    @Test
    fun testNegativeAmountSavesAsPositive() = runTest {
        val viewModel = AddTransactionViewModel(repository = fakeRepository, initialType = "EXPENSE")
        advanceUntilIdle()

        viewModel.onIntent(AddTransactionUiIntent.UpdateAmount("-45.50"))
        assertTrue(viewModel.uiState.value.isValid)
        assertEquals(45.50, viewModel.uiState.value.parsedAmount, 0.001)

        viewModel.onIntent(AddTransactionUiIntent.UpdateNote("Negative expense"))
        viewModel.onIntent(AddTransactionUiIntent.SaveTransaction)
        advanceUntilIdle()

        assertEquals(1, fakeRepository.addedTransactions.size)
        assertEquals(45.50, fakeRepository.addedTransactions[0].amount, 0.001)
    }

    @Test
    fun testNegativeIncomeSavesAsPositive() = runTest {
        val viewModel = AddTransactionViewModel(repository = fakeRepository, initialType = "INCOME")
        advanceUntilIdle()

        viewModel.onIntent(AddTransactionUiIntent.UpdateAmount("-1200"))
        assertTrue(viewModel.uiState.value.isValid)
        assertEquals(1200.0, viewModel.uiState.value.parsedAmount, 0.001)

        viewModel.onIntent(AddTransactionUiIntent.UpdateNote("Negative income"))
        viewModel.onIntent(AddTransactionUiIntent.SaveTransaction)
        advanceUntilIdle()

        assertEquals(1, fakeRepository.addedTransactions.size)
        assertEquals(1200.0, fakeRepository.addedTransactions[0].amount, 0.001)
        assertEquals("INCOME", fakeRepository.addedTransactions[0].type)
    }

    @Test
    fun testNegativeCalculationEvaluationConvertsToPositive() = runTest {
        val viewModel = AddTransactionViewModel(repository = fakeRepository, initialType = "EXPENSE")
        advanceUntilIdle()

        viewModel.onIntent(AddTransactionUiIntent.UpdateAmount("20 - 50"))
        assertEquals("30", viewModel.uiState.value.computedPreviewFormatted)
        viewModel.onIntent(AddTransactionUiIntent.EvaluateAmount)
        advanceUntilIdle()

        assertEquals("30", viewModel.uiState.value.amountExpression)
        assertEquals(30.0, viewModel.uiState.value.parsedAmount, 0.001)
    }

    @Test
    fun testCreateRecurringTransactionInitializesRuleProperly() = runTest {
        val viewModel = AddTransactionViewModel(repository = fakeRepository, initialType = "INCOME")
        advanceUntilIdle()

        val selectedTs = 1710000000000L
        viewModel.onIntent(AddTransactionUiIntent.UpdateAmount("3000.00"))
        viewModel.onIntent(AddTransactionUiIntent.UpdateTimestamp(selectedTs))
        viewModel.onIntent(AddTransactionUiIntent.ToggleRecurring(true))
        viewModel.onIntent(AddTransactionUiIntent.SelectFrequency("MONTHLY"))
        viewModel.onIntent(AddTransactionUiIntent.UpdateNote("Monthly Salary"))
        viewModel.onIntent(AddTransactionUiIntent.SaveTransaction)
        advanceUntilIdle()

        assertEquals(1, fakeRepository.addedTransactions.size)
        val tx = fakeRepository.addedTransactions[0]
        assertEquals("INCOME", tx.type)
        assertEquals(3000.0, tx.amount, 0.001)
        assertNotNull(tx.recurringRuleId)

        assertEquals(1, fakeRepository.storedRecurringRules.size)
        val rule = fakeRepository.storedRecurringRules[tx.recurringRuleId!!]!!
        assertEquals("INCOME", rule.type)
        assertEquals(3000.0, rule.amount, 0.001)
        assertEquals("MONTHLY", rule.frequency)
        assertEquals(selectedTs, rule.startDate)
        assertEquals(selectedTs, rule.lastExecuted) // Must be initialized to avoid duplicate on day 0
    }

    @Test
    fun testEditRecurringTransactionUpdatesExistingRule() = runTest {
        val rule = RecurringRuleEntity(
            id = "rule-salary-1",
            amount = 2500.0,
            categoryId = "cat_salary",
            frequency = "MONTHLY",
            startDate = 1700000000000L,
            lastExecuted = 1700000000000L,
            note = "Old Salary",
            type = "INCOME"
        )
        fakeRepository.storedRecurringRules["rule-salary-1"] = rule

        val tx = TransactionEntity(
            id = "tx-salary-1",
            amount = 2500.0,
            type = "INCOME",
            categoryId = "cat_salary",
            note = "Old Salary",
            timestamp = 1700000000000L,
            recurringRuleId = "rule-salary-1"
        )
        fakeRepository.storedTransactions["tx-salary-1"] = tx

        val viewModel = AddTransactionViewModel(
            repository = fakeRepository,
            initialType = "INCOME",
            transactionId = "tx-salary-1"
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isRecurring)
        assertEquals("rule-salary-1", viewModel.uiState.value.editingRecurringRuleId)

        // Modify amount and note
        viewModel.onIntent(AddTransactionUiIntent.UpdateAmount("2800.00"))
        viewModel.onIntent(AddTransactionUiIntent.UpdateNote("Raised Salary"))
        viewModel.onIntent(AddTransactionUiIntent.SaveTransaction)
        advanceUntilIdle()

        // Verify existing rule updated without creating a new rule ID
        assertEquals(1, fakeRepository.storedRecurringRules.size)
        val updatedRule = fakeRepository.storedRecurringRules["rule-salary-1"]!!
        assertEquals(2800.0, updatedRule.amount, 0.001)
        assertEquals("Raised Salary", updatedRule.note)
        assertEquals("INCOME", updatedRule.type)
    }

    @Test
    fun testEditTransactionUncheckingRecurringDeletesRule() = runTest {
        val rule = RecurringRuleEntity(
            id = "rule-to-delete",
            amount = 50.0,
            categoryId = "cat_general",
            frequency = "MONTHLY",
            startDate = 1700000000000L,
            lastExecuted = 1700000000000L,
            note = "Gym membership",
            type = "EXPENSE"
        )
        fakeRepository.storedRecurringRules["rule-to-delete"] = rule

        val tx = TransactionEntity(
            id = "tx-gym",
            amount = 50.0,
            type = "EXPENSE",
            categoryId = "cat_general",
            note = "Gym membership",
            timestamp = 1700000000000L,
            recurringRuleId = "rule-to-delete"
        )
        fakeRepository.storedTransactions["tx-gym"] = tx

        val viewModel = AddTransactionViewModel(
            repository = fakeRepository,
            initialType = "EXPENSE",
            transactionId = "tx-gym"
        )
        advanceUntilIdle()

        // Uncheck recurring
        viewModel.onIntent(AddTransactionUiIntent.ToggleRecurring(false))
        viewModel.onIntent(AddTransactionUiIntent.SaveTransaction)
        advanceUntilIdle()

        // Recurring rule should be deleted
        assertEquals(0, fakeRepository.storedRecurringRules.size)
        val updatedTx = fakeRepository.updatedTransactions[0]
        assertEquals(null, updatedTx.recurringRuleId)
    }

    @Test
    fun testEditTransactionLoadsExistingDataAndUpdatesOnSave() = runTest {
        val existingTx = TransactionEntity(
            id = "tx-123",
            amount = 85.50,
            type = "EXPENSE",
            categoryId = "cat_food",
            note = "Grocery store",
            timestamp = 1700000000000L,
            imageUri = "content://media/receipt.jpg"
        )
        fakeRepository.storedTransactions["tx-123"] = existingTx

        val viewModel = AddTransactionViewModel(
            repository = fakeRepository,
            initialType = "EXPENSE",
            transactionId = "tx-123"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertEquals("tx-123", state.editingTransactionId)
        assertEquals("85.50", state.amountExpression)
        assertEquals("cat_food", state.selectedCategoryId)
        assertEquals("Grocery store", state.noteText)
        assertEquals("content://media/receipt.jpg", state.selectedImageUri)

        // Modify the note and amount
        viewModel.onIntent(AddTransactionUiIntent.UpdateAmount("95.00"))
        viewModel.onIntent(AddTransactionUiIntent.UpdateNote("Supermarket run"))
        viewModel.onIntent(AddTransactionUiIntent.SaveTransaction)
        advanceUntilIdle()

        assertEquals(0, fakeRepository.addedTransactions.size)
        assertEquals(1, fakeRepository.updatedTransactions.size)
        val updated = fakeRepository.updatedTransactions[0]
        assertEquals("tx-123", updated.id)
        assertEquals(95.0, updated.amount, 0.001)
        assertEquals("Supermarket run", updated.note)
        assertEquals("cat_food", updated.categoryId)
    }

    @Test
    fun testResetAllDataReseedsStarterCategories() = runTest {
        // Initially populate some data
        fakeRepository.storedTransactions["tx-1"] = TransactionEntity(amount = 50.0, categoryId = "cat_general")
        fakeRepository.categoriesFlow.value = listOf(CategoryEntity(id = "cat_general", name = "General", iconName = "Category", colorHex = "#000"))

        fakeRepository.resetAllData(deleteDriveImages = false)

        // Verify categories are re-seeded and not empty
        assertTrue(fakeRepository.seedStarterDataCalled)
        assertTrue(fakeRepository.categoriesFlow.value.isNotEmpty())
        assertNotNull(fakeRepository.categoriesFlow.value.find { it.id == "cat_general" })
        assertEquals(0, fakeRepository.storedTransactions.size)
    }
}

private class FakeSpentRepository : SpentRepository {
    val storedTransactions = mutableMapOf<String, TransactionEntity>()
    val addedTransactions = mutableListOf<TransactionEntity>()
    val updatedTransactions = mutableListOf<TransactionEntity>()
    val storedRecurringRules = mutableMapOf<String, RecurringRuleEntity>()
    var seedStarterDataCalled = false

    val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(
        listOf(
            CategoryEntity(id = "cat_general", name = "General", iconName = "Category", colorHex = "#64748B"),
            CategoryEntity(id = "cat_salary", name = "Salary", iconName = "Payments", colorHex = "#10B981"),
            CategoryEntity(id = "cat_groceries", name = "Groceries", iconName = "ShoppingCart", colorHex = "#059669"),
            CategoryEntity(id = "cat_food", name = "Food & Dining", iconName = "Restaurant", colorHex = "#F59E0B")
        )
    )

    override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = MutableStateFlow(storedTransactions.values.toList())
    override fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>> = MutableStateFlow(storedTransactions.values.take(limit))
    override fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
    override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = categoriesFlow.asStateFlow()
    override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = MutableStateFlow(null)
    override fun getUserAccountFlow(): Flow<UserAccountEntity?> = MutableStateFlow(null)
    override fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>> = MutableStateFlow(emptyList())
    override fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?> = MutableStateFlow(null)
    override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = MutableStateFlow(storedRecurringRules.values.toList())
    override fun getLoansFlow(): Flow<List<com.app.spent.data.local.entity.LoanEntity>> = MutableStateFlow(emptyList())

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
    override suspend fun processAndSaveImage(sourceUri: android.net.Uri, destinationType: String): Result<String> = Result.success(sourceUri.toString())
    override suspend fun addOrUpdateSharedMember(member: SharedMemberInfo) {}
    override suspend fun updateSharedMemberName(fileId: String, newName: String) {}
    override suspend fun updateUserProfileName(newName: String) {}
    override suspend fun removeSharedMember(fileId: String) {}
    override suspend fun clearSharedMembers() {}
    override suspend fun savePartnerInfo(fileId: String, name: String, email: String?) {}
    override suspend fun setPartnerLastSyncTimestamp(timestamp: Long) {}
    override suspend fun clearPartnerInfo() {}

    override suspend fun addTransaction(transaction: TransactionEntity) {
        addedTransactions.add(transaction)
        storedTransactions[transaction.id] = transaction
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        updatedTransactions.add(transaction)
        storedTransactions[transaction.id] = transaction
    }

    override suspend fun getTransactionById(id: String): TransactionEntity? {
        return storedTransactions[id]
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        storedTransactions.remove(transaction.id)
    }

    override suspend fun deleteTransactionById(id: String) {
        storedTransactions.remove(id)
    }

    override suspend fun addCategory(category: CategoryEntity) {}
    override suspend fun updateCategory(category: CategoryEntity) {}
    override suspend fun deleteCategoryById(id: String) {}
    override suspend fun setPayCycle(payCycle: PayCycleEntity) {}
    override suspend fun addRecurringRule(rule: RecurringRuleEntity) {
        storedRecurringRules[rule.id] = rule
    }
    override suspend fun deleteRecurringRuleById(id: String) {
        storedRecurringRules.remove(id)
    }
    override suspend fun executePendingRecurringRules() {}

    override suspend fun addLoan(loan: com.app.spent.data.local.entity.LoanEntity) {}
    override suspend fun updateLoan(loan: com.app.spent.data.local.entity.LoanEntity) {}
    override suspend fun deleteLoanById(id: String) {}
    override suspend fun recordLoanPayment(loanId: String, amount: Double) {}
    override suspend fun getLoanById(id: String): com.app.spent.data.local.entity.LoanEntity? = null

    override suspend fun seedStarterDataIfEmpty() {
        seedStarterDataCalled = true
        categoriesFlow.value = listOf(
            CategoryEntity(id = "cat_general", name = "General", iconName = "Category", colorHex = "#64748B"),
            CategoryEntity(id = "cat_salary", name = "Salary", iconName = "Payments", colorHex = "#10B981"),
            CategoryEntity(id = "cat_groceries", name = "Groceries", iconName = "ShoppingCart", colorHex = "#059669"),
            CategoryEntity(id = "cat_food", name = "Food & Dining", iconName = "Restaurant", colorHex = "#F59E0B")
        )
    }

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
        loans: List<com.app.spent.data.local.entity.LoanEntity>
    ) {}

    override suspend fun resetAllData(deleteDriveImages: Boolean) {
        storedTransactions.clear()
        categoriesFlow.value = emptyList()
        seedStarterDataIfEmpty()
    }
}
