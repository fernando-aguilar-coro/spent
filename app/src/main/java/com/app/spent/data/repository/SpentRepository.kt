package com.app.spent.data.repository

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.FamilyMemberEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.data.local.entity.ParentalControlConfigEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import kotlinx.coroutines.flow.Flow

interface SpentRepository {
  fun getTransactionsFlow(): Flow<List<TransactionEntity>>
  fun getRecentTransactionsFlow(limit: Int = 20): Flow<List<TransactionEntity>>
  fun searchTransactionsFlow(query: String, type: String? = null, categoryId: String? = null): Flow<List<TransactionEntity>>
  fun getCategoriesFlow(): Flow<List<CategoryEntity>>
  fun getCurrentPayCycleFlow(): Flow<PayCycleEntity?>
  fun getUserAccountFlow(): Flow<UserAccountEntity?>
  fun getFamilyMembersFlow(): Flow<List<FamilyMemberEntity>>
  fun getParentalConfigFlow(): Flow<ParentalControlConfigEntity?>
  fun getRecurringRulesFlow(): Flow<List<RecurringRuleEntity>>
  fun getLoansFlow(): Flow<List<LoanEntity>>

  val isWalkthroughCompletedFlow: Flow<Boolean>
  val isDarkThemeFlow: Flow<Boolean?>
  val currencySymbolFlow: Flow<String>
  val appLanguageFlow: Flow<String?>
  val savingsGoalNameFlow: Flow<String>
  val savingsGoalTotalFlow: Flow<Double>
  val savingsMonthlyContributionFlow: Flow<Double>
  val lastDriveSyncTimestampFlow: Flow<Long>
  val isDriveConnectedFlow: Flow<Boolean>
  val driveAccountEmailFlow: Flow<String?>
  val isSyncingDriveFlow: Flow<Boolean>
  val partnerDriveFileIdFlow: Flow<String?>
  val partnerNameFlow: Flow<String?>
  val partnerEmailFlow: Flow<String?>
  val partnerLastSyncTimestampFlow: Flow<Long>
  val isPartnerPairedFlow: Flow<Boolean>
  val sharedMembersFlow: Flow<List<com.app.spent.data.sync.SharedMemberInfo>>
  val imageStorageLocationFlow: Flow<String>

  suspend fun connectGoogleDrive(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount): com.app.spent.data.sync.DriveConnectResult
  suspend fun disconnectGoogleDrive()
  suspend fun syncToGoogleDrive(): Result<Boolean>
  fun triggerAutoSync()
  suspend fun getOwnBackupFileId(): Result<String?>
  suspend fun enablePublicLinkSharing(fileId: String): Result<String>
  suspend fun processAndSaveImage(sourceUri: android.net.Uri, destinationType: String): Result<String>

  suspend fun addOrUpdateSharedMember(member: com.app.spent.data.sync.SharedMemberInfo)
  suspend fun updateSharedMemberName(fileId: String, newName: String)
  suspend fun updateUserProfileName(newName: String)
  suspend fun removeSharedMember(fileId: String)
  suspend fun clearSharedMembers()

  suspend fun savePartnerInfo(fileId: String, name: String, email: String?)
  suspend fun setPartnerLastSyncTimestamp(timestamp: Long)
  suspend fun clearPartnerInfo()

  suspend fun addTransaction(transaction: TransactionEntity)
  suspend fun updateTransaction(transaction: TransactionEntity)
  suspend fun getTransactionById(id: String): TransactionEntity?
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

  suspend fun addLoan(loan: LoanEntity)
  suspend fun updateLoan(loan: LoanEntity)
  suspend fun deleteLoanById(id: String)
  suspend fun recordLoanPayment(loanId: String, amount: Double)
  suspend fun getLoanById(id: String): LoanEntity?

  suspend fun setWalkthroughCompleted(completed: Boolean)
  suspend fun setDarkThemeMode(enabled: Boolean?)
  suspend fun setCurrencySymbol(symbol: String)
  suspend fun setAppLanguage(languageCode: String?)
  suspend fun setImageStorageLocation(location: String)
  suspend fun setSavingsGoal(name: String, totalGoal: Double, monthlyContribution: Double)
  suspend fun clearSavingsGoal()
  suspend fun setLastDriveSyncTimestamp(timestamp: Long)
  suspend fun restoreAllData(
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    payCycle: PayCycleEntity?,
    recurringRules: List<RecurringRuleEntity>,
    userAccount: UserAccountEntity?,
    loans: List<LoanEntity> = emptyList()
  )
  suspend fun resetAllData(deleteDriveImages: Boolean = false)
}
