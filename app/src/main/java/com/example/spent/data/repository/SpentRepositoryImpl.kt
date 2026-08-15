package com.example.spent.data.repository

import com.example.spent.data.local.dao.SpentDao
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.FamilyMemberEntity
import com.example.spent.data.local.entity.ParentalControlConfigEntity
import com.example.spent.data.local.entity.PayCycleEntity
import com.example.spent.data.local.entity.RecurringRuleEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.data.local.entity.UserAccountEntity
import com.example.spent.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class SpentRepositoryImpl(
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

    override suspend fun addTransaction(transaction: TransactionEntity) {
        dao.insertTransaction(transaction)
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
    }

    override suspend fun deleteTransactionById(id: String) {
        dao.deleteTransactionById(id)
    }

    override suspend fun addCategory(category: CategoryEntity) {
        dao.insertCategory(category)
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        dao.updateCategory(category)
    }

    override suspend fun deleteCategoryById(id: String) {
        dao.deleteCategoryById(id)
    }

    override suspend fun setPayCycle(payCycle: PayCycleEntity) {
        dao.insertPayCycle(payCycle)
        if (payCycle.frequency != "NONE" && payCycle.income > 0) {
            val existingTxList = dao.getTransactionsFlow().firstOrNull() ?: emptyList()
            val hasRecentSalary = existingTxList.any {
                it.type == "INCOME" && it.amount == payCycle.income && it.note.contains("Salary", ignoreCase = true)
            }
            if (!hasRecentSalary) {
                val catId = dao.getCategoriesFlow().firstOrNull()?.find { it.id == "cat_general" }?.id ?: "cat_general"
                val salaryTx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    amount = payCycle.income,
                    type = "INCOME",
                    categoryId = catId,
                    timestamp = System.currentTimeMillis(),
                    note = "Payday Base Salary (${payCycle.frequency})"
                )
                dao.insertTransaction(salaryTx)
            }
        }
    }

    override suspend fun addRecurringRule(rule: RecurringRuleEntity) {
        dao.insertRecurringRule(rule)
    }

    override suspend fun deleteRecurringRuleById(id: String) {
        dao.deleteRecurringRuleById(id)
    }

    override suspend fun executePendingRecurringRules() {
        val rules = dao.getAllRecurringRules()
        val now = System.currentTimeMillis()

        for (rule in rules) {
            val intervalMs = when (rule.frequency) {
                "DAILY" -> 86_400_000L
                "WEEKLY" -> 7 * 86_400_000L
                "MONTHLY" -> 30 * 86_400_000L
                else -> 30 * 86_400_000L
            }

            val lastRun = if (rule.lastExecuted > 0) rule.lastExecuted else rule.startDate
            if (now - lastRun >= intervalMs) {
                val transaction = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    ownerProfileId = rule.ownerProfileId,
                    amount = rule.amount,
                    type = "EXPENSE",
                    categoryId = rule.categoryId,
                    timestamp = now,
                    note = rule.note.ifEmpty { "Auto recurring payment (${rule.frequency})" },
                    recurringRuleId = rule.id
                )
                dao.insertTransaction(transaction)
                dao.updateRecurringRule(rule.copy(lastExecuted = now))
            }
        }
    }

    override suspend fun seedStarterDataIfEmpty() {
        val existingCategories = dao.getCategoriesFlow().firstOrNull() ?: emptyList()
        if (existingCategories.isEmpty()) {
            val defaultCategories = listOf(
                CategoryEntity(id = "cat_general", name = "General", iconName = "Category", colorHex = "#64748B", budgetAmount = 0.0, displayOrder = 0),
                CategoryEntity(id = "cat_groceries", name = "Groceries", iconName = "ShoppingCart", colorHex = "#4CAF50", budgetAmount = 0.0, displayOrder = 1),
                CategoryEntity(id = "cat_utilities", name = "Utilities", iconName = "Bolt", colorHex = "#FF9800", budgetAmount = 0.0, displayOrder = 2),
                CategoryEntity(id = "cat_transport", name = "Transport", iconName = "DirectionsCar", colorHex = "#2196F3", budgetAmount = 0.0, displayOrder = 3),
                CategoryEntity(id = "cat_entertainment", name = "Entertainment", iconName = "Movie", colorHex = "#9C27B0", budgetAmount = 0.0, displayOrder = 4),
                CategoryEntity(id = "cat_shopping", name = "Shopping", iconName = "ShoppingBag", colorHex = "#E91E63", budgetAmount = 0.0, displayOrder = 5),
                CategoryEntity(id = "cat_savings", name = "Savings", iconName = "Savings", colorHex = "#009688", budgetAmount = 0.0, displayOrder = 6)
            )
            dao.insertCategories(defaultCategories)
        }

        val currentCycle = dao.getCurrentPayCycle()
        if (currentCycle == null) {
            val defaultCycle = PayCycleEntity(
                id = "default_cycle",
                frequency = "MONTHLY",
                startDate = System.currentTimeMillis(),
                income = 0.0
            )
            dao.insertPayCycle(defaultCycle)
        }

        val userAccount = dao.getUserAccountFlow().firstOrNull()
        if (userAccount == null) {
            dao.insertOrUpdateUserAccount(
                UserAccountEntity(
                    id = "primary_account",
                    displayName = "Primary User",
                    role = "INDEPENDENT"
                )
            )
        }
    }

    override suspend fun setWalkthroughCompleted(completed: Boolean) {
        preferencesRepository.setWalkthroughCompleted(completed)
    }

    override suspend fun setDarkThemeMode(enabled: Boolean?) {
        preferencesRepository.setDarkThemeMode(enabled)
    }

    override suspend fun setCurrencySymbol(symbol: String) {
        preferencesRepository.setCurrencySymbol(symbol)
    }

    override suspend fun setAppLanguage(languageCode: String?) {
        preferencesRepository.setAppLanguage(languageCode)
    }

    override suspend fun resetAllData() {
        dao.deleteAllTransactions()
        dao.deleteAllCategories()
        preferencesRepository.setWalkthroughCompleted(false)
        seedStarterDataIfEmpty()
    }
}
