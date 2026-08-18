package com.app.spent.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.sync.DriveBackupFileInfo
import com.app.spent.data.sync.GoogleDriveRestService
import com.app.spent.data.sync.SharedLedgerParser
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SharedLedgerViewModel(
  private val repository: SpentRepository,
  private val context: Context
) : BaseViewModel<SharedLedgerUiState, SharedLedgerUiIntent, SharedLedgerUiEffect>(SharedLedgerUiState()) {

  init {
    observeDriveConnection()
  }

  private fun observeDriveConnection() {
    viewModelScope.launch {
      combine(
        repository.isDriveConnectedFlow,
        repository.driveAccountEmailFlow
      ) { isConnected, email ->
        Pair(isConnected, email)
      }.collect { (isConnected, email) ->
        setState {
          copy(
            isDriveConnected = isConnected,
            driveAccountEmail = email
          )
        }
        if (isConnected) {
          fetchDriveFilesAndOwnId()
        }
      }
    }
  }

  override fun onIntent(intent: SharedLedgerUiIntent) {
    when (intent) {
      is SharedLedgerUiIntent.LoadInitialData -> {
        if (currentState.isDriveConnected) {
          fetchDriveFilesAndOwnId()
        }
      }
      is SharedLedgerUiIntent.UpdateFileIdInput -> {
        setState { copy(manualFileIdInput = intent.input, errorMessage = null) }
      }
      is SharedLedgerUiIntent.FetchFromInput -> {
        val input = currentState.manualFileIdInput.trim()
        if (input.isNotEmpty()) {
          fetchByFileIdOrUrl(input)
        }
      }
      is SharedLedgerUiIntent.LoadFile -> {
        fetchByFileId(intent.fileInfo.id, intent.fileInfo.name)
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
      is SharedLedgerUiIntent.CopyOwnFileId -> {
        copyOwnFileIdToClipboard()
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

  private fun fetchByFileIdOrUrl(input: String) {
    // Check if input is a JSON string directly
    if (input.startsWith("{") && input.endsWith("}")) {
      val parseResult = SharedLedgerParser.parse(input, fileName = "Pasted JSON Ledger")
      if (parseResult.isSuccess) {
        val ledger = parseResult.getOrNull()
        setState {
          copy(
            activeLedger = ledger,
            errorMessage = null,
            manualFileIdInput = ""
          )
        }
        sendEffect(SharedLedgerUiEffect.ShowSnackbar("Loaded shared ledger for ${ledger?.ownerName}"))
        return
      }
    }

    val extractedId = extractFileIdFromUrl(input)
    fetchByFileId(extractedId, "Shared Drive Ledger")
  }

  private fun fetchByFileId(fileId: String, fileName: String) {
    viewModelScope.launch {
      val account = GoogleDriveRestService.getSignedInAccount(context)
      if (account == null) {
        setState {
          copy(
            errorMessage = "Google Drive is not connected. Please connect in Settings."
          )
        }
        sendEffect(SharedLedgerUiEffect.ShowSnackbar("Google Drive is not connected"))
        return@launch
      }

      setState { copy(isLoading = true, errorMessage = null) }

      val downloadResult = GoogleDriveRestService.downloadFileById(context, account, fileId)
      if (downloadResult.isSuccess) {
        val json = downloadResult.getOrNull() ?: ""
        val parseResult = SharedLedgerParser.parse(json, fileId = fileId, fileName = fileName)
        if (parseResult.isSuccess) {
          val ledger = parseResult.getOrNull()
          setState {
            copy(
              activeLedger = ledger,
              isLoading = false,
              errorMessage = null,
              manualFileIdInput = ""
            )
          }
          sendEffect(SharedLedgerUiEffect.ShowSnackbar("Loaded shared ledger for ${ledger?.ownerName}"))
        } else {
          val err = parseResult.exceptionOrNull()?.localizedMessage ?: "Invalid Spent JSON format"
          setState { copy(isLoading = false, errorMessage = err) }
          sendEffect(SharedLedgerUiEffect.ShowSnackbar("Error parsing ledger: $err"))
        }
      } else {
        val err = downloadResult.exceptionOrNull()?.localizedMessage ?: "File not found or permission denied"
        setState { copy(isLoading = false, errorMessage = err) }
        sendEffect(SharedLedgerUiEffect.ShowSnackbar("Drive error: $err"))
      }
    }
  }

  private fun refreshActiveLedger() {
    val active = currentState.activeLedger ?: return
    val fileId = active.sourceFileId
    if (fileId != null) {
      fetchByFileId(fileId, active.sourceFileName ?: "spent_backup.json")
    } else {
      loadSampleDemo()
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
      sendEffect(SharedLedgerUiEffect.ShowSnackbar("Previewing sample shared ledger: ${ledger?.ownerName}"))
    }
  }

  private fun copyOwnFileIdToClipboard() {
    val fileId = currentState.ownBackupFileId
    if (!fileId.isNullOrBlank()) {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText("Spent Drive Backup File ID", fileId)
      clipboard.setPrimaryClip(clip)
      sendEffect(SharedLedgerUiEffect.ShowSnackbar("Drive File ID copied to clipboard!"))
    } else {
      sendEffect(SharedLedgerUiEffect.ShowSnackbar("No Drive backup found yet. Sync your data to Drive first."))
    }
  }

  private fun extractFileIdFromUrl(input: String): String {
    // Regex for standard drive URLs: /d/<id>/ or id=<id>
    val idFromPathRegex = Regex("""/d/([a-zA-Z0-9_-]+)""")
    val matchPath = idFromPathRegex.find(input)
    if (matchPath != null && matchPath.groupValues.size > 1) {
      return matchPath.groupValues[1]
    }

    val idFromQueryRegex = Regex("""id=([a-zA-Z0-9_-]+)""")
    val matchQuery = idFromQueryRegex.find(input)
    if (matchQuery != null && matchQuery.groupValues.size > 1) {
      return matchQuery.groupValues[1]
    }

    return input.trim()
  }
}
