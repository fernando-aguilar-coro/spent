package com.example.spent.ui.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spent.R
import com.example.spent.data.repository.SpentRepository
import com.example.spent.ui.settings.components.AppInfoCard
import com.example.spent.ui.settings.components.CurrencySelectionCard
import com.example.spent.ui.settings.components.ExportCsvCard
import com.example.spent.ui.settings.components.GoogleDriveSyncCard
import com.example.spent.ui.settings.components.LanguageSelectionCard
import com.example.spent.ui.settings.components.PayCycleCard
import com.example.spent.ui.settings.components.ResetDataButton
import com.example.spent.ui.settings.components.ThemeSelectionCard

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    repository: SpentRepository
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
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
                    repository = repository,
                    lastSyncTimestamp = state.lastDriveSyncTimestamp,
                    onSyncComplete = { message ->
                        viewModel.onIntent(SettingsUiIntent.NotifySyncMessage(message))
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
                    onResetClick = {
                        viewModel.onIntent(SettingsUiIntent.ResetAllData)
                    }
                )
            }
        }
    }
}
