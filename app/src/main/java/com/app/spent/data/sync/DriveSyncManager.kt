package com.app.spent.data.sync

import android.content.Context
import android.util.Log
import com.app.spent.data.preferences.UserPreferencesRepository
import com.app.spent.data.repository.SpentRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
enum class SyncConflictChoice {
  MERGE,
  KEEP_LOCAL,
  KEEP_CLOUD
}

data class SyncDataSummary(
  val transactionCount: Int,
  val loanCount: Int,
  val categoryCount: Int,
  val lastModifiedTimestamp: Long
)

data class SyncConflictData(
  val localSummary: SyncDataSummary,
  val cloudSummary: SyncDataSummary,
  val cloudBackupJson: String,
  val account: GoogleSignInAccount
)

sealed class DriveConnectResult {
  data class RestoredFromCloud(val itemsCount: Int) : DriveConnectResult()
  object ConnectedNew : DriveConnectResult()
  data class ConflictDetected(val conflictData: SyncConflictData) : DriveConnectResult()
  data class Error(val message: String) : DriveConnectResult()
}

object DriveSyncManager {

  private const val TAG = "DriveSyncManager"
  private val syncScope = CoroutineScope(Dispatchers.IO)
  private val syncMutex = Mutex()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  suspend fun connectAccount(
  context: Context,
  account: GoogleSignInAccount,
  repository: SpentRepository,
  preferencesRepository: UserPreferencesRepository
  ): DriveConnectResult = withContext(Dispatchers.IO) {
    try {
      _isSyncing.value = true

      // Check if there is an existing backup on Google Drive
      val downloadResult = GoogleDriveRestService.downloadBackupFromDrive(context, account)
      val cloudJson = if (downloadResult.isSuccess) downloadResult.getOrNull() else null
      val hasCloudData = !cloudJson.isNullOrBlank() && DriveBackupManager.hasCloudUserData(cloudJson)
      val hasLocalData = DriveBackupManager.hasLocalUserData(repository)

      // Conflict condition: both local and cloud have active user data
      if (hasLocalData && hasCloudData) {
        _isSyncing.value = false
        val localSummary = DriveBackupManager.extractLocalSummary(repository)
        val cloudSummary = DriveBackupManager.extractCloudSummary(cloudJson!!)
        return@withContext DriveConnectResult.ConflictDetected(
          conflictData = SyncConflictData(
            localSummary = localSummary,
            cloudSummary = cloudSummary,
            cloudBackupJson = cloudJson,
            account = account
          )
        )
      }

      val email = account.email ?: "Google User"
      preferencesRepository.setDriveAccount(email)

      // If only cloud has user data, restore from cloud
      if (hasCloudData) {
        val restoreResult = DriveBackupManager.restoreFromJson(cloudJson!!, repository)
        if (restoreResult.isSuccess) {
          val now = System.currentTimeMillis()
          preferencesRepository.setLastDriveSyncTimestamp(now)
          _isSyncing.value = false
          return@withContext DriveConnectResult.RestoredFromCloud(1)
        }
      }

      // If only local data exists or both are clean, upload current/starter data to initialize Drive
      val backupJson = DriveBackupManager.generateBackupJson(repository)
      val uploadResult = GoogleDriveRestService.uploadBackupToDrive(context, account, backupJson)
      if (uploadResult.isSuccess) {
        val now = System.currentTimeMillis()
        preferencesRepository.setLastDriveSyncTimestamp(now)
      }

      _isSyncing.value = false
      DriveConnectResult.ConnectedNew
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting Drive account", e)
      _isSyncing.value = false
      DriveConnectResult.Error(e.localizedMessage ?: "Unknown connection error")
    }
  }

