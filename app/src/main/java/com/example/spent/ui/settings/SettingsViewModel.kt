package com.example.spent.ui.settings

import androidx.lifecycle.viewModelScope
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.PayCycleEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.data.repository.SpentRepository
import com.example.spent.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SpentRepository
) : BaseViewModel<SettingsUiState, SettingsUiIntent, SettingsUiEffect>(SettingsUiState()) {

    init {
        observeSettingsData()
    }

    private fun observeSettingsData() {
        viewModelScope.launch {
            val coreFlow = combine(
                repository.getCurrentPayCycleFlow(),
                repository.isDarkThemeFlow,
                repository.currencySymbolFlow,
                repository.appLanguageFlow,
                repository.lastDriveSyncTimestampFlow
            ) { payCycle, isDark, currency, language, lastSync ->
                SettingsCoreData(payCycle, isDark, currency, language, lastSync)
            }

            val dataFlow = combine(
                repository.getTransactionsFlow(),
                repository.getCategoriesFlow()
            ) { transactions, categories ->
                Pair(transactions, categories)
            }

            combine(coreFlow, dataFlow) { core, (transactions, categories) ->
                SettingsUiState(
                    currentPayCycle = core.payCycle,
                    isDarkThemeOverride = core.isDark,
                    currencySymbol = core.currency,
                    appLanguage = core.language,
                    transactions = transactions,
                    categories = categories,
                    lastDriveSyncTimestamp = core.lastSync,
                    isLoading = false
                )
            }.collect { newState ->
                setState { newState }
            }
        }
    }

    private data class SettingsCoreData(
        val payCycle: PayCycleEntity?,
        val isDark: Boolean?,
        val currency: String,
        val language: String?,
        val lastSync: Long
    )

    override fun onIntent(intent: SettingsUiIntent) {
        when (intent) {
            is SettingsUiIntent.SavePayCycle -> savePayCycle(intent.frequency, intent.income, intent.startDate)
            is SettingsUiIntent.SetDarkThemeMode -> setThemeMode(intent.isDark)
            is SettingsUiIntent.SetCurrencySymbol -> setCurrencySymbol(intent.symbol)
            is SettingsUiIntent.SetAppLanguage -> setAppLanguage(intent.languageCode)
            is SettingsUiIntent.NotifySyncMessage -> sendEffect(SettingsUiEffect.ShowSnackbar(intent.message))
            is SettingsUiIntent.ResetAllData -> resetAllData()
        }
    }

    private fun savePayCycle(frequency: String, income: Double, startDate: Long) {
        viewModelScope.launch {
            val current = currentState.currentPayCycle ?: PayCycleEntity()
            val updated = current.copy(
                frequency = frequency,
                income = income,
                startDate = startDate
            )
            repository.setPayCycle(updated)
            sendEffect(SettingsUiEffect.ShowSnackbar("Pay cycle updated"))
        }
    }

    private fun setThemeMode(isDark: Boolean?) {
        viewModelScope.launch {
            repository.setDarkThemeMode(isDark)
        }
    }

    private fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            repository.setCurrencySymbol(symbol)
            sendEffect(SettingsUiEffect.ShowSnackbar("Currency updated to $symbol"))
        }
    }

    private fun setAppLanguage(languageCode: String?) {
        viewModelScope.launch {
            repository.setAppLanguage(languageCode)
        }
    }

    private fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            sendEffect(SettingsUiEffect.ShowSnackbar("All data has been reset"))
        }
    }
}
