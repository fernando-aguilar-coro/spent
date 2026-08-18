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
sealed class DriveConnectResult {
  data class RestoredFromCloud(val itemsCount: Int) : DriveConnectResult()
  object ConnectedNew : DriveConnectResult()
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
      val email = account.email ?: "Google User"
      preferencesRepository.setDriveAccount(email)

      // Check if there is an existing backup on Google Drive
      val downloadResult = GoogleDriveRestService.downloadBackupFromDrive(context, account)
      if (downloadResult.isSuccess) {
        val jsonString = downloadResult.getOrNull()
        if (!jsonString.isNullOrBlank()) {
          val restoreResult = DriveBackupManager.restoreFromJson(jsonString, repository)
          if (restoreResult.isSuccess) {
            val now = System.currentTimeMillis()
            preferencesRepository.setLastDriveSyncTimestamp(now)
            _isSyncing.value = false
            return@withContext DriveConnectResult.RestoredFromCloud(1)
          }
        }
      }

      // No prior backup found or empty, upload current starter/local data to initialize Drive
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
