package com.app.spent.ui.settings

import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState
data class SettingsUiState(
val currentPayCycle: PayCycleEntity? = null,
val isDarkThemeOverride: Boolean? = null,
val currencySymbol: String = "$",
val appLanguage: String? = null,
val transactions: List<TransactionEntity> = emptyList(),
val categories: List<CategoryEntity> = emptyList(),
val lastDriveSyncTimestamp: Long = 0L,
val isDriveConnected: Boolean = false,
val driveAccountEmail: String? = null,
val isDriveSyncing: Boolean = false,
val isLoading: Boolean = false
) : UiState

sealed class SettingsUiIntent : UiIntent {
  data class SavePayCycle(val frequency: String, val income: Double, val startDate: Long) : SettingsUiIntent()
  data class SetDarkThemeMode(val isDark: Boolean?) : SettingsUiIntent()
  data class SetCurrencySymbol(val symbol: String) : SettingsUiIntent()
  data class SetAppLanguage(val languageCode: String?) : SettingsUiIntent()
  data class ConnectDriveAccount(val account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) : SettingsUiIntent()
  object DisconnectDrive : SettingsUiIntent()
  object SyncDriveNow : SettingsUiIntent()
  object RequestDriveSignIn : SettingsUiIntent()
  data class NotifySyncMessage(val message: String) : SettingsUiIntent()
  object ResetAllData : SettingsUiIntent()
}

sealed class SettingsUiEffect : UiEffect {
  data class ShowSnackbar(val message: String) : SettingsUiEffect()
  object LaunchDriveSignIn : SettingsUiEffect()
}
