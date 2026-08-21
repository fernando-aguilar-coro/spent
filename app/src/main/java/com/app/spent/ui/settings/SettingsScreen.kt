package com.app.spent.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.sync.GoogleDriveRestService
import com.app.spent.ui.settings.components.AppInfoCard
import com.app.spent.ui.settings.components.CurrencySelectionCard
import com.app.spent.ui.settings.components.ExportCsvCard
import com.app.spent.ui.settings.components.GoogleDriveSyncCard
import com.app.spent.ui.settings.components.ImageStorageLocationCard
import com.app.spent.ui.settings.components.LanguageSelectionCard
import com.app.spent.ui.settings.components.PayCycleCard
import com.app.spent.ui.settings.components.ResetDataButton
import com.app.spent.ui.settings.components.ThemeSelectionCard
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
@Composable
fun SettingsScreen(
viewModel: SettingsViewModel,
repository: SpentRepository
) {
  val state by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current

  val googleSignInLauncher = rememberLauncherForActivityResult(
  contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
    try {
      val account = task.getResult(ApiException::class.java)
      if (account != null) {
        viewModel.onIntent(SettingsUiIntent.ConnectDriveAccount(account))
      }
    } catch (e: ApiException) {
      if (e.statusCode != 12501) {
        val errorMsg = when (e.statusCode) {
          else -> "Sign-in error (${e.statusCode}): ${e.localizedMessage ?: "Unknown"}"
        }
        viewModel.onIntent(SettingsUiIntent.NotifySyncMessage(errorMsg))
      }
    } catch (e: Exception) {
      viewModel.onIntent(SettingsUiIntent.NotifySyncMessage("Error: ${e.localizedMessage ?: "Unknown error"}"))
    }
  }

  LaunchedEffect(Unit) {
    viewModel.effect.collect { effect ->
      when (effect) {
        is SettingsUiEffect.ShowSnackbar -> {
          snackbarHostState.showSnackbar(effect.message)
        }
        is SettingsUiEffect.LaunchDriveSignIn -> {
          val signInClient = GoogleDriveRestService.getGoogleSignInClient(context)
          googleSignInLauncher.launch(signInClient.signInIntent)
        }
      }
    }
  }

  Scaffold(
  snackbarHost = { SnackbarHost(snackbarHostState) },
  containerColor = MaterialTheme.colorScheme.background
  ) { innerPadding ->
    LazyColumn(
    modifier = Modifier
    .fillMaxSize()
    .padding(innerPadding),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
      item {
        Text(
        text = stringResource(R.string.settings_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
        )
        Text(
        text = stringResource(R.string.settings_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Google Drive Cloud Backup & Sync Option
      item {
        GoogleDriveSyncCard(
        isConnected = state.isDriveConnected,
        accountEmail = state.driveAccountEmail,
        lastSyncTimestamp = state.lastDriveSyncTimestamp,
        isSyncing = state.isDriveSyncing,
        onConnectClick = {
          viewModel.onIntent(SettingsUiIntent.RequestDriveSignIn)
        },
        onDisconnectClick = {
          viewModel.onIntent(SettingsUiIntent.DisconnectDrive)
        },
        onSyncNowClick = {
          viewModel.onIntent(SettingsUiIntent.SyncDriveNow)
        }
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Pay Cycle Configuration Option
      item {
        PayCycleCard(
        currentPayCycle = state.currentPayCycle,
        currencySymbol = state.currencySymbol,
        onSavePayCycle = { frequency, income, startDate ->
          viewModel.onIntent(SettingsUiIntent.SavePayCycle(frequency, income, startDate))
        }
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      // App Theme Selector Option
      item {
        ThemeSelectionCard(
        isDarkThemeOverride = state.isDarkThemeOverride,
        onSelectThemeMode = { mode ->
          viewModel.onIntent(SettingsUiIntent.SetDarkThemeMode(mode))
        }
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Image & Receipt Storage Location Option
      item {
        ImageStorageLocationCard(
        currentLocation = state.imageStorageLocation,
        onSelectLocation = { location ->
          viewModel.onIntent(SettingsUiIntent.SetImageStorageLocation(location))
        }
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Currency Selector Option
      item {
        CurrencySelectionCard(
        currentCurrencySymbol = state.currencySymbol,
        onSelectCurrencySymbol = { symbol ->
          viewModel.onIntent(SettingsUiIntent.SetCurrencySymbol(symbol))
        }
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Language Selector Option
      item {
        LanguageSelectionCard(
        currentLanguageCode = state.appLanguage,
        onSelectLanguage = { languageCode ->
          viewModel.onIntent(SettingsUiIntent.SetAppLanguage(languageCode))
        }
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Export Data (CSV) Option
      item {
        ExportCsvCard(
        transactions = state.transactions,
        categories = state.categories
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      // App Information Component
      item {
        AppInfoCard()
        Spacer(modifier = Modifier.height(24.dp))
      }

      // Data Reset Section Component
      item {
        ResetDataButton(
          isDriveConnected = state.isDriveConnected,
          onResetClick = { deleteDriveImages ->
            viewModel.onIntent(SettingsUiIntent.ResetAllData(deleteDriveImages))
          }
        )
      }
    }
  }
}
