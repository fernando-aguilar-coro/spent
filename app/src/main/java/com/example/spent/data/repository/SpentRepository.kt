package com.example.spent.data.repository

import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.FamilyMemberEntity
import com.example.spent.data.local.entity.ParentalControlConfigEntity
import com.example.spent.data.local.entity.PayCycleEntity
import com.example.spent.data.local.entity.RecurringRuleEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.data.local.entity.UserAccountEntity
import kotlinx.coroutines.flow.Flow

interface SpentRepository {
    fun getTransactionsFlow(): Flow<List<TransactionEntity>>
    fun getCategoriesFlow(): Flow<List<CategoryEntity>>
    fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?>
    fun getUserAccountFlow(): Flow<UserAccountEntity?>
    fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>>
    fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?>
    fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>>

    val isWalkthroughCompletedFlow: Flow<Boolean>
    val isDarkThemeFlow: Flow<Boolean?>
    val currencySymbolFlow: Flow<String>
    val appLanguageFlow: Flow<String?>
    val savingsGoalNameFlow: Flow<String>
    val savingsGoalTotalFlow: Flow<Double>
    val savingsMonthlyContributionFlow: Flow<Double>

    suspend fun addTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun deleteTransactionById(id: String)

    suspend fun addCategory(category: CategoryEntity)
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategoryById(id: String)

    suspend fun setPayCycle(payCycle: PayCycleEntity)
    suspend fun addRecurringRule(rule: RecurringRuleEntity)
    suspend fun deleteRecurringRuleById(id: String)
    suspend fun executePendingRecurringRules()
    suspend fun seedStarterDataIfEmpty()

    suspend fun setWalkthroughCompleted(completed: Boolean)
    suspend fun setDarkThemeMode(enabled: Boolean?)
    suspend fun setCurrencySymbol(symbol: String)
    suspend fun setAppLanguage(languageCode: String?)
    suspend fun setSavingsGoal(name: String, totalGoal: Double, monthlyContribution: Double)
    suspend fun clearSavingsGoal()
    suspend fun resetAllData()
}
