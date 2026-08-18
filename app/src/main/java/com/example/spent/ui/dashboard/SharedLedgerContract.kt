package com.app.spent.ui.dashboard

import com.app.spent.data.sync.DriveBackupFileInfo
import com.app.spent.data.sync.SharedLedgerData
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState

data class SharedLedgerUiState(
  val isLoading: Boolean = false,
  val isDriveConnected: Boolean = false,
  val driveAccountEmail: String? = null,
  val ownBackupFileId: String? = null,
  val activeLedger: SharedLedgerData? = null,
  val availableSharedFiles: List<DriveBackupFileInfo> = emptyList(),
  val manualFileIdInput: String = "",
  val errorMessage: String? = null,
  val showShareGuideDialog: Boolean = false,
  val isRefreshing: Boolean = false
) : UiState

sealed class SharedLedgerUiIntent : UiIntent {
  object LoadInitialData : SharedLedgerUiIntent()
  data class UpdateFileIdInput(val input: String) : SharedLedgerUiIntent()
  object FetchFromInput : SharedLedgerUiIntent()
  data class LoadFile(val fileInfo: DriveBackupFileInfo) : SharedLedgerUiIntent()
  object RefreshCurrentLedger : SharedLedgerUiIntent()
  object LoadSampleDemo : SharedLedgerUiIntent()
  data class ToggleShareGuide(val show: Boolean) : SharedLedgerUiIntent()
  object CopyOwnFileId : SharedLedgerUiIntent()
}

sealed class SharedLedgerUiEffect : UiEffect {
  data class ShowSnackbar(val message: String) : SharedLedgerUiEffect()
  object NavigateBack : SharedLedgerUiEffect()
}
