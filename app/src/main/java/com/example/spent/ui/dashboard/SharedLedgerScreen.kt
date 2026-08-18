package com.app.spent.ui.dashboard

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
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
import com.app.spent.data.sync.HouseholdCategoryEnvelope
import com.app.spent.data.sync.HouseholdSource
import com.app.spent.data.sync.HouseholdSummary
import com.app.spent.data.sync.HouseholdTransactionItem
import com.app.spent.data.sync.HouseholdUnifiedData
import com.app.spent.data.sync.SharedCategoryItem
import com.app.spent.data.sync.SharedLedgerData
import com.app.spent.data.sync.SharedTransactionItem
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
              text = stringResource(R.string.shared_ledgers_screen_title),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = stringResource(R.string.shared_ledgers_subtitle),
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
          IconButton(onClick = { viewModel.onIntent(SharedLedgerUiIntent.ToggleShareGuide(true)) }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.HelpOutline,
              contentDescription = stringResource(R.string.shared_ledgers_share_guide_btn)
            )
          }
          if (state.isPartnerPaired || state.activeLedger != null) {
            IconButton(onClick = { viewModel.onIntent(SharedLedgerUiIntent.RefreshCurrentLedger) }) {
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
      contentPadding = PaddingValues(bottom = 36.dp)
    ) {
      // 1. Partner Status & Action Banner
      item {
        PartnerStatusBar(
          isDriveConnected = state.isDriveConnected,
          isPartnerPaired = state.isPartnerPaired,
          partnerName = state.partnerName ?: state.activeLedger?.ownerName,
          lastSyncTimestamp = state.partnerLastSyncTimestamp,
          onPairClick = { viewModel.onIntent(SharedLedgerUiIntent.TogglePairPartnerDialog(true)) },
          onUnlinkClick = { viewModel.onIntent(SharedLedgerUiIntent.UnlinkPartner) },
          onRefreshClick = { viewModel.onIntent(SharedLedgerUiIntent.RefreshCurrentLedger) },
          onLoadDemo = { viewModel.onIntent(SharedLedgerUiIntent.LoadSampleDemo) }
        )
      }

      // 2. Tab Selector: [ 🏠 Household | 👤 You | 👥 Partner ]
      item {
        HouseholdTabRow(
          selectedTab = state.selectedTab,
          onTabSelected = { viewModel.onIntent(SharedLedgerUiIntent.SwitchTab(it)) }
        )
      }

      // 3. Loading Indicator
      if (state.isLoading) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }
      }

      // 4. Tab Content Rendering
      when (state.selectedTab) {
        HouseholdTab.HOUSEHOLD -> {
          val household = state.householdData
          if (household != null) {
            item {
              HouseholdHeroCard(
                summary = household.summary,
                partnerName = household.partnerName,
                isPartnerActive = household.isPartnerActive
              )
            }

            item {
              HouseholdKpiRow(summary = household.summary)
            }

            // Category Envelopes Breakdown
            if (household.categories.isNotEmpty()) {
              item {
                Text(
                  text = stringResource(R.string.shared_ledgers_category_breakdown),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
              }
              items(household.categories, key = { "hh_cat_${it.name}" }) { envelope ->
                HouseholdCategoryRow(envelope = envelope, currencySymbol = household.summary.currencySymbol)
              }
            }

            // Unified Activity Feed
            if (household.transactions.isNotEmpty()) {
              item {
                Text(
                  text = stringResource(R.string.shared_ledgers_recent_txs),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
              }
              items(household.transactions, key = { "hh_tx_${it.id}" }) { tx ->
                HouseholdTransactionRow(tx = tx, currencySymbol = household.summary.currencySymbol)
              }
            }
          } else if (!state.isLoading) {
            item {
              EmptyHouseholdState(
                onPairPartner = { viewModel.onIntent(SharedLedgerUiIntent.TogglePairPartnerDialog(true)) },
                onLoadDemo = { viewModel.onIntent(SharedLedgerUiIntent.LoadSampleDemo) }
              )
            }
          }
        }

        HouseholdTab.ME -> {
          val household = state.householdData
          if (household != null) {
            item {
              PersonalSummarySection(summary = household.summary)
            }

            val myTxs = household.transactions.filter { it.source == HouseholdSource.YOU }
            if (myTxs.isNotEmpty()) {
              item {
                Text(
                  text = stringResource(R.string.recent_activity),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
              }
              items(myTxs, key = { "me_tx_${it.id}" }) { tx ->
                HouseholdTransactionRow(tx = tx, currencySymbol = household.summary.currencySymbol)
              }
            }
          }
        }

        HouseholdTab.PARTNER -> {
          val active = state.activeLedger
          if (active != null) {
            item {
              PartnerDetailedHeader(ledger = active)
            }

            if (active.categories.isNotEmpty()) {
              item {
                Text(
                  text = stringResource(R.string.shared_ledgers_category_breakdown),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
              }
              items(active.categories, key = { "p_cat_${it.id}" }) { category ->
                PartnerCategoryRow(category = category, currencySymbol = active.currencySymbol)
              }
            }

            if (active.recentTransactions.isNotEmpty()) {
              item {
                Text(
                  text = stringResource(R.string.shared_ledgers_recent_txs),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
              }
              items(active.recentTransactions, key = { "p_tx_${it.id}" }) { tx ->
                PartnerTransactionRow(tx = tx, currencySymbol = active.currencySymbol)
              }
            }
          } else if (!state.isLoading) {
            item {
              EmptyPartnerState(
                onPairPartner = { viewModel.onIntent(SharedLedgerUiIntent.TogglePairPartnerDialog(true)) },
                onLoadDemo = { viewModel.onIntent(SharedLedgerUiIntent.LoadSampleDemo) }
              )
            }
          }
        }
      }
    }
  }

  // Automated In-App Pair Dialog
  if (state.showPairPartnerDialog) {
    PairPartnerDialog(
      emailInput = state.partnerEmailInput,
      linkInput = state.manualFileIdInput,
      isLoading = state.isSharingWithEmail || state.isLoading,
      errorMessage = state.errorMessage,
      onEmailChange = { viewModel.onIntent(SharedLedgerUiIntent.UpdatePartnerEmailInput(it)) },
      onLinkChange = { viewModel.onIntent(SharedLedgerUiIntent.UpdateFileIdInput(it)) },
      onInviteByEmail = { viewModel.onIntent(SharedLedgerUiIntent.InvitePartnerByEmail(it)) },
      onPairByLink = { viewModel.onIntent(SharedLedgerUiIntent.PairPartnerWithIdOrUrl(it)) },
      onCopyMyLink = { viewModel.onIntent(SharedLedgerUiIntent.CopyInviteLink) },
      onDismiss = { viewModel.onIntent(SharedLedgerUiIntent.TogglePairPartnerDialog(false)) }
    )
  }

  // How it works Guide Dialog
  if (state.showShareGuideDialog) {
    HouseholdGuideDialog(
      onDismiss = { viewModel.onIntent(SharedLedgerUiIntent.ToggleShareGuide(false)) },
      onCopyMyLink = { viewModel.onIntent(SharedLedgerUiIntent.CopyInviteLink) }
    )
  }
}

@Composable
private fun PartnerStatusBar(
  isDriveConnected: Boolean,
  isPartnerPaired: Boolean,
  partnerName: String?,
  lastSyncTimestamp: Long,
  onPairClick: () -> Unit,
  onUnlinkClick: () -> Unit,
  onRefreshClick: () -> Unit,
  onLoadDemo: () -> Unit
) {
  val formattedSync = remember(lastSyncTimestamp) {
    if (lastSyncTimestamp > 0) {
      SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault()).format(Date(lastSyncTimestamp))
    } else "Just now"
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isPartnerPaired || partnerName != null) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
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
            .size(42.dp)
            .clip(CircleShape)
            .background(
              if (isPartnerPaired || partnerName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isPartnerPaired || partnerName != null) Icons.Default.Group else Icons.Default.Person,
            contentDescription = null,
            tint = if (isPartnerPaired || partnerName != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = if (isPartnerPaired || partnerName != null) {
              stringResource(R.string.household_paired_with, partnerName ?: "Partner")
            } else {
              stringResource(R.string.household_not_paired)
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = if (isPartnerPaired || partnerName != null) {
              stringResource(R.string.shared_ledgers_last_synced, formattedSync)
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

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isPartnerPaired || partnerName != null) {
          IconButton(onClick = onRefreshClick, modifier = Modifier.size(34.dp)) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Sync",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }
          OutlinedButton(
            onClick = onUnlinkClick,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
          ) {
            Text(text = stringResource(R.string.household_unlink_btn), style = MaterialTheme.typography.labelSmall)
          }
        } else {
          Button(
            onClick = onPairClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
          ) {
            Text(text = stringResource(R.string.household_pair_btn), style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
  }
}

@Composable
private fun HouseholdTabRow(
  selectedTab: HouseholdTab,
  onTabSelected: (HouseholdTab) -> Unit
) {
  val tabs = listOf(HouseholdTab.HOUSEHOLD, HouseholdTab.ME, HouseholdTab.PARTNER)
  val titles = listOf(
    stringResource(R.string.household_tab_combined),
    stringResource(R.string.household_tab_me),
    stringResource(R.string.household_tab_partner)
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
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      )
    }
  }
}

@Composable
private fun HouseholdHeroCard(
  summary: HouseholdSummary,
  partnerName: String,
  isPartnerActive: Boolean
) {
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

      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(
            text = "Your Safe Spend",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
          )
          Text(
            text = "${summary.currencySymbol}${"%.2f".format(summary.mySafeToSpendToday)}/day",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "$partnerName's Safe Spend",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
          )
          Text(
            text = "${summary.currencySymbol}${"%.2f".format(summary.partnerSafeToSpendToday)}/day",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }
    }
  }
}

@Composable
private fun HouseholdKpiRow(summary: HouseholdSummary) {
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
private fun HouseholdCategoryRow(
  envelope: HouseholdCategoryEnvelope,
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
            Text(
              text = "${stringResource(R.string.household_you_spent, "$currencySymbol${"%.2f".format(envelope.mySpent)}")} · ${stringResource(R.string.household_partner_spent, "$currencySymbol${"%.2f".format(envelope.partnerSpent)}")}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun HouseholdTransactionRow(
  tx: HouseholdTransactionItem,
  currencySymbol: String
) {
  val dateFormatted = remember(tx.timestamp) {
    SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
  }
  val isIncome = tx.type.equals("INCOME", ignoreCase = true)
  val isMe = tx.source == HouseholdSource.YOU

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
                if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
              )
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = if (isMe) stringResource(R.string.household_badge_you) else tx.authorName,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
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
private fun PersonalSummarySection(summary: HouseholdSummary) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 8.dp),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "Personal Budget Summary",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(text = "My Total Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(text = "${summary.currencySymbol}${"%.2f".format(summary.myTotalIncome)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IncomeGreen)
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(text = "My Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(text = "${summary.currencySymbol}${"%.2f".format(summary.myTotalSpent)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
        }
      }
    }
  }
}

@Composable
private fun PartnerDetailedHeader(ledger: SharedLedgerData) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 8.dp),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = ledger.ownerName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = ledger.ownerRole,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "${ledger.daysRemainingInCycle} days left",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(text = "Safe Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(text = "${ledger.currencySymbol}${"%.2f".format(ledger.safeToSpendToday)}/day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(text = "Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(text = "${ledger.currencySymbol}${"%.2f".format(ledger.totalSpent)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
        }
      }
    }
  }
}

