package com.app.spent.ui.dashboard

import com.app.spent.data.sync.DriveBackupFileInfo
import com.app.spent.data.sync.HouseholdUnifiedData
import com.app.spent.data.sync.SharedLedgerData
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState

enum class HouseholdTab {
  HOUSEHOLD, // Combined View
  ME,        // Personal View
  PARTNER    // Partner View
}

data class SharedLedgerUiState(
  val isLoading: Boolean = false,
  val isRefreshing: Boolean = false,
  val isDriveConnected: Boolean = false,
  val driveAccountEmail: String? = null,
  val ownBackupFileId: String? = null,
  val isPartnerPaired: Boolean = false,
  val partnerName: String? = null,
  val partnerEmail: String? = null,
  val partnerLastSyncTimestamp: Long = 0L,
  val activeLedger: SharedLedgerData? = null,
  val householdData: HouseholdUnifiedData? = null,
  val selectedTab: HouseholdTab = HouseholdTab.HOUSEHOLD,
  val availableSharedFiles: List<DriveBackupFileInfo> = emptyList(),
  val manualFileIdInput: String = "",
  val partnerEmailInput: String = "",
  val errorMessage: String? = null,
  val showShareGuideDialog: Boolean = false,
  val showPairPartnerDialog: Boolean = false,
  val isSharingWithEmail: Boolean = false
) : UiState

sealed class SharedLedgerUiIntent : UiIntent {
  object LoadInitialData : SharedLedgerUiIntent()
  data class SwitchTab(val tab: HouseholdTab) : SharedLedgerUiIntent()
  data class UpdateFileIdInput(val input: String) : SharedLedgerUiIntent()
  data class UpdatePartnerEmailInput(val email: String) : SharedLedgerUiIntent()
  object FetchFromInput : SharedLedgerUiIntent()
  data class LoadFile(val fileInfo: DriveBackupFileInfo) : SharedLedgerUiIntent()
  object RefreshCurrentLedger : SharedLedgerUiIntent()
  object LoadSampleDemo : SharedLedgerUiIntent()
  data class ToggleShareGuide(val show: Boolean) : SharedLedgerUiIntent()
  data class TogglePairPartnerDialog(val show: Boolean) : SharedLedgerUiIntent()
  object CopyOwnFileId : SharedLedgerUiIntent()
  object CopyInviteLink : SharedLedgerUiIntent()
  data class InvitePartnerByEmail(val email: String) : SharedLedgerUiIntent()
  data class PairPartnerWithIdOrUrl(val input: String) : SharedLedgerUiIntent()
  object UnlinkPartner : SharedLedgerUiIntent()
}

sealed class SharedLedgerUiEffect : UiEffect {
  data class ShowSnackbar(val message: String) : SharedLedgerUiEffect()
  object NavigateBack : SharedLedgerUiEffect()
}
