package com.app.spent.ui.dashboard

import com.app.spent.data.sync.DriveBackupFileInfo
import com.app.spent.data.sync.SharedMemberInfo
import com.app.spent.data.sync.SharedUnifiedData
import com.app.spent.data.sync.SharedLedgerData
import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState

enum class SharedFinancesTab {
  STATISTICS,  // 📊 Statistics / Estadísticas
  MEMBERS,     // 👥 Members Panel / Miembros
  INVITE_JOIN  // 🔗 Invite & Join / Invitar y Unirse
}

typealias HouseholdTab = SharedFinancesTab

data class SharedLedgerUiState(
  val isLoading: Boolean = false,
  val isRefreshing: Boolean = false,
  val isDriveConnected: Boolean = false,
  val driveAccountEmail: String? = null,
  val ownBackupFileId: String? = null,
  val ownShareWebLink: String? = null,
  val isGeneratingShareLink: Boolean = false,
  val members: List<SharedMemberInfo> = emptyList(),
  val activeMemberLedgers: Map<String, SharedLedgerData> = emptyMap(),
  val unifiedData: SharedUnifiedData? = null,
  val selectedTab: SharedFinancesTab = SharedFinancesTab.STATISTICS,
  val addMemberInput: String = "",
  val errorMessage: String? = null,
  val showGuideDialog: Boolean = false,
  val showAddMemberDialog: Boolean = false,
  val activeLedger: SharedLedgerData? = null // Legacy compatibility
) : UiState

sealed class SharedLedgerUiIntent : UiIntent {
  object LoadInitialData : SharedLedgerUiIntent()
  data class SwitchTab(val tab: SharedFinancesTab) : SharedLedgerUiIntent()
  data class UpdateAddMemberInput(val input: String) : SharedLedgerUiIntent()
  object ShareMyFinances : SharedLedgerUiIntent()
  object CopyShareLink : SharedLedgerUiIntent()
  data class AddMemberByUrlOrId(val input: String) : SharedLedgerUiIntent()
  data class RemoveMember(val fileId: String) : SharedLedgerUiIntent()
  data class RefreshMember(val fileId: String) : SharedLedgerUiIntent()
  object RefreshAll : SharedLedgerUiIntent()
  object LoadSampleDemo : SharedLedgerUiIntent()
  data class ToggleGuideDialog(val show: Boolean) : SharedLedgerUiIntent()
  data class ToggleAddMemberDialog(val show: Boolean) : SharedLedgerUiIntent()

  // Compatibility aliases
  data class UpdateFileIdInput(val input: String) : SharedLedgerUiIntent()
  object FetchFromInput : SharedLedgerUiIntent()
  data class LoadFile(val fileInfo: DriveBackupFileInfo) : SharedLedgerUiIntent()
  object RefreshCurrentLedger : SharedLedgerUiIntent()
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