@Composable
private fun PartnerCategoryRow(category: SharedCategoryItem, currencySymbol: String) {
  val catColor = remember(category.colorHex) {
    try {
      Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
      Color(0xFF64748B)
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 4.dp),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(catColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = CategoryIconHelper.getIconByName(category.iconName),
            contentDescription = category.name,
            tint = catColor,
            modifier = Modifier.size(16.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = category.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
      }
      Text(
        text = if (category.budgetAmount > 0) {
          "$currencySymbol${"%.2f".format(category.spentAmount)} / $currencySymbol${"%.2f".format(category.budgetAmount)}"
        } else {
          "$currencySymbol${"%.2f".format(category.spentAmount)}"
        },
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun PartnerTransactionRow(tx: SharedTransactionItem, currencySymbol: String) {
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
    Column {
      Text(
        text = tx.note.ifBlank { tx.categoryName },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
      )
      Text(
        text = "${tx.categoryName} · $dateFormatted",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Text(
      text = "${if (isIncome) "+" else "-"}$currencySymbol${"%.2f".format(tx.amount)}",
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      color = if (isIncome) IncomeGreen else ExpenseRed
    )
  }
}

@Composable
private fun EmptyHouseholdState(
  onPairPartner: () -> Unit,
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
        Button(onClick = onPairPartner, shape = RoundedCornerShape(12.dp)) {
          Text(text = stringResource(R.string.household_pair_btn))
        }
        OutlinedButton(onClick = onLoadDemo, shape = RoundedCornerShape(12.dp)) {
          Text(text = stringResource(R.string.shared_ledgers_load_sample_btn))
        }
      }
    }
  }
}

@Composable
private fun EmptyPartnerState(
  onPairPartner: () -> Unit,
  onLoadDemo: () -> Unit
) {
  EmptyHouseholdState(onPairPartner = onPairPartner, onLoadDemo = onLoadDemo)
}

@Composable
private fun PairPartnerDialog(
  emailInput: String,
  linkInput: String,
  isLoading: Boolean,
  errorMessage: String?,
  onEmailChange: (String) -> Unit,
  onLinkChange: (String) -> Unit,
  onInviteByEmail: (String) -> Unit,
  onPairByLink: (String) -> Unit,
  onCopyMyLink: () -> Unit,
  onDismiss: () -> Unit
) {
  var selectedTab by remember { mutableStateOf(0) } // 0 = Grant Access (Email), 1 = Paste Link/ID

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.household_dialog_pair_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Tab switcher
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
              .clickable { selectedTab = 0 }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "1. Grant Access",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
              .clickable { selectedTab = 1 }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "2. Link Partner",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (selectedTab == 0) {
          Text(
            text = "Enter your partner's Google email. Spent will automatically grant them Drive permissions so they can view your ledger safely.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedTextField(
            value = emailInput,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.household_enter_partner_email)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )

          Button(
            onClick = { onInviteByEmail(emailInput) },
            enabled = emailInput.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            if (isLoading) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
              Text(stringResource(R.string.household_grant_access_btn))
            }
          }

          OutlinedButton(
            onClick = onCopyMyLink,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.household_invite_by_link))
          }
        } else {
          Text(
            text = "Paste the invite link or Google Drive File ID sent to you by your partner.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedTextField(
            value = linkInput,
            onValueChange = onLinkChange,
            label = { Text(stringResource(R.string.household_enter_partner_link)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Link, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )

          Button(
            onClick = { onPairByLink(linkInput) },
            enabled = linkInput.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            if (isLoading) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
              Text(stringResource(R.string.household_connect_partner_btn))
            }
          }
        }

        if (errorMessage != null) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.labelSmall,
            color = ExpenseRed
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.btn_cancel))
      }
    }
  )
}

@Composable
private fun HouseholdGuideDialog(
  onDismiss: () -> Unit,
  onCopyMyLink: () -> Unit
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

        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
          onClick = onCopyMyLink,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = stringResource(R.string.household_invite_by_link), style = MaterialTheme.typography.labelSmall)
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
