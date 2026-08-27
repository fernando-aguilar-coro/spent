package com.app.spent.data.preferences

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
import com.app.spent.util.LocaleHelper

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
    val IS_DRIVE_CONNECTED = booleanPreferencesKey("is_drive_connected")
    val DRIVE_ACCOUNT_EMAIL = stringPreferencesKey("drive_account_email")
    val PARTNER_DRIVE_FILE_ID = stringPreferencesKey("partner_drive_file_id")
    val PARTNER_NAME = stringPreferencesKey("partner_name")
    val PARTNER_EMAIL = stringPreferencesKey("partner_email")
    val PARTNER_LAST_SYNC_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("partner_last_sync_timestamp")
    val IS_PARTNER_PAIRED = booleanPreferencesKey("is_partner_paired")
    val SHARED_MEMBERS_JSON = stringPreferencesKey("shared_members_json")
    val IMAGE_STORAGE_LOCATION = stringPreferencesKey("image_storage_location")
  }

  val imageStorageLocationFlow: Flow<String> = context.dataStore.data.map { preferences ->
    val explicitLocation = preferences[PreferencesKeys.IMAGE_STORAGE_LOCATION]
    if (explicitLocation != null) {
      explicitLocation
    } else {
      val isDriveConnected = preferences[PreferencesKeys.IS_DRIVE_CONNECTED] ?: false
      if (isDriveConnected) "GOOGLE_DRIVE" else "IN_APP"
    }
  }

  val sharedMembersFlow: Flow<List<com.app.spent.data.sync.SharedMemberInfo>> = context.dataStore.data.map { preferences ->
    val json = preferences[PreferencesKeys.SHARED_MEMBERS_JSON]
    if (!json.isNullOrBlank()) {
      try {
        val array = org.json.JSONArray(json)
        val list = mutableListOf<com.app.spent.data.sync.SharedMemberInfo>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val nickname = obj.optString("customNickname", "").ifBlank { null }
          list.add(
            com.app.spent.data.sync.SharedMemberInfo(
              fileId = obj.optString("fileId", ""),
              name = obj.optString("name", "Member"),
              role = obj.optString("role", "Member"),
              lastSyncTimestamp = obj.optLong("lastSyncTimestamp", 0L),
              isLocal = obj.optBoolean("isLocal", false),
              customNickname = nickname
            )
          )
        }
        list
      } catch (e: Exception) {
        emptyList()
      }
    } else {
      val partnerId = preferences[PreferencesKeys.PARTNER_DRIVE_FILE_ID]
      val partnerName = preferences[PreferencesKeys.PARTNER_NAME] ?: "Member"
      val partnerSync = preferences[PreferencesKeys.PARTNER_LAST_SYNC_TIMESTAMP] ?: 0L
      if (!partnerId.isNullOrBlank()) {
        listOf(com.app.spent.data.sync.SharedMemberInfo(fileId = partnerId, name = partnerName, role = "Member", lastSyncTimestamp = partnerSync))
      } else {
        emptyList()
      }
    }
  }

  val partnerDriveFileIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
    preferences[PreferencesKeys.PARTNER_DRIVE_FILE_ID]
  }

  val partnerNameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
    preferences[PreferencesKeys.PARTNER_NAME]
  }

  val partnerEmailFlow: Flow<String?> = context.dataStore.data.map { preferences ->
    preferences[PreferencesKeys.PARTNER_EMAIL]
  }

  val partnerLastSyncTimestampFlow: Flow<Long> = context.dataStore.data.map { preferences ->
    preferences[PreferencesKeys.PARTNER_LAST_SYNC_TIMESTAMP] ?: 0L
  }

  val isPartnerPairedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[PreferencesKeys.IS_PARTNER_PAIRED] ?: false
  }

  val lastDriveSyncTimestampFlow: Flow<Long> = context.dataStore.data.map { preferences ->
    preferences[PreferencesKeys.LAST_DRIVE_SYNC_TIMESTAMP] ?: 0L
  }

  val isDriveConnectedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[PreferencesKeys.IS_DRIVE_CONNECTED] ?: false
  }

  val driveAccountEmailFlow: Flow<String?> = context.dataStore.data.map { preferences ->
    preferences[PreferencesKeys.DRIVE_ACCOUNT_EMAIL]
  }

  val isWalkthroughCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[PreferencesKeys.IS_WALKTHROUGH_COMPLETED] ?: false
  }

  val isDarkThemeFlow: Flow<Boolean?> = context.dataStore.data.map { preferences ->
    preferences[PreferencesKeys.DARK_THEME_ENABLED]
  }

  val currencySymbolFlow: Flow<String> = context.dataStore.data.map { preferences ->
    val storedSymbol = preferences[PreferencesKeys.CURRENCY_SYMBOL]
    if (!storedSymbol.isNullOrBlank()) {
      storedSymbol
    } else {
      LocaleHelper.getSystemCurrencySymbol(context)
    }
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

  suspend fun setDriveAccount(email: String?) {
    context.dataStore.edit { preferences ->
      if (email != null) {
        preferences[PreferencesKeys.IS_DRIVE_CONNECTED] = true
        preferences[PreferencesKeys.DRIVE_ACCOUNT_EMAIL] = email
      } else {
        preferences[PreferencesKeys.IS_DRIVE_CONNECTED] = false
        preferences.remove(PreferencesKeys.DRIVE_ACCOUNT_EMAIL)
      }
    }
  }

  suspend fun clearDriveAccount() {
    context.dataStore.edit { preferences ->
      preferences[PreferencesKeys.IS_DRIVE_CONNECTED] = false
      preferences.remove(PreferencesKeys.DRIVE_ACCOUNT_EMAIL)
      preferences[PreferencesKeys.LAST_DRIVE_SYNC_TIMESTAMP] = 0L
    }
  }

  suspend fun savePartnerInfo(fileId: String, name: String, email: String?) {
    context.dataStore.edit { preferences ->
      preferences[PreferencesKeys.PARTNER_DRIVE_FILE_ID] = fileId
      preferences[PreferencesKeys.PARTNER_NAME] = name
      if (email != null) {
        preferences[PreferencesKeys.PARTNER_EMAIL] = email
      } else {
        preferences.remove(PreferencesKeys.PARTNER_EMAIL)
      }
      preferences[PreferencesKeys.IS_PARTNER_PAIRED] = true
      preferences[PreferencesKeys.PARTNER_LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
    }
  }

  suspend fun setPartnerLastSyncTimestamp(timestamp: Long) {
    context.dataStore.edit { preferences ->
      preferences[PreferencesKeys.PARTNER_LAST_SYNC_TIMESTAMP] = timestamp
    }
  }

  suspend fun addOrUpdateSharedMember(member: com.app.spent.data.sync.SharedMemberInfo) {
    context.dataStore.edit { preferences ->
      val currentJson = preferences[PreferencesKeys.SHARED_MEMBERS_JSON]
      val list = mutableListOf<com.app.spent.data.sync.SharedMemberInfo>()
      if (!currentJson.isNullOrBlank()) {
        try {
          val array = org.json.JSONArray(currentJson)
          for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val nickname = obj.optString("customNickname", "").ifBlank { null }
            list.add(
              com.app.spent.data.sync.SharedMemberInfo(
                fileId = obj.optString("fileId", ""),
                name = obj.optString("name", "Member"),
                role = obj.optString("role", "Member"),
                lastSyncTimestamp = obj.optLong("lastSyncTimestamp", 0L),
                isLocal = obj.optBoolean("isLocal", false),
                customNickname = nickname
              )
            )
          }
        } catch (e: Exception) {}
      }

      val index = list.indexOfFirst { it.fileId == member.fileId }
      if (index >= 0) {
        val existing = list[index]
        val preservedNickname = member.customNickname ?: existing.customNickname
        list[index] = member.copy(customNickname = preservedNickname)
      } else {
        list.add(member)
      }

      val newArray = org.json.JSONArray()
      for (m in list) {
        val obj = org.json.JSONObject().apply {
          put("fileId", m.fileId)
          put("name", m.name)
          put("role", m.role)
          put("lastSyncTimestamp", m.lastSyncTimestamp)
          put("isLocal", m.isLocal)
          if (!m.customNickname.isNullOrBlank()) {
            put("customNickname", m.customNickname)
          }
        }
        newArray.put(obj)
      }
      preferences[PreferencesKeys.SHARED_MEMBERS_JSON] = newArray.toString()
    }
  }

  suspend fun updateSharedMemberName(fileId: String, newName: String) {
    context.dataStore.edit { preferences ->
      val currentJson = preferences[PreferencesKeys.SHARED_MEMBERS_JSON]
      val list = mutableListOf<com.app.spent.data.sync.SharedMemberInfo>()
      if (!currentJson.isNullOrBlank()) {
        try {
          val array = org.json.JSONArray(currentJson)
          for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val nickname = obj.optString("customNickname", "").ifBlank { null }
            list.add(
              com.app.spent.data.sync.SharedMemberInfo(
                fileId = obj.optString("fileId", ""),
                name = obj.optString("name", "Member"),
                role = obj.optString("role", "Member"),
                lastSyncTimestamp = obj.optLong("lastSyncTimestamp", 0L),
                isLocal = obj.optBoolean("isLocal", false),
                customNickname = nickname
              )
            )
          }
        } catch (e: Exception) {}
      }

      val index = list.indexOfFirst { it.fileId == fileId }
      if (index >= 0) {
        val existing = list[index]
        list[index] = existing.copy(name = newName, customNickname = newName)
      }

      val newArray = org.json.JSONArray()
      for (m in list) {
        val obj = org.json.JSONObject().apply {
          put("fileId", m.fileId)
          put("name", m.name)
          put("role", m.role)
          put("lastSyncTimestamp", m.lastSyncTimestamp)
          put("isLocal", m.isLocal)
          if (!m.customNickname.isNullOrBlank()) {
            put("customNickname", m.customNickname)
          }
        }
        newArray.put(obj)
      }
      preferences[PreferencesKeys.SHARED_MEMBERS_JSON] = newArray.toString()
    }
  }

  suspend fun removeSharedMember(fileId: String) {
    context.dataStore.edit { preferences ->
      val currentJson = preferences[PreferencesKeys.SHARED_MEMBERS_JSON]
      if (!currentJson.isNullOrBlank()) {
        try {
          val array = org.json.JSONArray(currentJson)
          val newArray = org.json.JSONArray()
          for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.optString("fileId") != fileId) {
              newArray.put(obj)
            }
          }
          preferences[PreferencesKeys.SHARED_MEMBERS_JSON] = newArray.toString()
        } catch (e: Exception) {}
      }
      if (preferences[PreferencesKeys.PARTNER_DRIVE_FILE_ID] == fileId) {
        preferences.remove(PreferencesKeys.PARTNER_DRIVE_FILE_ID)
        preferences.remove(PreferencesKeys.PARTNER_NAME)
        preferences[PreferencesKeys.IS_PARTNER_PAIRED] = false
      }
    }
  }

  suspend fun clearSharedMembers() {
    context.dataStore.edit { preferences ->
      preferences.remove(PreferencesKeys.SHARED_MEMBERS_JSON)
      preferences.remove(PreferencesKeys.PARTNER_DRIVE_FILE_ID)
      preferences.remove(PreferencesKeys.PARTNER_NAME)
      preferences[PreferencesKeys.IS_PARTNER_PAIRED] = false
    }
  }

  suspend fun clearPartnerInfo() {
    clearSharedMembers()
  }

  suspend fun setImageStorageLocation(location: String) {
    context.dataStore.edit { preferences ->
      preferences[PreferencesKeys.IMAGE_STORAGE_LOCATION] = location
    }
  }
}
