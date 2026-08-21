package com.app.spent.ui.sharedledger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.ui.sharedledger.components.DriveStatusBar
import com.app.spent.ui.sharedledger.components.EmptyMembersState
import com.app.spent.ui.sharedledger.components.EmptyStatisticsState
import com.app.spent.ui.sharedledger.components.InviteAndJoinSection
import com.app.spent.ui.sharedledger.components.MemberCardRow
import com.app.spent.ui.sharedledger.components.MembersPanelHeader
import com.app.spent.ui.sharedledger.components.SharedCategoryEnvelopeRow
import com.app.spent.ui.sharedledger.components.SharedFinancesTabRow
import com.app.spent.ui.sharedledger.components.SharedTransactionRow
import com.app.spent.ui.sharedledger.components.StatisticsHeroCard
import com.app.spent.ui.sharedledger.components.StatisticsKpiRow
import com.app.spent.ui.sharedledger.components.dialogs.AddMemberDialog
import com.app.spent.ui.sharedledger.components.dialogs.EditMemberNameDialog
import com.app.spent.ui.sharedledger.components.dialogs.SharedFinancesGuideDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLedgerScreen(
    viewModel: SharedLedgerViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SharedLedgerUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SharedLedgerUiEffect.CopyToClipboard -> {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(effect.text))
                }
                is SharedLedgerUiEffect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.shared_finances_screen_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.shared_finances_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_close)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onIntent(SharedLedgerUiIntent.ToggleGuideDialog(true)) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.shared_ledgers_share_guide_btn)
                        )
                    }
                    if (state.isDriveConnected) {
                        IconButton(onClick = { viewModel.onIntent(SharedLedgerUiIntent.RefreshAll) }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // 1. Google Drive Status Header
            item {
                DriveStatusBar(
                    isDriveConnected = state.isDriveConnected,
                    accountEmail = state.driveAccountEmail,
                    membersCount = state.members.filter { !it.isLocal }.size,
                    onRefreshAll = { viewModel.onIntent(SharedLedgerUiIntent.RefreshAll) }
                )
            }

            // 2. Navigation Tab Row: [ 📊 Statistics | 👥 Members | 🔗 Invite / Join ]
            item {
                SharedFinancesTabRow(
                    selectedTab = state.selectedTab,
                    onTabSelected = { viewModel.onIntent(SharedLedgerUiIntent.SwitchTab(it)) }
                )
            }

            // 3. Loading Indicator
            if (state.isLoading || state.isRefreshing) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 4. Tab Contents
            when (state.selectedTab) {
                SharedFinancesTab.STATISTICS -> {
                    val unified = state.unifiedData
                    if (unified != null && (unified.summary.combinedIncome > 0 || (unified.summary.combinedSpent) > 0 || state.members.isNotEmpty())) {
                        val summary = unified.summary
                        item {
                            StatisticsHeroCard(summary = summary)
                        }

                        item {
                            StatisticsKpiRow(summary = summary)
                        }

                        // Category Envelopes Breakdown
                        if (unified.categories.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.shared_ledgers_category_breakdown),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                )
                            }
                            items(unified.categories, key = { "shared_cat_${it.name}" }) { envelope ->
                                SharedCategoryEnvelopeRow(envelope = envelope, currencySymbol = summary.currencySymbol)
                            }
                        }

                        // Activity Feed
                        if (unified.transactions.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.shared_ledgers_recent_txs),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                )
                            }
                            items(unified.transactions, key = { "shared_tx_${it.id}" }) { tx ->
                                SharedTransactionRow(tx = tx, currencySymbol = summary.currencySymbol)
                            }
                        }
                    } else if (!state.isLoading) {
                        item {
                            EmptyStatisticsState(
                                onGoToInvite = { viewModel.onIntent(SharedLedgerUiIntent.SwitchTab(SharedFinancesTab.INVITE_JOIN)) },
                                onLoadDemo = { viewModel.onIntent(SharedLedgerUiIntent.LoadSampleDemo) }
                            )
                        }
                    }
                }

                SharedFinancesTab.MEMBERS -> {
                    item {
                        MembersPanelHeader(
                            membersCount = state.members.size,
                            onAddMemberClick = { viewModel.onIntent(SharedLedgerUiIntent.ToggleAddMemberDialog(true)) },
                            onRefreshAll = { viewModel.onIntent(SharedLedgerUiIntent.RefreshAll) }
                        )
                    }

                    val nonLocalMembers = state.members.filter { !it.isLocal }
                    if (nonLocalMembers.isEmpty()) {
                        item {
                            EmptyMembersState(
                                onInviteClick = { viewModel.onIntent(SharedLedgerUiIntent.SwitchTab(SharedFinancesTab.INVITE_JOIN)) },
                                onLoadDemo = { viewModel.onIntent(SharedLedgerUiIntent.LoadSampleDemo) }
                            )
                        }
                    } else {
                        items(nonLocalMembers, key = { "member_${it.fileId}" }) { member ->
                            MemberCardRow(
                                member = member,
                                onEdit = { viewModel.onIntent(SharedLedgerUiIntent.ToggleEditMemberDialog(member)) },
                                onRefresh = { viewModel.onIntent(SharedLedgerUiIntent.RefreshMember(member.fileId)) },
                                onRemove = { viewModel.onIntent(SharedLedgerUiIntent.RemoveMember(member.fileId)) }
                            )
                        }
                    }
                }

                SharedFinancesTab.INVITE_JOIN -> {
                    item {
                        InviteAndJoinSection(
                            isDriveConnected = state.isDriveConnected,
                            isGeneratingLink = state.isGeneratingShareLink,
                            shareWebLink = state.ownShareWebLink,
                            inputUrl = state.addMemberInput,
                            isLoading = state.isLoading,
                            errorMessage = state.errorMessage,
                            onShareClick = { viewModel.onIntent(SharedLedgerUiIntent.ShareMyFinances) },
                            onCopyLink = { viewModel.onIntent(SharedLedgerUiIntent.CopyShareLink) },
                            onInputChange = { viewModel.onIntent(SharedLedgerUiIntent.UpdateAddMemberInput(it)) },
                            onAddMember = { viewModel.onIntent(SharedLedgerUiIntent.AddMemberByUrlOrId(it)) },
                            onLoadDemo = { viewModel.onIntent(SharedLedgerUiIntent.LoadSampleDemo) }
                        )
                    }
                }
            }
        }
    }

    // Edit Member Name Modal Dialog
    state.editingMember?.let { memberToEdit ->
        EditMemberNameDialog(
            member = memberToEdit,
            onSaveName = { newName ->
                viewModel.onIntent(SharedLedgerUiIntent.UpdateMemberName(memberToEdit.fileId, newName))
            },
            onDismiss = { viewModel.onIntent(SharedLedgerUiIntent.ToggleEditMemberDialog(null)) }
        )
    }

    // Add Member Modal Dialog
    if (state.showAddMemberDialog) {
        AddMemberDialog(
            input = state.addMemberInput,
            isLoading = state.isLoading,
            errorMessage = state.errorMessage,
            onInputChange = { viewModel.onIntent(SharedLedgerUiIntent.UpdateAddMemberInput(it)) },
            onAdd = { viewModel.onIntent(SharedLedgerUiIntent.AddMemberByUrlOrId(it)) },
            onDismiss = { viewModel.onIntent(SharedLedgerUiIntent.ToggleAddMemberDialog(false)) }
        )
    }

    // Guide Dialog
    if (state.showGuideDialog) {
        SharedFinancesGuideDialog(
            onDismiss = { viewModel.onIntent(SharedLedgerUiIntent.ToggleGuideDialog(false)) },
            onShareLink = { viewModel.onIntent(SharedLedgerUiIntent.ShareMyFinances) }
        )
    }
}
