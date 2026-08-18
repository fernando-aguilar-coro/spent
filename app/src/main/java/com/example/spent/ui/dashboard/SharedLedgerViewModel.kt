package com.app.spent.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.sync.DriveBackupFileInfo
import com.app.spent.data.sync.GoogleDriveRestService
import com.app.spent.data.sync.HouseholdAggregator
import com.app.spent.data.sync.SharedLedgerData
import com.app.spent.data.sync.SharedLedgerParser
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SharedLedgerViewModel(
  private val repository: SpentRepository,
  private val context: Context
) : BaseViewModel<SharedLedgerUiState, SharedLedgerUiIntent, SharedLedgerUiEffect>(SharedLedgerUiState()) {

  private var localTransactions: List<TransactionEntity> = emptyList()
  private var localCategories: List<CategoryEntity> = emptyList()
  private var localPayCycle: PayCycleEntity? = null
  private var localUserAccount: UserAccountEntity? = null
  private var localCurrencySymbol: String = "$"

  init {
    observeDriveAndPartnerState()
    observeLocalData()
  }

  private fun observeDriveAndPartnerState() {
    viewModelScope.launch {
      val driveStateFlow = combine(
        repository.isDriveConnectedFlow,
        repository.driveAccountEmailFlow,
        repository.isPartnerPairedFlow
      ) { isConnected, email, isPaired ->
        Triple(isConnected, email, isPaired)
      }

      val partnerInfoFlow = combine(
        repository.partnerDriveFileIdFlow,
        repository.partnerNameFlow,
        repository.partnerEmailFlow,
        repository.partnerLastSyncTimestampFlow
      ) { fileId, name, email, lastSync ->
        PartnerInfoState(fileId, name, email, lastSync)
      }

      combine(driveStateFlow, partnerInfoFlow) { (isConnected, email, isPaired), partner ->
        DrivePartnerState(
          isConnected = isConnected,
          email = email,
          isPaired = isPaired,
          partnerFileId = partner.fileId,
          partnerName = partner.name,
          partnerEmail = partner.email,
          lastSync = partner.lastSync
        )
      }.collect { state ->
        setState {
          copy(
            isDriveConnected = state.isConnected,
            driveAccountEmail = state.email,
            isPartnerPaired = state.isPaired,
            partnerName = state.partnerName,
            partnerEmail = state.partnerEmail,
            partnerLastSyncTimestamp = state.lastSync
          )
        }

        if (state.isConnected) {
          fetchDriveFilesAndOwnId()

          // Auto-fetch paired partner data if not loaded or newly paired
          if (state.isPaired && !state.partnerFileId.isNullOrBlank() && currentState.activeLedger == null) {
            fetchByFileId(
              fileId = state.partnerFileId,
              fileName = state.partnerName ?: "Partner Ledger",
              isSilent = true
            )
          }
        }
      }
    }
  }

  private fun observeLocalData() {
    viewModelScope.launch {
      combine(
        repository.getTransactionsFlow(),
        repository.getCategoriesFlow(),
        repository.getCurrentPayCycleFlow(),
        repository.getUserAccountFlow(),
        repository.currencySymbolFlow
      ) { txs, cats, pc, ua, curr ->
        localTransactions = txs
        localCategories = cats
        localPayCycle = pc
        localUserAccount = ua
        localCurrencySymbol = curr
        recalculateHouseholdData()
      }.collect {}
    }
  }

  private fun recalculateHouseholdData() {
    val myName = localUserAccount?.displayName?.ifBlank { "You" } ?: "You"
    val household = HouseholdAggregator.combine(
      localTransactions = localTransactions,
      localCategories = localCategories,
      localPayCycle = localPayCycle,
      currencySymbol = localCurrencySymbol,
      partnerLedger = currentState.activeLedger,
      myDisplayName = myName
    )
    setState { copy(householdData = household) }
  }

  override fun onIntent(intent: SharedLedgerUiIntent) {
    when (intent) {
      is SharedLedgerUiIntent.LoadInitialData -> {
        if (currentState.isDriveConnected) {
          fetchDriveFilesAndOwnId()
          currentState.partnerName?.let { pName ->
            val pId = currentState.ownBackupFileId
            if (currentState.isPartnerPaired) {
              refreshActiveLedger()
            }
          }
        }
      }
      is SharedLedgerUiIntent.SwitchTab -> {
        setState { copy(selectedTab = intent.tab) }
      }
      is SharedLedgerUiIntent.UpdateFileIdInput -> {
        setState { copy(manualFileIdInput = intent.input, errorMessage = null) }
      }
      is SharedLedgerUiIntent.UpdatePartnerEmailInput -> {
        setState { copy(partnerEmailInput = intent.email, errorMessage = null) }
      }
      is SharedLedgerUiIntent.FetchFromInput -> {
        val input = currentState.manualFileIdInput.trim()
        if (input.isNotEmpty()) {
          pairFromInput(input)
        }
      }
      is SharedLedgerUiIntent.LoadFile -> {
        fetchByFileId(intent.fileInfo.id, intent.fileInfo.name, isPairing = true)
      }
      is SharedLedgerUiIntent.RefreshCurrentLedger -> {
        refreshActiveLedger()
      }
      is SharedLedgerUiIntent.LoadSampleDemo -> {
        loadSampleDemo()
      }
      is SharedLedgerUiIntent.ToggleShareGuide -> {
        setState { copy(showShareGuideDialog = intent.show) }
      }
      is SharedLedgerUiIntent.TogglePairPartnerDialog -> {
        setState { copy(showPairPartnerDialog = intent.show, errorMessage = null) }
      }
      is SharedLedgerUiIntent.CopyOwnFileId -> {
        copyOwnFileIdToClipboard()
      }
      is SharedLedgerUiIntent.CopyInviteLink -> {
        generateAndCopyInviteLink()
      }
      is SharedLedgerUiIntent.InvitePartnerByEmail -> {
        invitePartnerByEmail(intent.email)
      }
      is SharedLedgerUiIntent.PairPartnerWithIdOrUrl -> {
        pairFromInput(intent.input)
      }
      is SharedLedgerUiIntent.UnlinkPartner -> {
        unlinkPartner()
      }
    }
  }

  private fun fetchDriveFilesAndOwnId() {
    viewModelScope.launch {
      val account = GoogleDriveRestService.getSignedInAccount(context) ?: return@launch

      // 1. Get own backup file ID
      val ownIdResult = GoogleDriveRestService.getOwnBackupFileId(context, account)
      val ownId = ownIdResult.getOrNull()

      // 2. Search available shared files
      val sharedFilesResult = GoogleDriveRestService.searchSharedBackupFiles(context, account)
      val sharedFiles = sharedFilesResult.getOrNull() ?: emptyList()

      setState {
        copy(
          ownBackupFileId = ownId,
          availableSharedFiles = sharedFiles
        )
      }
    }
  }

  private fun pairFromInput(input: String) {
    // Check if input is a JSON string directly
    if (input.startsWith("{") && input.endsWith("}")) {
      val parseResult = SharedLedgerParser.parse(input, fileName = "Pasted JSON Ledger")
      if (parseResult.isSuccess) {
        val ledger = parseResult.getOrNull()
        setState {
          copy(
            activeLedger = ledger,
            errorMessage = null,
            manualFileIdInput = "",
            showPairPartnerDialog = false
          )
        }
        recalculateHouseholdData()
        sendEffect(SharedLedgerUiEffect.ShowSnackbar("Loaded shared ledger for ${ledger?.ownerName}"))
        return
      }
    }

    val (extractedId, parsedName, parsedEmail) = parseInviteLinkOrId(input)
    if (extractedId.isBlank()) {
      setState { copy(errorMessage = "Invalid Link or Drive File ID") }
      return
    }

    fetchByFileId(
      fileId = extractedId,
      fileName = parsedName ?: "Shared Partner Ledger",
      isPairing = true,
      partnerEmail = parsedEmail
    )
  }

  private fun fetchByFileId(
    fileId: String,
    fileName: String,
    isPairing: Boolean = false,
    isSilent: Boolean = false,
    partnerEmail: String? = null
  ) {
    viewModelScope.launch {
      val account = GoogleDriveRestService.getSignedInAccount(context)
      if (account == null) {
        if (!isSilent) {
          setState {
            copy(
              errorMessage = "Google Drive is not connected. Please connect in Settings."
            )
          }
          sendEffect(SharedLedgerUiEffect.ShowSnackbar("Google Drive is not connected"))
        }
        return@launch
      }

      if (!isSilent) {
        setState { copy(isLoading = true, errorMessage = null) }
      }

      val downloadResult = GoogleDriveRestService.downloadFileById(context, account, fileId)
      if (downloadResult.isSuccess) {
        val json = downloadResult.getOrNull() ?: ""
        val parseResult = SharedLedgerParser.parse(json, fileId = fileId, fileName = fileName)
        if (parseResult.isSuccess) {
          val ledger = parseResult.getOrNull()
          val ownerName = ledger?.ownerName ?: fileName

          if (isPairing) {
            repository.savePartnerInfo(fileId, ownerName, partnerEmail ?: ledger?.ownerRole)
          }
          repository.setPartnerLastSyncTimestamp(System.currentTimeMillis())

          setState {
            copy(
              activeLedger = ledger,
              isLoading = false,
              errorMessage = null,
              manualFileIdInput = "",
              showPairPartnerDialog = false
            )
          }
          recalculateHouseholdData()
          if (!isSilent) {
            sendEffect(SharedLedgerUiEffect.ShowSnackbar("Synced with $ownerName"))
          }
        } else {
          val err = parseResult.exceptionOrNull()?.localizedMessage ?: "Invalid Spent JSON format"
          setState { copy(isLoading = false, errorMessage = err) }
          if (!isSilent) {
            sendEffect(SharedLedgerUiEffect.ShowSnackbar("Error: $err"))
          }
        }
      } else {
        val err = downloadResult.exceptionOrNull()?.localizedMessage ?: "File not found or permission denied"
        setState { copy(isLoading = false, errorMessage = err) }
        if (!isSilent) {
          sendEffect(SharedLedgerUiEffect.ShowSnackbar("Drive error: $err"))
        }
      }
    }
  }

  private fun refreshActiveLedger() {
    val active = currentState.activeLedger
    val fileId = active?.sourceFileId
    viewModelScope.launch {
      val partnerFileId = repository.partnerDriveFileIdFlow.firstOrNull() ?: fileId
      if (partnerFileId != null && partnerFileId != "sample_demo_id") {
        fetchByFileId(partnerFileId, currentState.partnerName ?: "Partner Ledger", isSilent = false)
      } else if (active?.sourceFileId == "sample_demo_id") {
        loadSampleDemo()
      } else {
        sendEffect(SharedLedgerUiEffect.ShowSnackbar("No partner paired yet"))
      }
    }
  }

  private fun invitePartnerByEmail(partnerEmail: String) {
    val email = partnerEmail.trim()
    if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      setState { copy(errorMessage = "Please enter a valid Google email address") }
      return
    }

    viewModelScope.launch {
      val account = GoogleDriveRestService.getSignedInAccount(context)
      if (account == null) {
        setState { copy(errorMessage = "Google Drive is not connected. Connect in Settings first.") }
        return@launch
      }

      setState { copy(isSharingWithEmail = true, errorMessage = null) }

      // Make sure we have our own backup file ID
      var fileId = currentState.ownBackupFileId
      if (fileId.isNullOrBlank()) {
        val syncRes = repository.syncToGoogleDrive()
        if (syncRes.isSuccess) {
          fileId = GoogleDriveRestService.getOwnBackupFileId(context, account).getOrNull()
        }
      }

      if (fileId.isNullOrBlank()) {
        setState { copy(isSharingWithEmail = false, errorMessage = "Could not find your backup file. Please backup your data first.") }
        return@launch
      }

      val shareResult = GoogleDriveRestService.shareFileWithEmail(context, account, fileId, email)
      setState { copy(isSharingWithEmail = false) }

      if (shareResult.isSuccess) {
        // Also auto-save as paired partner email reference
        val inviteLink = buildInviteLink(fileId, localUserAccount?.displayName ?: "Partner", account.email)
        copyToClipboard("Spent Pair Link", inviteLink)
        setState {
          copy(
            showPairPartnerDialog = false,
            partnerEmailInput = ""
          )
        }
        sendEffect(SharedLedgerUiEffect.ShowSnackbar("Access granted to $email! Pair link copied to clipboard."))
      } else {
        val err = shareResult.exceptionOrNull()?.localizedMessage ?: "Failed to grant Drive permission"
        setState { copy(errorMessage = err) }
      }
    }
  }

  private fun generateAndCopyInviteLink() {
    viewModelScope.launch {
      val account = GoogleDriveRestService.getSignedInAccount(context)
      val fileId = currentState.ownBackupFileId ?: run {
        if (account != null) {
          repository.syncToGoogleDrive()
          GoogleDriveRestService.getOwnBackupFileId(context, account).getOrNull()
        } else null
      }

      if (fileId != null) {
        val myName = localUserAccount?.displayName?.ifBlank { "Spent User" } ?: "Spent User"
        val inviteLink = buildInviteLink(fileId, myName, account?.email)
        copyToClipboard("Spent Household Pair Link", inviteLink)
        sendEffect(SharedLedgerUiEffect.ShowSnackbar("Household Invite Link copied to clipboard!"))
      } else {
        sendEffect(SharedLedgerUiEffect.ShowSnackbar("No Drive backup found yet. Sync your data to Drive first."))
      }
    }
  }

  private fun unlinkPartner() {
    viewModelScope.launch {
      repository.clearPartnerInfo()
      setState {
        copy(
          isPartnerPaired = false,
          partnerName = null,
          partnerEmail = null,
          partnerLastSyncTimestamp = 0L,
          activeLedger = null
        )
      }
      recalculateHouseholdData()
      sendEffect(SharedLedgerUiEffect.ShowSnackbar("Unlinked partner successfully"))
    }
  }

  private fun loadSampleDemo() {
    val sampleJson = SharedLedgerParser.generateSampleLedgerJson()
    val parseResult = SharedLedgerParser.parse(sampleJson, fileId = "sample_demo_id", fileName = "Demo Shared Ledger")
    if (parseResult.isSuccess) {
      val ledger = parseResult.getOrNull()
      setState {
        copy(
          activeLedger = ledger,
          errorMessage = null,
          isLoading = false
        )
      }
      recalculateHouseholdData()
      sendEffect(SharedLedgerUiEffect.ShowSnackbar("Loaded sample demo: ${ledger?.ownerName}"))
    }
  }

  private fun copyOwnFileIdToClipboard() {
    val fileId = currentState.ownBackupFileId
    if (!fileId.isNullOrBlank()) {
      copyToClipboard("Spent Drive Backup File ID", fileId)
      sendEffect(SharedLedgerUiEffect.ShowSnackbar("Drive File ID copied to clipboard!"))
    } else {
      sendEffect(SharedLedgerUiEffect.ShowSnackbar("No Drive backup found yet. Sync your data to Drive first."))
    }
  }

  private fun copyToClipboard(label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
  }

  private fun buildInviteLink(fileId: String, name: String, email: String?): String {
    val encodedName = Uri.encode(name)
    val encodedEmail = if (email != null) Uri.encode(email) else ""
    return "spent://pair?fileId=$fileId&name=$encodedName&email=$encodedEmail"
  }

  private fun parseInviteLinkOrId(input: String): Triple<String, String?, String?> {
    val trimmed = input.trim()
    if (trimmed.startsWith("spent://pair")) {
      try {
        val uri = Uri.parse(trimmed)
        val fileId = uri.getQueryParameter("fileId") ?: ""
        val name = uri.getQueryParameter("name")
        val email = uri.getQueryParameter("email")
        return Triple(fileId, name, email)
      } catch (e: Exception) {
        // Fallback to extraction
      }
    }

    // Check standard Drive URL regex
    val idFromPathRegex = Regex("""/d/([a-zA-Z0-9_-]+)""")
    val matchPath = idFromPathRegex.find(trimmed)
    if (matchPath != null && matchPath.groupValues.size > 1) {
      return Triple(matchPath.groupValues[1], null, null)
    }

    val idFromQueryRegex = Regex("""id=([a-zA-Z0-9_-]+)""")
    val matchQuery = idFromQueryRegex.find(trimmed)
    if (matchQuery != null && matchQuery.groupValues.size > 1) {
      return Triple(matchQuery.groupValues[1], null, null)
    }

    return Triple(trimmed, null, null)
  }

  private data class PartnerInfoState(
    val fileId: String?,
    val name: String?,
    val email: String?,
    val lastSync: Long
  )

  private data class DrivePartnerState(
    val isConnected: Boolean,
    val email: String?,
    val isPaired: Boolean,
    val partnerFileId: String?,
    val partnerName: String?,
    val partnerEmail: String?,
    val lastSync: Long
  )
}

