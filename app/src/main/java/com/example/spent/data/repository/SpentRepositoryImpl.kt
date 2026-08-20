package com.app.spent.data.repository

import android.content.Context
import com.app.spent.data.local.dao.SpentDao
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.FamilyMemberEntity
import com.app.spent.data.local.entity.ParentalControlConfigEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import com.app.spent.data.preferences.UserPreferencesRepository
import com.app.spent.data.sync.DriveConnectResult
import com.app.spent.data.sync.DriveSyncManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class SpentRepositoryImpl(
    private val context: Context,
    private val dao: SpentDao,
    private val preferencesRepository: UserPreferencesRepository
) : SpentRepository {

    override fun getTransactionsFlow(): Flow<List<TransactionEntity>> = dao.getTransactionsFlow()
    override fun getCategoriesFlow(): Flow<List<CategoryEntity>> = dao.getCategoriesFlow()
    override fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?> = dao.getCurrentPayCycleFlow()
    override fun getUserAccountFlow(): Flow<UserAccountEntity?> = dao.getUserAccountFlow()
    override fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>> = dao.getFamilyMembersFlow()
    override fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?> = dao.getParentalConfigFlow()
    override fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>> = dao.getRecurringRulesFlow()

    override val isWalkthroughCompletedFlow: Flow<Boolean> = preferencesRepository.isWalkthroughCompletedFlow
    override val isDarkThemeFlow: Flow<Boolean?> = preferencesRepository.isDarkThemeFlow
    override val currencySymbolFlow: Flow<String> = preferencesRepository.currencySymbolFlow
    override val appLanguageFlow: Flow<String?> = preferencesRepository.appLanguageFlow
    override val savingsGoalNameFlow: Flow<String> = preferencesRepository.savingsGoalNameFlow
    override val savingsGoalTotalFlow: Flow<Double> = preferencesRepository.savingsGoalTotalFlow
    override val savingsMonthlyContributionFlow: Flow<Double> = preferencesRepository.savingsMonthlyContributionFlow
    override val lastDriveSyncTimestampFlow: Flow<Long> = preferencesRepository.lastDriveSyncTimestampFlow

    override val isDriveConnectedFlow: Flow<Boolean> = preferencesRepository.isDriveConnectedFlow
    override val driveAccountEmailFlow: Flow<String?> = preferencesRepository.driveAccountEmailFlow
    override val isSyncingDriveFlow: Flow<Boolean> = DriveSyncManager.isSyncing
    override val partnerDriveFileIdFlow: Flow<String?> = preferencesRepository.partnerDriveFileIdFlow
    override val partnerNameFlow: Flow<String?> = preferencesRepository.partnerNameFlow
    override val partnerEmailFlow: Flow<String?> = preferencesRepository.partnerEmailFlow
    override val partnerLastSyncTimestampFlow: Flow<Long> = preferencesRepository.partnerLastSyncTimestampFlow
    override val isPartnerPairedFlow: Flow<Boolean> = preferencesRepository.isPartnerPairedFlow
    override val sharedMembersFlow: Flow<List<com.app.spent.data.sync.SharedMemberInfo>> = preferencesRepository.sharedMembersFlow

    override suspend fun connectGoogleDrive(account: GoogleSignInAccount): DriveConnectResult =
        DriveSyncManager.connectAccount(context, account, this, preferencesRepository)

    override suspend fun disconnectGoogleDrive() {
        DriveSyncManager.disconnectAccount(context, preferencesRepository)
    }

    override suspend fun syncToGoogleDrive(): Result<Boolean> =
        DriveSyncManager.syncNow(context, this, preferencesRepository)

    override fun triggerAutoSync() {
        DriveSyncManager.triggerAutoSync(context, this, preferencesRepository)
    }

    override suspend fun addOrUpdateSharedMember(member: com.app.spent.data.sync.SharedMemberInfo) {
        preferencesRepository.addOrUpdateSharedMember(member)
    }

    override suspend fun removeSharedMember(fileId: String) {
        preferencesRepository.removeSharedMember(fileId)
    }

    override suspend fun clearSharedMembers() {
        preferencesRepository.clearSharedMembers()
    }

    override suspend fun savePartnerInfo(fileId: String, name: String, email: String?) {
        preferencesRepository.savePartnerInfo(fileId, name, email)
    }

    override suspend fun setPartnerLastSyncTimestamp(timestamp: Long) {
        preferencesRepository.setPartnerLastSyncTimestamp(timestamp)
    }

    override suspend fun clearPartnerInfo() {
        preferencesRepository.clearPartnerInfo()
    }

    override suspend fun addTransaction(transaction: TransactionEntity) {
        dao.insertTransaction(transaction)
        triggerAutoSync()
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
        triggerAutoSync()
    }

    override suspend fun deleteTransactionById(id: String) {
        dao.deleteTransactionById(id)
        triggerAutoSync()
    }

    override suspend fun addCategory(category: CategoryEntity) {
        dao.insertCategory(category)
        triggerAutoSync()
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        dao.updateCategory(category)
        triggerAutoSync()
    }

    override suspend fun deleteCategoryById(id: String) {
        dao.deleteCategoryById(id)
        triggerAutoSync()
    }

    override suspend fun setPayCycle(payCycle: PayCycleEntity) {
        dao.insertPayCycle(payCycle)
        if (payCycle.frequency != "NONE" && payCycle.income > 0) {
            val transactions = dao.getTransactionsFlow().firstOrNull() ?: emptyList()
            val hasSalary = transactions.any { it.type == "INCOME" && it.amount == payCycle.income }
            if (!hasSalary) {
                val catId = dao.getCategoriesFlow().firstOrNull()?.find { it.id == "cat_salary" }?.id ?: "cat_general"
                dao.insertTransaction(
                    TransactionEntity(
                        amount = payCycle.income,
                        type = "INCOME",
                        categoryId = catId,
                        note = "Initial Salary Setup"
                    )
                )
            }
        }
        triggerAutoSync()
    }

    override suspend fun addRecurringRule(rule: RecurringRuleEntity) {
        dao.insertRecurringRule(rule)
        triggerAutoSync()
    }

    override suspend fun deleteRecurringRuleById(id: String) {
        dao.deleteRecurringRuleById(id)
        triggerAutoSync()
    }

    override suspend fun executePendingRecurringRules() {
        val rules = dao.getAllRecurringRules()
        val now = System.currentTimeMillis()
        for (rule in rules) {
            if (rule.endDate != null && rule.endDate > 0 && now > rule.endDate) continue
            val intervalMillis: Long = when (rule.frequency) {
                "DAILY" -> 24L * 60 * 60 * 1000
                "WEEKLY" -> 7L * 24 * 60 * 60 * 1000
                "BIWEEKLY" -> 14L * 24 * 60 * 60 * 1000
                "MONTHLY" -> 30L * 24 * 60 * 60 * 1000
                else -> 30L * 24 * 60 * 60 * 1000
            }
            val lastRun = rule.lastExecuted
            if (lastRun == 0L || (now - lastRun) >= intervalMillis) {
                dao.insertTransaction(
                    TransactionEntity(
                        ownerProfileId = rule.ownerProfileId,
                        amount = rule.amount,
                        type = "EXPENSE",
                        categoryId = rule.categoryId,
                        timestamp = now,
                        note = rule.note,
                        recurringRuleId = rule.id
                    )
                )
                dao.updateRecurringRule(rule.copy(lastExecuted = now))
            }
        }
    }

    override suspend fun seedStarterDataIfEmpty() {
        val cats = dao.getCategoriesFlow().firstOrNull()
        if (cats.isNullOrEmpty()) {
            dao.insertCategories(
                listOf(
                    CategoryEntity(id = "cat_general", name = "General", iconName = "Category", colorHex = "#64748B", budgetAmount = 0.0),
                    CategoryEntity(id = "cat_salary", name = "Salary", iconName = "Payments", colorHex = "#10B981", budgetAmount = 0.0)
                )
            )
        }
    }

    override suspend fun setWalkthroughCompleted(completed: Boolean) =
        preferencesRepository.setWalkthroughCompleted(completed)

    override suspend fun setDarkThemeMode(enabled: Boolean?) =
        preferencesRepository.setDarkThemeMode(enabled)

    override suspend fun setCurrencySymbol(symbol: String) =
        preferencesRepository.setCurrencySymbol(symbol)

    override suspend fun setAppLanguage(languageCode: String?) =
        preferencesRepository.setAppLanguage(languageCode)

    override suspend fun setSavingsGoal(name: String, totalGoal: Double, monthlyContribution: Double) =
        preferencesRepository.setSavingsGoal(name, totalGoal, monthlyContribution)

    override suspend fun clearSavingsGoal() =
        preferencesRepository.clearSavingsGoal()

    override suspend fun setLastDriveSyncTimestamp(timestamp: Long) =
        preferencesRepository.setLastDriveSyncTimestamp(timestamp)

    override suspend fun restoreAllData(
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        payCycle: PayCycleEntity?,
        recurringRules: List<RecurringRuleEntity>,
        userAccount: UserAccountEntity?
    ) {
        dao.deleteAllTransactions()
        dao.deleteAllCategories()
        dao.deleteAllRecurringRules()
        dao.deleteAllPayCycles()

        if (categories.isNotEmpty()) dao.insertCategories(categories)
        if (transactions.isNotEmpty()) dao.insertTransactions(transactions)
        if (recurringRules.isNotEmpty()) dao.insertRecurringRules(recurringRules)
        if (payCycle != null) dao.insertPayCycle(payCycle)
        if (userAccount != null) dao.insertOrUpdateUserAccount(userAccount)
    }

    override suspend fun resetAllData() {
        dao.deleteAllTransactions()
        dao.deleteAllCategories()
        dao.deleteAllRecurringRules()
        dao.deleteAllPayCycles()
        preferencesRepository.clearSavingsGoal()
    }
}
