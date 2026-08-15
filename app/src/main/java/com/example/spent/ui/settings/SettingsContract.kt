package com.example.spent.ui.settings

import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.PayCycleEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.ui.mvi.UiEffect
import com.example.spent.ui.mvi.UiIntent
import com.example.spent.ui.mvi.UiState

data class SettingsUiState(
    val currentPayCycle: PayCycleEntity? = null,
    val isDarkThemeOverride: Boolean? = null,
    val currencySymbol: String = "$",
    val appLanguage: String? = null,
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = false
) : UiState

sealed class SettingsUiIntent : UiIntent {
    data class SavePayCycle(val frequency: String, val income: Double, val startDate: Long) : SettingsUiIntent()
    data class SetDarkThemeMode(val isDark: Boolean?) : SettingsUiIntent()
    data class SetCurrencySymbol(val symbol: String) : SettingsUiIntent()
    data class SetAppLanguage(val languageCode: String?) : SettingsUiIntent()
    object ResetAllData : SettingsUiIntent()
}

sealed class SettingsUiEffect : UiEffect {
    data class ShowSnackbar(val message: String) : SettingsUiEffect()
}
