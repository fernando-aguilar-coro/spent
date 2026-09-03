package com.app.spent.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.FamilyMemberEntity
import com.app.spent.data.local.entity.ParentalControlConfigEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface SpentDao {

  // User Account
  @Query("SELECT * FROM user_account WHERE id = 'primary_account'")
  fun getUserAccountFlow(): Flow<UserAccountEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateUserAccount(account: UserAccountEntity)

  // Family Members
  @Query("SELECT * FROM family_members")
  fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFamilyMember(member: FamilyMemberEntity)

  // Parental Controls
  @Query("SELECT * FROM parental_control_config WHERE id = 'primary_config'")
  fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertParentalConfig(config: ParentalControlConfigEntity)

  // Categories
  @Query("SELECT * FROM categories ORDER BY displayOrder ASC")
  fun getCategoriesFlow(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM categories WHERE id = :id")
  suspend fun getCategoryById(id: String): CategoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategory(category: CategoryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategories(categories: List<CategoryEntity>)

  @Update
  suspend fun updateCategory(category: CategoryEntity)

  @Query("DELETE FROM categories WHERE id = :id")
  suspend fun deleteCategoryById(id: String)

  // Pay Cycles
  @Query("SELECT * FROM pay_cycles ORDER BY startDate DESC LIMIT 1")
  fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?>

  @Query("SELECT * FROM pay_cycles ORDER BY startDate DESC LIMIT 1")
  suspend fun getCurrentPayCycle(): PayCycleEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPayCycle(payCycle: PayCycleEntity)

  // Transactions
  @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
  fun getTransactionsFlow(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
  fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE timestamp >= :startTime ORDER BY timestamp DESC")
  fun getTransactionsSinceFlow(startTime: Long): Flow<List<TransactionEntity>>

  @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = :type")
  fun getTotalAmountByTypeFlow(type: String): Flow<Double>

  @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = :type AND timestamp BETWEEN :startTime AND :endTime")
  fun getTotalAmountByTypeBetweenFlow(type: String, startTime: Long, endTime: Long): Flow<Double>

  @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE categoryId = :categoryId AND type = 'EXPENSE' AND timestamp BETWEEN :startTime AND :endTime")
  fun getCategorySpentBetweenFlow(categoryId: String, startTime: Long, endTime: Long): Flow<Double>

  @Query("SELECT * FROM transactions WHERE (:query = '' OR note LIKE '%' || :query || '%' OR merchantName LIKE '%' || :query || '%') AND (:type IS NULL OR type = :type) AND (:categoryId IS NULL OR categoryId = :categoryId) ORDER BY timestamp DESC")
  fun searchTransactionsFlow(query: String, type: String?, categoryId: String?): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE id = :id")
  suspend fun getTransactionById(id: String): TransactionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(transaction: TransactionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransactions(transactions: List<TransactionEntity>)

  @Delete
  suspend fun deleteTransaction(transaction: TransactionEntity)

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteTransactionById(id: String)

  @Query("SELECT COUNT(*) FROM transactions WHERE recurringRuleId = :ruleId")
  suspend fun getTransactionCountForRecurringRule(ruleId: String): Int

  // Recurring Rules
  @Query("SELECT * FROM recurring_rules")
  fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>>

  @Query("SELECT * FROM recurring_rules")
  suspend fun getAllRecurringRules(): List<RecurringRuleEntity>

  @Query("SELECT * FROM recurring_rules WHERE isActive = 1")
  suspend fun getActiveRecurringRules(): List<RecurringRuleEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecurringRule(rule: RecurringRuleEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecurringRules(rules: List<RecurringRuleEntity>)

  @Update
  suspend fun updateRecurringRule(rule: RecurringRuleEntity)

  @Query("UPDATE recurring_rules SET isActive = :isActive WHERE id = :id")
  suspend fun updateRecurringRuleActiveStatus(id: String, isActive: Boolean)

  @Query("DELETE FROM recurring_rules WHERE id = :id")
  suspend fun deleteRecurringRuleById(id: String)

  @Query("DELETE FROM transactions WHERE recurringRuleId = :ruleId")
  suspend fun deleteTransactionsByRecurringRuleId(ruleId: String)

  // Loans & Debts
  @Query("SELECT * FROM loans ORDER BY createdAt DESC")
  fun getLoansFlow(): Flow<List<com.app.spent.data.local.entity.LoanEntity>>

  @Query("SELECT * FROM loans WHERE id = :id")
  suspend fun getLoanById(id: String): com.app.spent.data.local.entity.LoanEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLoan(loan: com.app.spent.data.local.entity.LoanEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLoans(loans: List<com.app.spent.data.local.entity.LoanEntity>)

  @Update
  suspend fun updateLoan(loan: com.app.spent.data.local.entity.LoanEntity)

  @Query("DELETE FROM loans WHERE id = :id")
  suspend fun deleteLoanById(id: String)

  @Query("DELETE FROM loans")
  suspend fun deleteAllLoans()

  // Data Reset / Restore
  @Query("DELETE FROM transactions")
  suspend fun deleteAllTransactions()

  @Query("DELETE FROM categories")
  suspend fun deleteAllCategories()

  @Query("DELETE FROM recurring_rules")
  suspend fun deleteAllRecurringRules()

  @Query("DELETE FROM pay_cycles")
  suspend fun deleteAllPayCycles()
}
