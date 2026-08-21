package com.app.spent.ui.sharedledger

import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.sync.DriveBackupFileInfo
import com.app.spent.data.sync.GoogleDriveRestService
import com.app.spent.data.sync.SharedFinancesAggregator
import com.app.spent.data.sync.SharedLedgerData
import com.app.spent.data.sync.SharedLedgerParser
import com.app.spent.data.sync.SharedMemberInfo
import com.app.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SharedLedgerViewModel(
    private val repository: SpentRepository
) : BaseViewModel<SharedLedgerUiState, SharedLedgerUiIntent, SharedLedgerUiEffect>(SharedLedgerUiState()) {

    private var localTransactions: List<TransactionEntity> = emptyList()
    private var localCategories: List<CategoryEntity> = emptyList()
    private var localPayCycle: PayCycleEntity? = null
    private var localUserAccount: UserAccountEntity? = null
    private var localCurrencySymbol: String = "$"
    private val memberLedgerCache = mutableMapOf<String, SharedLedgerData>()

    init {
        observeDriveAndMembers()
        observeLocalData()
    }

    // =========================================================================
    // Observation & Aggregation Pipeline
    // =========================================================================

    private fun observeDriveAndMembers() {
        viewModelScope.launch {
            val driveFlow = combine(
                repository.isDriveConnectedFlow,
                repository.driveAccountEmailFlow
            ) { isConnected, email -> Pair(isConnected, email) }

            combine(driveFlow, repository.sharedMembersFlow) { (isConnected, email), members ->
                Triple(isConnected, email, members)
            }.collect { (isConnected, email, members) ->
                setState {
                    copy(
                        isDriveConnected = isConnected,
                        driveAccountEmail = email,
                        members = members
                    )
                }

                if (isConnected) {
                    fetchOwnBackupFileId()
                    fetchRemoteMemberLedgers(members, isSilent = true)
                } else {
                    recalculateUnifiedData()
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
                recalculateUnifiedData()
            }.collect {}
        }
    }

    private fun recalculateUnifiedData() {
        val myName = localUserAccount?.displayName?.ifBlank { "You" } ?: "You"
        val ledgersList = memberLedgerCache.values.toList()
        val unified = SharedFinancesAggregator.combine(
            localTransactions = localTransactions,
            localCategories = localCategories,
            localPayCycle = localPayCycle,
            currencySymbol = localCurrencySymbol,
            memberLedgers = ledgersList,
            myDisplayName = myName,
            knownMembers = currentState.members
        )
        setState {
            copy(
                unifiedData = unified,
                activeMemberLedgers = memberLedgerCache.toMap(),
                activeLedger = ledgersList.firstOrNull()
            )
        }
    }

    // =========================================================================
    // Intent Dispatcher
    // =========================================================================

    override fun onIntent(intent: SharedLedgerUiIntent) {
        when (intent) {
            is SharedLedgerUiIntent.LoadInitialData -> handleInitialLoad()
            is SharedLedgerUiIntent.SwitchTab -> setState { copy(selectedTab = intent.tab) }
            is SharedLedgerUiIntent.UpdateAddMemberInput -> setState { copy(addMemberInput = intent.input, errorMessage = null) }
            is SharedLedgerUiIntent.ShareMyFinances -> shareMyFinancesViaDrive()
            is SharedLedgerUiIntent.CopyShareLink -> handleCopyShareLink()
            is SharedLedgerUiIntent.AddMemberByUrlOrId -> addMemberFromUrlOrId(intent.input)
            is SharedLedgerUiIntent.RemoveMember -> removeMember(intent.fileId)
            is SharedLedgerUiIntent.RefreshMember -> refreshSingleMember(intent.fileId)
            is SharedLedgerUiIntent.ToggleEditMemberDialog -> setState { copy(editingMember = intent.member) }
            is SharedLedgerUiIntent.UpdateMemberName -> updateMemberName(intent.fileId, intent.newName)
            is SharedLedgerUiIntent.RefreshAll -> fetchRemoteMemberLedgers(currentState.members, isSilent = false)
            is SharedLedgerUiIntent.LoadSampleDemo -> loadSampleDemo()
            is SharedLedgerUiIntent.ToggleGuideDialog -> setState { copy(showGuideDialog = intent.show) }
            is SharedLedgerUiIntent.ToggleAddMemberDialog -> setState { copy(showAddMemberDialog = intent.show, errorMessage = null) }

            // Backward-compatible intent aliases
            is SharedLedgerUiIntent.UpdateFileIdInput -> setState { copy(addMemberInput = intent.input, errorMessage = null) }
            is SharedLedgerUiIntent.FetchFromInput -> addMemberFromUrlOrId(currentState.addMemberInput)
            is SharedLedgerUiIntent.LoadFile -> addMemberFromUrlOrId(intent.fileInfo.id)
            is SharedLedgerUiIntent.RefreshCurrentLedger -> fetchRemoteMemberLedgers(currentState.members, isSilent = false)
            is SharedLedgerUiIntent.ToggleShareGuide -> setState { copy(showGuideDialog = intent.show) }
            is SharedLedgerUiIntent.TogglePairPartnerDialog -> setState { copy(showAddMemberDialog = intent.show, errorMessage = null) }
            is SharedLedgerUiIntent.CopyOwnFileId -> shareMyFinancesViaDrive()
            is SharedLedgerUiIntent.CopyInviteLink -> shareMyFinancesViaDrive()
            is SharedLedgerUiIntent.InvitePartnerByEmail -> shareMyFinancesViaDrive()
            is SharedLedgerUiIntent.PairPartnerWithIdOrUrl -> addMemberFromUrlOrId(intent.input)
            is SharedLedgerUiIntent.UnlinkPartner -> handleUnlinkFirstPartner()
        }
    }

    // =========================================================================
    // Drive Sharing & Sync Handlers
    // =========================================================================

    private fun handleInitialLoad() {
        if (currentState.isDriveConnected) {
            fetchOwnBackupFileId()
            fetchRemoteMemberLedgers(currentState.members, isSilent = false)
        }
    }

    private fun handleCopyShareLink() {
        currentState.ownShareWebLink?.let { link ->
            sendEffect(SharedLedgerUiEffect.CopyToClipboard("Spent Drive Share Link", link))
            sendEffect(SharedLedgerUiEffect.ShowSnackbar("Drive link copied to clipboard!"))
        } ?: shareMyFinancesViaDrive()
    }

    private fun handleUnlinkFirstPartner() {
        val firstMember = currentState.members.firstOrNull { !it.isLocal }
        if (firstMember != null) {
            removeMember(firstMember.fileId)
        }
    }

    private fun fetchOwnBackupFileId() {
        viewModelScope.launch {
            val ownIdResult = repository.getOwnBackupFileId()
            val ownId = ownIdResult.getOrNull()
            setState {
                copy(
                    ownBackupFileId = ownId,
                    ownShareWebLink = if (ownId != null) "https://drive.google.com/file/d/$ownId/view?usp=sharing" else null
                )
            }
        }
    }

    private fun shareMyFinancesViaDrive() {
        viewModelScope.launch {
            setState { copy(isGeneratingShareLink = true, errorMessage = null) }

            // 1. Ensure own backup file exists on Drive
            var fileId = currentState.ownBackupFileId
            if (fileId.isNullOrBlank()) {
                val syncRes = repository.syncToGoogleDrive()
                if (syncRes.isSuccess) {
                    fileId = repository.getOwnBackupFileId().getOrNull()
                }
            }

            if (fileId.isNullOrBlank()) {
                setState { copy(isGeneratingShareLink = false) }
                sendEffect(SharedLedgerUiEffect.ShowSnackbar("Could not locate backup file on Google Drive. Connect in Settings first."))
                return@launch
            }

            // 2. Request Drive API permissions.create(role: "reader", type: "anyone")
            val shareResult = repository.enablePublicLinkSharing(fileId)
            setState { copy(isGeneratingShareLink = false) }

            val webUrl = if (shareResult.isSuccess) {
                shareResult.getOrNull() ?: "https://drive.google.com/file/d/$fileId/view?usp=sharing"
            } else {
                "https://drive.google.com/file/d/$fileId/view?usp=sharing"
            }

            setState { copy(ownBackupFileId = fileId, ownShareWebLink = webUrl) }
            sendEffect(SharedLedgerUiEffect.CopyToClipboard("Spent Drive Share Link", webUrl))
            sendEffect(SharedLedgerUiEffect.ShowSnackbar("Google Drive link copied! Anyone with the link can view your finances."))
        }
    }

    // =========================================================================
    // Member Operations
    // =========================================================================

    private fun addMemberFromUrlOrId(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            setState { copy(errorMessage = "Please enter a valid Google Drive link or File ID") }
            return
        }

        // Direct JSON string parsing check
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            parseAndAddRawJsonLedger(trimmed)
            return
        }

        // Extract File ID from HTTPS link or raw ID
        val fileId = GoogleDriveRestService.extractDriveFileId(trimmed)
        if (fileId.isBlank()) {
            setState { copy(errorMessage = "Could not parse Google Drive File ID from link") }
            return
        }

        fetchAndAddMember(fileId)
    }

    private fun parseAndAddRawJsonLedger(json: String) {
        val parseRes = SharedLedgerParser.parse(
            jsonString = json,
            fileId = "pasted_json_${System.currentTimeMillis()}",
            fileName = "Pasted JSON"
        )
        if (parseRes.isSuccess) {
            val ledger = parseRes.getOrNull()!!
            val memberInfo = SharedMemberInfo(
                fileId = ledger.sourceFileId ?: "pasted_json",
                name = ledger.ownerName,
                role = ledger.ownerRole,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.addOrUpdateSharedMember(memberInfo)
                memberLedgerCache[memberInfo.fileId] = ledger
                setState {
                    copy(
                        showAddMemberDialog = false,
                        addMemberInput = "",
                        errorMessage = null
                    )
                }
                recalculateUnifiedData()
                sendEffect(SharedLedgerUiEffect.ShowSnackbar("Added ${ledger.ownerName} to shared finances"))
            }
        } else {
            setState { copy(errorMessage = "Invalid JSON ledger format") }
        }
    }

    private fun fetchAndAddMember(fileId: String) {
        viewModelScope.launch {
            setState { copy(isLoading = true, errorMessage = null) }

            val downloadResult = GoogleDriveRestService.downloadFileById(fileId)
            if (downloadResult.isSuccess) {
                val json = downloadResult.getOrNull() ?: ""
                val parseResult = SharedLedgerParser.parse(json, fileId = fileId, fileName = "Shared Ledger")
                if (parseResult.isSuccess) {
                    val ledger = parseResult.getOrNull()!!
                    val memberInfo = SharedMemberInfo(
                        fileId = fileId,
                        name = ledger.ownerName,
                        role = ledger.ownerRole,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        isLocal = false
                    )
                    repository.addOrUpdateSharedMember(memberInfo)
                    memberLedgerCache[fileId] = ledger

                    setState {
                        copy(
                            isLoading = false,
                            showAddMemberDialog = false,
                            addMemberInput = "",
                            errorMessage = null
                        )
                    }
                    recalculateUnifiedData()
                    sendEffect(SharedLedgerUiEffect.ShowSnackbar("Connected with ${ledger.ownerName}!"))
                } else {
                    val err = parseResult.exceptionOrNull()?.localizedMessage ?: "Invalid Spent JSON file format"
                    setState { copy(isLoading = false, errorMessage = err) }
                    sendEffect(SharedLedgerUiEffect.ShowSnackbar("Error: $err"))
                }
            } else {
                val err = downloadResult.exceptionOrNull()?.localizedMessage ?: "File not accessible or permission denied"
                setState { copy(isLoading = false, errorMessage = err) }
                sendEffect(SharedLedgerUiEffect.ShowSnackbar("Drive download error: $err"))
            }
        }
    }

    private fun fetchRemoteMemberLedgers(members: List<SharedMemberInfo>, isSilent: Boolean) {
        val nonLocalMembers = members.filter { !it.isLocal }
        if (nonLocalMembers.isEmpty()) {
            recalculateUnifiedData()
            return
        }

        viewModelScope.launch {
            if (!isSilent) {
                setState { copy(isRefreshing = true) }
            }

            var successCount = 0
            for (m in nonLocalMembers) {
                if (m.fileId == "sample_demo_id") {
                    val sampleJson = SharedLedgerParser.generateSampleLedgerJson()
                    val parseRes = SharedLedgerParser.parse(sampleJson, fileId = "sample_demo_id", fileName = "Demo Shared Ledger")
                    parseRes.getOrNull()?.let {
                        memberLedgerCache[m.fileId] = it
                        successCount++
                    }
                    continue
                }

                val downloadRes = GoogleDriveRestService.downloadFileById(m.fileId)
                if (downloadRes.isSuccess) {
                    val json = downloadRes.getOrNull() ?: ""
                    val parseRes = SharedLedgerParser.parse(json, fileId = m.fileId, fileName = m.name)
                    if (parseRes.isSuccess) {
                        val ledger = parseRes.getOrNull()!!
                        memberLedgerCache[m.fileId] = ledger
                        repository.addOrUpdateSharedMember(
                            m.copy(name = ledger.ownerName, role = ledger.ownerRole, lastSyncTimestamp = System.currentTimeMillis())
                        )
                        successCount++
                    }
                }
            }

            setState { copy(isRefreshing = false) }
            recalculateUnifiedData()

            if (!isSilent) {
                sendEffect(SharedLedgerUiEffect.ShowSnackbar("Synced $successCount member ledger(s)"))
            }
        }
    }

    private fun refreshSingleMember(fileId: String) {
        viewModelScope.launch {
            val member = currentState.members.find { it.fileId == fileId }
            if (member != null) {
                fetchRemoteMemberLedgers(listOf(member), isSilent = false)
            }
        }
    }

    private fun removeMember(fileId: String) {
        viewModelScope.launch {
            repository.removeSharedMember(fileId)
            memberLedgerCache.remove(fileId)
            recalculateUnifiedData()
            sendEffect(SharedLedgerUiEffect.ShowSnackbar("Member removed"))
        }
    }

    private fun updateMemberName(fileId: String, newName: String) {
        viewModelScope.launch {
            val trimmed = newName.trim()
            if (trimmed.isNotBlank()) {
                if (fileId == "local_user") {
                    repository.updateUserProfileName(trimmed)
                } else {
                    repository.updateSharedMemberName(fileId, trimmed)
                }
                setState { copy(editingMember = null) }
                recalculateUnifiedData()
                sendEffect(SharedLedgerUiEffect.ShowSnackbar("Member name updated to '$trimmed'"))
            }
        }
    }

    private fun loadSampleDemo() {
        val sampleJson = SharedLedgerParser.generateSampleLedgerJson()
        val parseRes = SharedLedgerParser.parse(sampleJson, fileId = "sample_demo_id", fileName = "Demo Shared Ledger")
        if (parseRes.isSuccess) {
            val ledger = parseRes.getOrNull()!!
            val demoMember = SharedMemberInfo(
                fileId = "sample_demo_id",
                name = ledger.ownerName,
                role = ledger.ownerRole,
                lastSyncTimestamp = System.currentTimeMillis(),
                isLocal = false
            )
            viewModelScope.launch {
                repository.addOrUpdateSharedMember(demoMember)
                memberLedgerCache["sample_demo_id"] = ledger
                recalculateUnifiedData()
                sendEffect(SharedLedgerUiEffect.ShowSnackbar("Loaded demo member: ${ledger.ownerName}"))
            }
        }
    }
}
