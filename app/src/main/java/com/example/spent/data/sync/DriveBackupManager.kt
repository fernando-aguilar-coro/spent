package com.example.spent.data.sync

import android.content.Context
import android.net.Uri
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.PayCycleEntity
import com.example.spent.data.local.entity.RecurringRuleEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.data.local.entity.UserAccountEntity
import com.example.spent.data.repository.SpentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DriveBackupManager {

    private const val BACKUP_VERSION = 1

    suspend fun generateBackupJson(repository: SpentRepository): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Spent")
        root.put("version", BACKUP_VERSION)
        val now = System.currentTimeMillis()
        root.put("exportTimestamp", now)

        // 1. Categories
        val categories = repository.getCategoriesFlow().firstOrNull() ?: emptyList()
        val catArray = JSONArray()
        for (c in categories) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("iconName", c.iconName)
                put("colorHex", c.colorHex)
                put("budgetAmount", c.budgetAmount)
                put("displayOrder", c.displayOrder)
            }
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // 2. Transactions
        val transactions = repository.getTransactionsFlow().firstOrNull() ?: emptyList()
        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("ownerProfileId", tx.ownerProfileId)
                put("amount", tx.amount)
                put("type", tx.type)
                put("categoryId", tx.categoryId)
                put("timestamp", tx.timestamp)
                put("note", tx.note)
                put("recurringRuleId", tx.recurringRuleId ?: "")
            }
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // 3. Pay Cycle
        val payCycle = repository.getCurrentPayCycleFlow().firstOrNull()
        if (payCycle != null) {
            val pcObj = JSONObject().apply {
                put("id", payCycle.id)
                put("frequency", payCycle.frequency)
                put("startDate", payCycle.startDate)
                put("income", payCycle.income)
            }
            root.put("payCycle", pcObj)
        }

        // 4. Recurring Rules
        val recurringRules = repository.getRecurringRulesFlow().firstOrNull() ?: emptyList()
        val rrArray = JSONArray()
        for (r in recurringRules) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("ownerProfileId", r.ownerProfileId)
                put("amount", r.amount)
                put("categoryId", r.categoryId)
                put("note", r.note)
                put("frequency", r.frequency)
                put("startDate", r.startDate)
                put("endDate", r.endDate ?: -1L)
                put("lastExecuted", r.lastExecuted)
            }
            rrArray.put(obj)
        }
        root.put("recurringRules", rrArray)

        // 5. User Account
        val userAccount = repository.getUserAccountFlow().firstOrNull()
        if (userAccount != null) {
            val uaObj = JSONObject().apply {
                put("id", userAccount.id)
                put("displayName", userAccount.displayName)
                put("role", userAccount.role)
                put("lastDriveSyncTimestamp", now)
            }
            root.put("userAccount", uaObj)
        }

        // 6. Preferences
        val currency = repository.currencySymbolFlow.firstOrNull() ?: "$"
        val darkTheme = repository.isDarkThemeFlow.firstOrNull()
        val language = repository.appLanguageFlow.firstOrNull()
        val savingsGoalName = repository.savingsGoalNameFlow.firstOrNull() ?: ""
        val savingsGoalTotal = repository.savingsGoalTotalFlow.firstOrNull() ?: 0.0
        val savingsMonthly = repository.savingsMonthlyContributionFlow.firstOrNull() ?: 0.0

        val prefObj = JSONObject().apply {
            put("currencySymbol", currency)
            if (darkTheme != null) put("darkTheme", darkTheme)
            if (language != null) put("appLanguage", language)
            put("savingsGoalName", savingsGoalName)
            put("savingsGoalTotal", savingsGoalTotal)
            put("savingsMonthlyContribution", savingsMonthly)
        }
        root.put("preferences", prefObj)

        root.toString(2)
    }

    suspend fun restoreFromJson(jsonString: String, repository: SpentRepository): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Categories
            val categoriesList = mutableListOf<CategoryEntity>()
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    categoriesList.add(
                        CategoryEntity(
                            id = obj.optString("id", "cat_$i"),
                            name = obj.optString("name", "Category"),
                            iconName = obj.optString("iconName", "Category"),
                            colorHex = obj.optString("colorHex", "#64748B"),
                            budgetAmount = obj.optDouble("budgetAmount", 0.0),
                            displayOrder = obj.optInt("displayOrder", i)
                        )
                    )
                }
            }

            // Transactions
            val transactionsList = mutableListOf<TransactionEntity>()
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val recRuleId = obj.optString("recurringRuleId", "").ifEmpty { null }
                    transactionsList.add(
                        TransactionEntity(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            ownerProfileId = obj.optString("ownerProfileId", "primary_user"),
                            amount = obj.optDouble("amount", 0.0),
                            type = obj.optString("type", "EXPENSE"),
                            categoryId = obj.optString("categoryId", "cat_general"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            note = obj.optString("note", ""),
                            recurringRuleId = recRuleId
                        )
                    )
                }
            }

            // Pay Cycle
            var payCycle: PayCycleEntity? = null
            if (root.has("payCycle")) {
                val obj = root.getJSONObject("payCycle")
                payCycle = PayCycleEntity(
                    id = obj.optString("id", "default_cycle"),
                    frequency = obj.optString("frequency", "MONTHLY"),
                    startDate = obj.optLong("startDate", System.currentTimeMillis()),
                    income = obj.optDouble("income", 0.0)
                )
            }

            // Recurring Rules
            val recurringRulesList = mutableListOf<RecurringRuleEntity>()
            if (root.has("recurringRules")) {
                val rrArray = root.getJSONArray("recurringRules")
                for (i in 0 until rrArray.length()) {
                    val obj = rrArray.getJSONObject(i)
                    val endTs = obj.optLong("endDate", -1L)
                    recurringRulesList.add(
                        RecurringRuleEntity(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            ownerProfileId = obj.optString("ownerProfileId", "primary_account"),
                            amount = obj.optDouble("amount", 0.0),
                            categoryId = obj.optString("categoryId", "cat_general"),
                            frequency = obj.optString("frequency", "MONTHLY"),
                            startDate = obj.optLong("startDate", System.currentTimeMillis()),
                            endDate = if (endTs > 0) endTs else null,
                            lastExecuted = obj.optLong("lastExecuted", 0L),
                            note = obj.optString("note", "")
                        )
                    )
                }
            }

            // User Account
            var userAccount: UserAccountEntity? = null
            if (root.has("userAccount")) {
                val obj = root.getJSONObject("userAccount")
                userAccount = UserAccountEntity(
                    id = obj.optString("id", "primary_account"),
                    displayName = obj.optString("displayName", "Primary User"),
                    role = obj.optString("role", "INDEPENDENT"),
                    lastDriveSyncTimestamp = System.currentTimeMillis()
                )
            }

            // Restore Database in Repository
            repository.restoreAllData(
                categories = categoriesList,
                transactions = transactionsList,
                payCycle = payCycle,
                recurringRules = recurringRulesList,
                userAccount = userAccount
            )

            // Restore Preferences
            if (root.has("preferences")) {
                val prefObj = root.getJSONObject("preferences")
                if (prefObj.has("currencySymbol")) {
                    repository.setCurrencySymbol(prefObj.getString("currencySymbol"))
                }
                if (prefObj.has("darkTheme")) {
                    repository.setDarkThemeMode(prefObj.getBoolean("darkTheme"))
                }
                if (prefObj.has("appLanguage")) {
                    repository.setAppLanguage(prefObj.getString("appLanguage"))
                }
                if (prefObj.has("savingsGoalName") || prefObj.has("savingsGoalTotal")) {
                    val name = prefObj.optString("savingsGoalName", "")
                    val total = prefObj.optDouble("savingsGoalTotal", 0.0)
                    val monthly = prefObj.optDouble("savingsMonthlyContribution", 0.0)
                    if (name.isNotBlank() || total > 0) {
                        repository.setSavingsGoal(name, total, monthly)
                    }
                }
            }

            val now = System.currentTimeMillis()
            repository.setLastDriveSyncTimestamp(now)
            repository.setWalkthroughCompleted(true)

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun createLocalBackupFile(context: Context, jsonString: String): File {
        val exportDir = File(context.cacheDir, "drive_backups").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "Spent_Backup_$timeStamp.json"
        val file = File(exportDir, fileName)
        FileOutputStream(file).use { out ->
            out.write(jsonString.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    fun writeBackupToUri(context: Context, uri: Uri, jsonString: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
