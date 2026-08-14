package com.example.spent.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spent.data.local.entity.PayCycleEntity
import com.example.spent.data.repository.SpentRepository
import com.example.spent.ui.settings.components.AppInfoCard
import com.example.spent.ui.settings.components.CurrencySelectionCard
import com.example.spent.ui.settings.components.ExportCsvCard
import com.example.spent.ui.settings.components.PayCycleCard
import com.example.spent.ui.settings.components.ResetDataButton
import com.example.spent.ui.settings.components.ThemeSelectionCard
import com.example.spent.ui.settings.components.UserProfileCard
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: SpentRepository
) {
    val scope = rememberCoroutineScope()

    val currentPayCycle by repository.getCurrentPayCycleFlow().collectAsState(initial = null)
    val isDarkThemeOverride by repository.isDarkThemeFlow.collectAsState(initial = null)
    val currencySymbol by repository.currencySymbolFlow.collectAsState(initial = "$")
    val transactions by repository.getTransactionsFlow().collectAsState(initial = emptyList())
    val categories by repository.getCategoriesFlow().collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "App preferences and data management",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Profile Section Component
        item {
            UserProfileCard()
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Pay Cycle Configuration Option
        item {
            PayCycleCard(
                currentPayCycle = currentPayCycle,
                currencySymbol = currencySymbol,
                onSavePayCycle = { frequency, income ->
                    scope.launch {
                        val updatedCycle = (currentPayCycle ?: PayCycleEntity()).copy(
                            frequency = frequency,
                            income = income
                        )
                        repository.setPayCycle(updatedCycle)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // App Theme Selector Option
        item {
            ThemeSelectionCard(
                isDarkThemeOverride = isDarkThemeOverride,
                onSelectThemeMode = { mode ->
                    scope.launch {
                        repository.setDarkThemeMode(mode)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Currency Selector Option
        item {
            CurrencySelectionCard(
                currentCurrencySymbol = currencySymbol,
                onSelectCurrencySymbol = { symbol ->
                    scope.launch {
                        repository.setCurrencySymbol(symbol)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Export Data (CSV) Option
        item {
            ExportCsvCard(
                transactions = transactions,
                categories = categories
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
                    scope.launch {
                        repository.resetAllData()
                    }
                }
            )
        }
    }
}
