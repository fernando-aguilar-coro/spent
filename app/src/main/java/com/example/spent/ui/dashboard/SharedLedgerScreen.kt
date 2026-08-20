package com.app.spent.ui.dashboard

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.data.sync.SharedCategoryEnvelope
import com.app.spent.data.sync.SharedFinancesSummary
import com.app.spent.data.sync.SharedMemberInfo
import com.app.spent.data.sync.SharedUnifiedTransactionItem
import com.app.spent.ui.components.CategoryIconHelper
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLedgerScreen(
  viewModel: SharedLedgerViewModel,
  onNavigateBack: () -> Unit
) {
  val state by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.effect.collect { effect ->
      when (effect) {
        is SharedLedgerUiEffect.ShowSnackbar -> {
          snackbarHostState.showSnackbar(effect.message)
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
          if (unified != null && unified.summary.combinedIncome > 0 || (unified?.summary?.combinedSpent ?: 0.0) > 0 || state.members.isNotEmpty()) {
            val summary = unified?.summary
            if (summary != null) {
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

@Composable
private fun DriveStatusBar(
  isDriveConnected: Boolean,
  accountEmail: String?,
  membersCount: Int,
  onRefreshAll: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isDriveConnected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant
      }
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
              if (isDriveConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isDriveConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
            contentDescription = null,
            tint = if (isDriveConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = if (isDriveConnected) {
              stringResource(R.string.shared_ledgers_drive_status_connected, accountEmail ?: "Google Drive")
            } else {
              stringResource(R.string.shared_ledgers_drive_status_disconnected)
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = if (membersCount > 0) {
              stringResource(R.string.members_connected_count, membersCount)
            } else {
              stringResource(R.string.household_privacy_note)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (isDriveConnected && membersCount > 0) {
        IconButton(onClick = onRefreshAll, modifier = Modifier.size(34.dp)) {
          Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Sync All",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun SharedFinancesTabRow(
  selectedTab: SharedFinancesTab,
  onTabSelected: (SharedFinancesTab) -> Unit
) {
  val tabs = listOf(SharedFinancesTab.STATISTICS, SharedFinancesTab.MEMBERS, SharedFinancesTab.INVITE_JOIN)
  val titles = listOf(
    stringResource(R.string.tab_statistics),
    stringResource(R.string.tab_members),
    stringResource(R.string.tab_invite_join)
  )

  TabRow(
    selectedTabIndex = tabs.indexOf(selectedTab),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 8.dp)
      .clip(RoundedCornerShape(14.dp)),
    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    contentColor = MaterialTheme.colorScheme.primary,
    indicator = { tabPositions ->
      TabRowDefaults.SecondaryIndicator(
        modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]),
        height = 3.dp,
        color = MaterialTheme.colorScheme.primary
      )
    },
    divider = {}
  ) {
    tabs.forEachIndexed { index, tab ->
      val isSelected = selectedTab == tab
      Tab(
        selected = isSelected,
        onClick = { onTabSelected(tab) },
        text = {
          Text(
            text = titles[index],
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      )
    }
  }
}

@Composable
private fun StatisticsHeroCard(summary: SharedFinancesSummary) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(R.string.household_kpi_combined_safe_today),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "${summary.daysRemainingInCycle} days left",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "${summary.currencySymbol}${"%.2f".format(summary.combinedSafeToSpendToday)}",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
      )

      if (summary.memberContributions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          summary.memberContributions.take(3).forEach { contrib ->
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${contrib.memberName} Safe",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${summary.currencySymbol}${"%.2f".format(contrib.safeToSpend)}/day",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatisticsKpiRow(summary: SharedFinancesSummary) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Combined Income
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            tint = IncomeGreen,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.household_kpi_combined_income),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${summary.currencySymbol}${"%.2f".format(summary.combinedIncome)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = IncomeGreen
        )
      }
    }

    // Combined Spent
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
            contentDescription = null,
            tint = ExpenseRed,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.household_kpi_combined_spent),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${summary.currencySymbol}${"%.2f".format(summary.combinedSpent)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = ExpenseRed
        )
      }
    }
  }
}

@Composable
private fun SharedCategoryEnvelopeRow(
  envelope: SharedCategoryEnvelope,
  currencySymbol: String
) {
  val catColor = remember(envelope.colorHex) {
    try {
      Color(android.graphics.Color.parseColor(envelope.colorHex))
    } catch (e: Exception) {
      Color(0xFF64748B)
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 5.dp),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(catColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = CategoryIconHelper.getIconByName(envelope.iconName),
              contentDescription = envelope.name,
              tint = catColor,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = envelope.name,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            val breakdownText = if (envelope.memberBreakdown.isNotEmpty()) {
              envelope.memberBreakdown.entries.joinToString(" · ") { "${it.key}: $currencySymbol${"%.0f".format(it.value)}" }
            } else {
              "You: $currencySymbol${"%.2f".format(envelope.mySpent)}"
            }
            Text(
              text = breakdownText,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Text(
          text = if (envelope.totalBudget > 0) {
            "${currencySymbol}${"%.2f".format(envelope.totalSpent)} / ${currencySymbol}${"%.2f".format(envelope.totalBudget)}"
          } else {
            "${currencySymbol}${"%.2f".format(envelope.totalSpent)}"
          },
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          color = if (envelope.totalBudget > 0 && envelope.totalSpent > envelope.totalBudget) ExpenseRed else MaterialTheme.colorScheme.onSurface
        )
      }

      if (envelope.totalBudget > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
          progress = { envelope.progress.coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = if (envelope.totalSpent > envelope.totalBudget) ExpenseRed else catColor,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
      }
    }
  }
}

@Composable
private fun SharedTransactionRow(
  tx: SharedUnifiedTransactionItem,
  currencySymbol: String
) {
  val dateFormatted = remember(tx.timestamp) {
    SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
  }
  val isIncome = tx.type.equals("INCOME", ignoreCase = true)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(
            if (isIncome) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
          contentDescription = null,
          tint = if (isIncome) IncomeGreen else ExpenseRed,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = tx.note,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(
                if (tx.isLocal) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
              )
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = if (tx.isLocal) stringResource(R.string.household_badge_you) else tx.authorName,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (tx.isLocal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
          }
        }
        Text(
          text = "${tx.categoryName} · $dateFormatted",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Text(
      text = "${if (isIncome) "+" else "-"}${currencySymbol}${"%.2f".format(tx.amount)}",
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      color = if (isIncome) IncomeGreen else ExpenseRed
    )
  }
}

@Composable
private fun MembersPanelHeader(
  membersCount: Int,
  onAddMemberClick: () -> Unit,
  onRefreshAll: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = stringResource(R.string.members_panel_title),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      IconButton(onClick = onRefreshAll, modifier = Modifier.size(34.dp)) {
        Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync All", tint = MaterialTheme.colorScheme.primary)
      }
      FilledTonalButton(
        onClick = onAddMemberClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(34.dp)
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = stringResource(R.string.add_member_btn), style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}

@Composable
private fun MemberCardRow(
  member: SharedMemberInfo,
  onRefresh: () -> Unit,
  onRemove: () -> Unit
) {
  val formattedSync = remember(member.lastSyncTimestamp) {
    if (member.lastSyncTimestamp > 0) {
      SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault()).format(Date(member.lastSyncTimestamp))
    } else "Never"
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 5.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = member.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Updated: $formattedSync",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Sync",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Remove",
            tint = ExpenseRed,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun InviteAndJoinSection(
  isDriveConnected: Boolean,
  isGeneratingLink: Boolean,
  shareWebLink: String?,
  inputUrl: String,
  isLoading: Boolean,
  errorMessage: String?,
  onShareClick: () -> Unit,
  onCopyLink: () -> Unit,
  onInputChange: (String) -> Unit,
  onAddMember: (String) -> Unit,
  onLoadDemo: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Share My Finances Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.share_my_finances_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = stringResource(R.string.share_my_finances_desc),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = onShareClick,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        ) {
          if (isGeneratingLink) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
          } else {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.share_my_finances_btn))
          }
        }
      }
    }

    // 2. Add Member Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.add_member_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = stringResource(R.string.add_member_desc),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = inputUrl,
          onValueChange = onInputChange,
          placeholder = { Text(stringResource(R.string.add_member_input_placeholder)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = errorMessage, style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = { onAddMember(inputUrl) },
          enabled = inputUrl.isNotBlank() && !isLoading,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        ) {
          if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
          } else {
            Text(stringResource(R.string.add_member_btn))
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = onLoadDemo,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(stringResource(R.string.shared_ledgers_load_sample_btn))
        }
      }
    }
  }
}

