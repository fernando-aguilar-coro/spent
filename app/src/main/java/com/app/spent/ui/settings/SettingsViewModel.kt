package com.app.spent.ui.settings

import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
class SettingsViewModel(
private val repository: SpentRepository
) : BaseViewModel<SettingsUiState, SettingsUiIntent, SettingsUiEffect>(SettingsUiState()) {

  init {
    observeSettingsData()
  }

  private fun observeSettingsData() {
    viewModelScope.launch {
      val prefs1Flow = combine(
      repository.getCurrentPayCycleFlow(),
      repository.isDarkThemeFlow,
      repository.currencySymbolFlow
      ) { payCycle, isDark, currency ->
        Triple(payCycle, isDark, currency)
      }

      val prefs2Flow = combine(
      repository.appLanguageFlow,
      repository.imageStorageLocationFlow,
      repository.lastDriveSyncTimestampFlow
      ) { language, imageStorage, lastSync ->
        Triple(language, imageStorage, lastSync)
      }

      val coreFlow = combine(prefs1Flow, prefs2Flow) { (payCycle, isDark, currency), (language, imageStorage, lastSync) ->
        SettingsCoreData(payCycle, isDark, currency, language, imageStorage, lastSync)
      }

      val driveFlow = combine(
      repository.isDriveConnectedFlow,
      repository.driveAccountEmailFlow,
      repository.isSyncingDriveFlow
      ) { isConnected, email, isSyncing ->
        Triple(isConnected, email, isSyncing)
      }

      val dataFlow = combine(
      repository.getTransactionsFlow(),
      repository.getCategoriesFlow()
      ) { transactions, categories ->
        Pair(transactions, categories)
      }

      combine(coreFlow, driveFlow, dataFlow) { core, (isConnected, email, isSyncing), (transactions, categories) ->
        SettingsUiState(
        currentPayCycle = core.payCycle,
        isDarkThemeOverride = core.isDark,
        currencySymbol = core.currency,
        appLanguage = core.language,
        imageStorageLocation = core.imageStorageLocation,
        transactions = transactions,
        categories = categories,
        lastDriveSyncTimestamp = core.lastSync,
        isDriveConnected = isConnected,
        driveAccountEmail = email,
        isDriveSyncing = isSyncing,
        isLoading = false
        )
      }.collect { newState ->
        setState { newState }
      }
    }
  }

  private data class SettingsCoreData(
  val payCycle: PayCycleEntity?,
  val isDark: Boolean?,
  val currency: String,
  val language: String?,
  val imageStorageLocation: String,
  val lastSync: Long
  )

  override fun onIntent(intent: SettingsUiIntent) {
    when (intent) {
      is SettingsUiIntent.SavePayCycle -> savePayCycle(intent.frequency, intent.income, intent.startDate)
      is SettingsUiIntent.SetDarkThemeMode -> setThemeMode(intent.isDark)
      is SettingsUiIntent.SetCurrencySymbol -> setCurrencySymbol(intent.symbol)
      is SettingsUiIntent.SetAppLanguage -> setAppLanguage(intent.languageCode)
      is SettingsUiIntent.SetImageStorageLocation -> setImageStorageLocation(intent.location)
      is SettingsUiIntent.ConnectDriveAccount -> connectDriveAccount(intent.account)
      is SettingsUiIntent.ResolveSyncConflict -> resolveSyncConflict(intent.choice)
      is SettingsUiIntent.DismissSyncConflict -> dismissSyncConflict()
      is SettingsUiIntent.DisconnectDrive -> disconnectDrive()
      is SettingsUiIntent.SyncDriveNow -> syncDriveNow()
      is SettingsUiIntent.RequestDriveSignIn -> sendEffect(SettingsUiEffect.LaunchDriveSignIn)
      is SettingsUiIntent.NotifySyncMessage -> sendEffect(SettingsUiEffect.ShowSnackbar(intent.message))
      is SettingsUiIntent.ResetAllData -> resetAllData(intent.deleteDriveImages)
    }
  }

  private fun connectDriveAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
    viewModelScope.launch {
      val result = repository.connectGoogleDrive(account)
      when (result) {
        is com.app.spent.data.sync.DriveConnectResult.ConflictDetected -> {
          setState { copy(syncConflict = result.conflictData) }
        }
        is com.app.spent.data.sync.DriveConnectResult.RestoredFromCloud -> {
          sendEffect(SettingsUiEffect.ShowSnackbar("Data restored from Google Drive successfully"))
        }
        is com.app.spent.data.sync.DriveConnectResult.ConnectedNew -> {
          sendEffect(SettingsUiEffect.ShowSnackbar("Google Drive connected"))
        }
        is com.app.spent.data.sync.DriveConnectResult.Error -> {
          sendEffect(SettingsUiEffect.ShowSnackbar(result.message))
        }
      }
    }
  }

  private fun resolveSyncConflict(choice: com.app.spent.data.sync.SyncConflictChoice) {
    val conflict = currentState.syncConflict ?: return
    viewModelScope.launch {
      val result = repository.resolveDriveConflict(
        account = conflict.account,
        choice = choice,
        cloudBackupJson = conflict.cloudBackupJson
      )
      setState { copy(syncConflict = null) }
      when (result) {
        is com.app.spent.data.sync.DriveConnectResult.RestoredFromCloud -> {
          val msg = if (choice == com.app.spent.data.sync.SyncConflictChoice.MERGE) {
            "Data merged and synchronized with Google Drive"
          } else {
            "Data restored from Google Drive successfully"
          }
          sendEffect(SettingsUiEffect.ShowSnackbar(msg))
        }
        is com.app.spent.data.sync.DriveConnectResult.ConnectedNew -> {
          sendEffect(SettingsUiEffect.ShowSnackbar("Local data preserved and uploaded to Google Drive"))
        }
        is com.app.spent.data.sync.DriveConnectResult.Error -> {
          sendEffect(SettingsUiEffect.ShowSnackbar(result.message))
        }
        else -> {}
      }
    }
  }

  private fun dismissSyncConflict() {
    viewModelScope.launch {
      repository.cancelDriveConflict()
      setState { copy(syncConflict = null) }
      sendEffect(SettingsUiEffect.ShowSnackbar("Sync cancelled, local data unchanged"))
    }
  }

  private fun disconnectDrive() {
    viewModelScope.launch {
      repository.disconnectGoogleDrive()
      sendEffect(SettingsUiEffect.ShowSnackbar("Google Drive disconnected"))
    }
  }

  private fun syncDriveNow() {
    viewModelScope.launch {
      val result = repository.syncToGoogleDrive()
      if (result.isSuccess) {
        sendEffect(SettingsUiEffect.ShowSnackbar("Google Drive synchronized successfully"))
      } else {
        val err = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
        sendEffect(SettingsUiEffect.ShowSnackbar("Error syncing: $err"))
      }
    }
  }

  private fun savePayCycle(frequency: String, income: Double, startDate: Long) {
    viewModelScope.launch {
      val current = currentState.currentPayCycle ?: PayCycleEntity()
      val updated = current.copy(
      frequency = frequency,
      income = income,
      startDate = startDate
      )
      repository.setPayCycle(updated)
      sendEffect(SettingsUiEffect.ShowSnackbar("Pay cycle updated"))
    }
  }

  private fun setThemeMode(isDark: Boolean?) {
    viewModelScope.launch {
      repository.setDarkThemeMode(isDark)
    }
  }

  private fun setCurrencySymbol(symbol: String) {
    viewModelScope.launch {
      repository.setCurrencySymbol(symbol)
      sendEffect(SettingsUiEffect.ShowSnackbar("Currency updated to $symbol"))
    }
  }

  private fun setAppLanguage(languageCode: String?) {
    viewModelScope.launch {
      repository.setAppLanguage(languageCode)
    }
  }

  private fun setImageStorageLocation(location: String) {
    viewModelScope.launch {
      repository.setImageStorageLocation(location)
      sendEffect(SettingsUiEffect.ShowSnackbar("Image storage location updated"))
    }
  }

  private fun resetAllData(deleteDriveImages: Boolean) {
    viewModelScope.launch {
      repository.resetAllData(deleteDriveImages)
      sendEffect(SettingsUiEffect.ShowSnackbar("All data has been reset"))
    }
  }
}
