package com.example.spent.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_WALKTHROUGH_COMPLETED = booleanPreferencesKey("is_walkthrough_completed")
        val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val SAVINGS_GOAL_NAME = stringPreferencesKey("savings_goal_name")
        val SAVINGS_GOAL_TOTAL = doublePreferencesKey("savings_goal_total")
        val SAVINGS_MONTHLY_CONTRIBUTION = doublePreferencesKey("savings_monthly_contribution")
        val LAST_DRIVE_SYNC_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("last_drive_sync_timestamp")
    }

    val lastDriveSyncTimestampFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_DRIVE_SYNC_TIMESTAMP] ?: 0L
    }

    val isWalkthroughCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_WALKTHROUGH_COMPLETED] ?: false
    }

    val isDarkThemeFlow: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_THEME_ENABLED]
    }

    val currencySymbolFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENCY_SYMBOL] ?: "$"
    }

    val appLanguageFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE]
    }

    val savingsGoalNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SAVINGS_GOAL_NAME] ?: ""
    }

    val savingsGoalTotalFlow: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SAVINGS_GOAL_TOTAL] ?: 0.0
    }

    val savingsMonthlyContributionFlow: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SAVINGS_MONTHLY_CONTRIBUTION] ?: 0.0
    }

    suspend fun setWalkthroughCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_WALKTHROUGH_COMPLETED] = completed
        }
    }

    suspend fun setDarkThemeMode(enabled: Boolean?) {
        context.dataStore.edit { preferences ->
            if (enabled == null) {
                preferences.remove(PreferencesKeys.DARK_THEME_ENABLED)
            } else {
                preferences[PreferencesKeys.DARK_THEME_ENABLED] = enabled
            }
        }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun setAppLanguage(languageCode: String?) {
        context.dataStore.edit { preferences ->
            if (languageCode == null) {
                preferences.remove(PreferencesKeys.APP_LANGUAGE)
            } else {
                preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
            }
        }
    }

    suspend fun setSavingsGoal(name: String, totalGoal: Double, monthlyContribution: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVINGS_GOAL_NAME] = name
            preferences[PreferencesKeys.SAVINGS_GOAL_TOTAL] = totalGoal
            preferences[PreferencesKeys.SAVINGS_MONTHLY_CONTRIBUTION] = monthlyContribution
        }
    }

    suspend fun clearSavingsGoal() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SAVINGS_GOAL_NAME)
            preferences.remove(PreferencesKeys.SAVINGS_GOAL_TOTAL)
            preferences.remove(PreferencesKeys.SAVINGS_MONTHLY_CONTRIBUTION)
        }
    }

    suspend fun setLastDriveSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_DRIVE_SYNC_TIMESTAMP] = timestamp
        }
    }
}