@Composable
private fun EmptyStatisticsState(
  onGoToInvite: () -> Unit,
  onLoadDemo: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 30.dp, vertical = 40.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Default.Group,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        modifier = Modifier.size(56.dp)
      )
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = stringResource(R.string.shared_ledgers_no_file_loaded),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onGoToInvite, shape = RoundedCornerShape(12.dp)) {
          Text(text = stringResource(R.string.add_member_btn))
        }
        OutlinedButton(onClick = onLoadDemo, shape = RoundedCornerShape(12.dp)) {
          Text(text = stringResource(R.string.shared_ledgers_load_sample_btn))
        }
      }
    }
  }
}

@Composable
private fun EmptyMembersState(
  onInviteClick: () -> Unit,
  onLoadDemo: () -> Unit
) {
  EmptyStatisticsState(onGoToInvite = onInviteClick, onLoadDemo = onLoadDemo)
}

@Composable
private fun AddMemberDialog(
  input: String,
  isLoading: Boolean,
  errorMessage: String?,
  onInputChange: (String) -> Unit,
  onAdd: (String) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.add_member_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.add_member_desc),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
          value = input,
          onValueChange = onInputChange,
          placeholder = { Text(stringResource(R.string.add_member_input_placeholder)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
        if (errorMessage != null) {
          Text(text = errorMessage, style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onAdd(input) },
        enabled = input.isNotBlank() && !isLoading,
        shape = RoundedCornerShape(10.dp)
      ) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
          Text(stringResource(R.string.add_member_btn))
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.btn_cancel))
      }
    }
  )
}

@Composable
private fun SharedFinancesGuideDialog(
  onDismiss: () -> Unit,
  onShareLink: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.shared_ledgers_guide_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(text = stringResource(R.string.shared_ledgers_guide_step1), style = MaterialTheme.typography.bodySmall)
        Text(text = stringResource(R.string.shared_ledgers_guide_step2), style = MaterialTheme.typography.bodySmall)
        Text(text = stringResource(R.string.shared_ledgers_guide_step3), style = MaterialTheme.typography.bodySmall)
        Text(text = stringResource(R.string.shared_ledgers_guide_step4), style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(6.dp))
        OutlinedButton(
          onClick = onShareLink,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = stringResource(R.string.share_my_finances_btn), style = MaterialTheme.typography.labelSmall)
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text(stringResource(R.string.shared_ledgers_guide_understood))
      }
    }
  )
}