  suspend fun resolveConflict(
  context: Context,
  account: GoogleSignInAccount,
  repository: SpentRepository,
  preferencesRepository: UserPreferencesRepository,
  choice: SyncConflictChoice,
  cloudBackupJson: String
  ): DriveConnectResult = withContext(Dispatchers.IO) {
    try {
      _isSyncing.value = true
      val email = account.email ?: "Google User"
      preferencesRepository.setDriveAccount(email)

      when (choice) {
        SyncConflictChoice.MERGE -> {
          val mergeResult = DriveBackupManager.mergeCloudAndLocal(cloudBackupJson, repository)
          if (mergeResult.isFailure) {
            _isSyncing.value = false
            return@withContext DriveConnectResult.Error(
              mergeResult.exceptionOrNull()?.localizedMessage ?: "Failed to merge cloud and local data"
            )
          }
          val mergedJson = DriveBackupManager.generateBackupJson(repository)
          val uploadResult = GoogleDriveRestService.uploadBackupToDrive(context, account, mergedJson)
          val now = System.currentTimeMillis()
          preferencesRepository.setLastDriveSyncTimestamp(now)
          _isSyncing.value = false
          DriveConnectResult.RestoredFromCloud(1)
        }
        SyncConflictChoice.KEEP_LOCAL -> {
          val localJson = DriveBackupManager.generateBackupJson(repository)
          val uploadResult = GoogleDriveRestService.uploadBackupToDrive(context, account, localJson)
          val now = System.currentTimeMillis()
          preferencesRepository.setLastDriveSyncTimestamp(now)
          _isSyncing.value = false
          DriveConnectResult.ConnectedNew
        }
        SyncConflictChoice.KEEP_CLOUD -> {
          val restoreResult = DriveBackupManager.restoreFromJson(cloudBackupJson, repository)
          if (restoreResult.isFailure) {
            _isSyncing.value = false
            return@withContext DriveConnectResult.Error(
              restoreResult.exceptionOrNull()?.localizedMessage ?: "Failed to restore cloud backup"
            )
          }
          val now = System.currentTimeMillis()
          preferencesRepository.setLastDriveSyncTimestamp(now)
          _isSyncing.value = false
          DriveConnectResult.RestoredFromCloud(1)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error resolving sync conflict", e)
      _isSyncing.value = false
      DriveConnectResult.Error(e.localizedMessage ?: "Unknown resolution error")
    }
  }

  suspend fun cancelConflict(
  context: Context,
  preferencesRepository: UserPreferencesRepository
  ) = withContext(Dispatchers.IO) {
    try {
      GoogleDriveRestService.signOut(context)
      preferencesRepository.clearDriveAccount()
    } catch (e: Exception) {
      Log.e(TAG, "Error cancelling sync conflict", e)
    }
  }

  suspend fun disconnectAccount(
  context: Context,
  preferencesRepository: UserPreferencesRepository
  ) = withContext(Dispatchers.IO) {
    try {
      GoogleDriveRestService.signOut(context)
      preferencesRepository.clearDriveAccount()
    } catch (e: Exception) {
      Log.e(TAG, "Error disconnecting Drive account", e)
    }
  }

  suspend fun syncNow(
  context: Context,
  repository: SpentRepository,
  preferencesRepository: UserPreferencesRepository
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    syncMutex.withLock {
      try {
        val isConnected = preferencesRepository.isDriveConnectedFlow.firstOrNull() ?: false
        val account = GoogleDriveRestService.getSignedInAccount(context)
        if (!isConnected || account == null) {
          return@withContext Result.failure(Exception("Google Drive is not connected"))
        }

        _isSyncing.value = true
        val backupJson = DriveBackupManager.generateBackupJson(repository)
        val uploadResult = GoogleDriveRestService.uploadBackupToDrive(context, account, backupJson)

        _isSyncing.value = false
        if (uploadResult.isSuccess) {
          val now = System.currentTimeMillis()
          preferencesRepository.setLastDriveSyncTimestamp(now)
          Result.success(true)
        } else {
          Result.failure(uploadResult.exceptionOrNull() ?: Exception("Failed to upload backup"))
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error syncing to Drive", e)
        _isSyncing.value = false
        Result.failure(e)
      }
    }
  }

  fun triggerAutoSync(
  context: Context,
  repository: SpentRepository,
  preferencesRepository: UserPreferencesRepository
  ) {
    syncScope.launch {
      try {
        val isConnected = preferencesRepository.isDriveConnectedFlow.firstOrNull() ?: false
        if (!isConnected) return@launch

        val account = GoogleDriveRestService.getSignedInAccount(context) ?: return@launch

        syncMutex.withLock {
          _isSyncing.value = true
          val backupJson = DriveBackupManager.generateBackupJson(repository)
          val uploadResult = GoogleDriveRestService.uploadBackupToDrive(context, account, backupJson)
          _isSyncing.value = false

          if (uploadResult.isSuccess) {
            val now = System.currentTimeMillis()
            preferencesRepository.setLastDriveSyncTimestamp(now)
            Log.d(TAG, "Auto-sync to Google Drive completed successfully")
          } else {
            Log.w(TAG, "Auto-sync to Google Drive failed: ${uploadResult.exceptionOrNull()?.message}")
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Auto-sync exception", e)
        _isSyncing.value = false
      }
    }
  }
}
