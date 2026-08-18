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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.spent.R
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
          Text(
            text = stringResource(R.string.shared_ledgers_screen_title),
            fontWeight = FontWeight.Bold
          )
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
          if (state.activeLedger != null) {
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
      contentPadding = PaddingValues(bottom = 32.dp)
    ) {
      // 1. Connection Banner & Quick Guide
      item {
        DriveStatusBanner(
          isDriveConnected = state.isDriveConnected,
          driveAccountEmail = state.driveAccountEmail,
          onOpenGuide = { viewModel.onIntent(SharedLedgerUiIntent.ToggleShareGuide(true)) },
          onCopyMyId = { viewModel.onIntent(SharedLedgerUiIntent.CopyOwnFileId) }
        )
      }

      // 2. Drive File ID / Link Input Box
      item {
        DriveInputSection(
          input = state.manualFileIdInput,
          isLoading = state.isLoading,
          onInputChange = { viewModel.onIntent(SharedLedgerUiIntent.UpdateFileIdInput(it)) },
          onFetch = { viewModel.onIntent(SharedLedgerUiIntent.FetchFromInput) },
          onLoadDemo = { viewModel.onIntent(SharedLedgerUiIntent.LoadSampleDemo) }
        )
      }

      // 3. Available Drive Files chips (if found on user's drive)
      if (state.availableSharedFiles.isNotEmpty()) {
        item {
          Text(
            text = stringResource(R.string.shared_ledgers_detected_files),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
          )
          LazyRow(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(state.availableSharedFiles, key = { it.id }) { file ->
              FilledTonalButton(
                onClick = { viewModel.onIntent(SharedLedgerUiIntent.LoadFile(file)) },
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CloudDone,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = file.name,
                  style = MaterialTheme.typography.labelMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      // Loading state indicator
      if (state.isLoading) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(40.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }
      }

      // 4. Render Active Shared Ledger Data
      state.activeLedger?.let { ledger ->
        item {
          Spacer(modifier = Modifier.height(8.dp))
          SharedLedgerHeaderCard(ledger = ledger)
        }

        item {
          SharedLedgerKpiGrid(ledger = ledger)
        }

        // Category Envelopes Breakdown
        if (ledger.categories.isNotEmpty()) {
          item {
            Text(
              text = stringResource(R.string.shared_ledgers_category_breakdown),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
          }
          items(ledger.categories, key = { it.id }) { category ->
            SharedCategoryRow(category = category, currencySymbol = ledger.currencySymbol)
          }
        }

        // Recent Transactions Activity
        if (ledger.recentTransactions.isNotEmpty()) {
          item {
            Text(
              text = stringResource(R.string.shared_ledgers_recent_txs),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
          }
          items(ledger.recentTransactions, key = { it.id }) { tx ->
            SharedTransactionRow(tx = tx, currencySymbol = ledger.currencySymbol)
          }
        }
      } ?: run {
        if (!state.isLoading) {
          item {
            EmptySharedLedgerState(
              onLoadDemo = { viewModel.onIntent(SharedLedgerUiIntent.LoadSampleDemo) }
            )
          }
        }
      }
    }
  }

  // "How to Share" Dialog
  if (state.showShareGuideDialog) {
    ShareGuideDialog(
      ownFileId = state.ownBackupFileId,
      onDismiss = { viewModel.onIntent(SharedLedgerUiIntent.ToggleShareGuide(false)) },
      onCopyId = { viewModel.onIntent(SharedLedgerUiIntent.CopyOwnFileId) }
    )
  }
}

@Composable
private fun DriveStatusBanner(
  isDriveConnected: Boolean,
  driveAccountEmail: String?,
  onOpenGuide: () -> Unit,
  onCopyMyId: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isDriveConnected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant
      }
    )
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            imageVector = if (isDriveConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
            contentDescription = null,
            tint = if (isDriveConnected) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isDriveConnected && !driveAccountEmail.isNullOrBlank()) {
              stringResource(R.string.shared_ledgers_drive_status_connected, driveAccountEmail)
            } else {
              stringResource(R.string.shared_ledgers_drive_status_disconnected)
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onOpenGuide,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(text = stringResource(R.string.shared_ledgers_share_guide_btn), style = MaterialTheme.typography.labelMedium)
        }
        if (isDriveConnected) {
          Button(
            onClick = onCopyMyId,
            modifier = Modifier.weight(1.2f),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.shared_ledgers_copy_my_id), style = MaterialTheme.typography.labelMedium, maxLines = 1)
          }
        }
      }
    }
  }
}

@Composable
private fun DriveInputSection(
  input: String,
  isLoading: Boolean,
  onInputChange: (String) -> Unit,
  onFetch: () -> Unit,
  onLoadDemo: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp)
  ) {
    OutlinedTextField(
      value = input,
      onValueChange = onInputChange,
      modifier = Modifier.fillMaxWidth(),
      label = { Text(stringResource(R.string.shared_ledgers_enter_id_or_url)) },
      placeholder = { Text("e.g. 1B2c3D4e5F6G7H8... or paste link / JSON") },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      trailingIcon = {
        if (input.isNotBlank()) {
          IconButton(onClick = onFetch, enabled = !isLoading) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = stringResource(R.string.shared_ledgers_fetch_btn),
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = onFetch,
        enabled = input.isNotBlank() && !isLoading,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(text = stringResource(R.string.shared_ledgers_fetch_btn))
      }

      OutlinedButton(
        onClick = onLoadDemo,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(text = stringResource(R.string.shared_ledgers_load_sample_btn))
      }
    }
  }
}

@Composable
private fun SharedLedgerHeaderCard(ledger: SharedLedgerData) {
  val dateFormatted = remember(ledger.exportTimestamp) {
    SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date(ledger.exportTimestamp))
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = ledger.ownerName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
              text = "${ledger.ownerRole} · ${ledger.payCycleFrequency.lowercase().replaceFirstChar { it.uppercase() }}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
          }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "READ-ONLY",
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
          Text(
            text = stringResource(R.string.safe_to_spend_today),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
          )
          Text(
            text = "${ledger.currencySymbol}${"%.2f".format(ledger.safeToSpendToday)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = stringResource(R.string.remaining_in_cycle),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
          )
          Text(
            text = "${ledger.daysRemainingInCycle} days",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.shared_ledgers_last_synced, dateFormatted),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
      )
    }
  }
}

@Composable
private fun SharedLedgerKpiGrid(ledger: SharedLedgerData) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Total Income Card
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            tint = IncomeGreen,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = stringResource(R.string.total_income),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${ledger.currencySymbol}${"%.2f".format(ledger.totalIncome)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = IncomeGreen
        )
      }
    }

    // Total Spent Card
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
            contentDescription = null,
            tint = ExpenseRed,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = stringResource(R.string.total_spent),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${ledger.currencySymbol}${"%.2f".format(ledger.totalSpent)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = ExpenseRed
        )
      }
    }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Net Balance Card
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = stringResource(R.string.net_savings),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${ledger.currencySymbol}${"%.2f".format(ledger.netBalance)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
    }

    // Fixed Bills Card
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${ledger.recurringRulesCount} Fixed Bills",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${ledger.currencySymbol}${"%.2f".format(ledger.fixedBillsTotal)}/mo",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.secondary
        )
      }
    }
  }
}

@Composable
private fun SharedCategoryRow(category: SharedCategoryItem, currencySymbol: String) {
  val catColor = try {
    Color(android.graphics.Color.parseColor(category.colorHex))
  } catch (e: Exception) {
    MaterialTheme.colorScheme.primary
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 4.dp),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(catColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = CategoryIconHelper.getIconByName(category.iconName),
              contentDescription = category.name,
              tint = catColor,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = category.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
          )
        }

        Text(
          text = if (category.budgetAmount > 0) {
            "${currencySymbol}${"%.2f".format(category.spentAmount)} / ${currencySymbol}${"%.2f".format(category.budgetAmount)}"
          } else {
            "${currencySymbol}${"%.2f".format(category.spentAmount)}"
          },
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium,
          color = if (category.budgetAmount > 0 && category.spentAmount > category.budgetAmount) {
            ExpenseRed
          } else {
            MaterialTheme.colorScheme.onSurface
          }
        )
      }

      if (category.budgetAmount > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
          progress = { category.progress.coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = if (category.spentAmount > category.budgetAmount) ExpenseRed else catColor,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
      }
    }
  }
}

@Composable
private fun SharedTransactionRow(tx: SharedTransactionItem, currencySymbol: String) {
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
        Text(
          text = if (tx.note.isNotBlank()) tx.note else tx.categoryName,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
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
private fun EmptySharedLedgerState(onLoadDemo: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 30.dp, vertical = 40.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Default.AccountBalance,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        modifier = Modifier.size(56.dp)
      )
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = stringResource(R.string.shared_ledgers_no_file_loaded),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )
      Spacer(modifier = Modifier.height(16.dp))
      OutlinedButton(onClick = onLoadDemo, shape = RoundedCornerShape(12.dp)) {
        Text(text = stringResource(R.string.shared_ledgers_load_sample_btn))
      }
    }
  }
}

@Composable
private fun ShareGuideDialog(
  ownFileId: String?,
  onDismiss: () -> Unit,
  onCopyId: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.shared_ledgers_guide_title),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium
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

        if (!ownFileId.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(text = "Your Drive File ID:", style = MaterialTheme.typography.labelSmall)
                Text(
                  text = ownFileId,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              IconButton(onClick = onCopyId) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text(text = stringResource(R.string.shared_ledgers_guide_understood))
      }
    }
  )
}
