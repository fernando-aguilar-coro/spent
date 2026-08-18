package com.app.spent.data.sync

import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.TransactionEntity
import org.json.JSONObject

enum class HouseholdSource {
  YOU,
  PARTNER
}

data class HouseholdTransactionItem(
  val id: String,
  val amount: Double,
  val type: String, // INCOME, EXPENSE
  val categoryName: String,
  val categoryColorHex: String,
  val timestamp: Long,
  val note: String,
  val source: HouseholdSource,
  val authorName: String
)

data class HouseholdCategoryEnvelope(
  val name: String,
  val iconName: String,
  val colorHex: String,
  val totalBudget: Double,
  val totalSpent: Double,
  val mySpent: Double,
  val partnerSpent: Double,
  val progress: Float
)

data class HouseholdSummary(
  val combinedIncome: Double,
  val combinedSpent: Double,
  val combinedNetBalance: Double,
  val combinedSafeToSpendToday: Double,
  val myTotalIncome: Double,
  val myTotalSpent: Double,
  val mySafeToSpendToday: Double,
  val partnerTotalIncome: Double,
  val partnerTotalSpent: Double,
  val partnerSafeToSpendToday: Double,
  val daysRemainingInCycle: Int,
  val currencySymbol: String
)

data class HouseholdUnifiedData(
  val summary: HouseholdSummary,
  val categories: List<HouseholdCategoryEnvelope>,
  val transactions: List<HouseholdTransactionItem>,
  val partnerName: String,
  val partnerLastUpdated: Long,
  val isPartnerActive: Boolean
)

data class DriveBackupFileInfo(
  val id: String,
  val name: String,
  val modifiedTime: Long = 0L,
  val ownerEmail: String? = null
)

data class SharedCategoryItem(
  val id: String,
  val name: String,
  val iconName: String,
  val colorHex: String,
  val budgetAmount: Double,
  val spentAmount: Double,
  val progress: Float
)

data class SharedTransactionItem(
  val id: String,
  val amount: Double,
  val type: String,
  val categoryId: String,
  val categoryName: String,
  val categoryColorHex: String,
  val timestamp: Long,
  val note: String
)

data class SharedLedgerData(
  val ownerName: String,
  val ownerRole: String,
  val exportTimestamp: Long,
  val currencySymbol: String,
  val totalIncome: Double,
  val totalSpent: Double,
  val netBalance: Double,
  val safeToSpendToday: Double,
  val daysRemainingInCycle: Int,
  val savingsGoalName: String,
  val savingsGoalTotal: Double,
  val savingsTotalSaved: Double,
  val savingsMonthlyContribution: Double,
  val categories: List<SharedCategoryItem>,
  val recentTransactions: List<SharedTransactionItem>,
  val recurringRulesCount: Int,
  val fixedBillsTotal: Double,
  val payCycleFrequency: String,
  val payCycleIncome: Double,
  val sourceFileId: String? = null,
  val sourceFileName: String? = null
)

object SharedLedgerParser {

  fun parse(jsonString: String, fileId: String? = null, fileName: String? = null): Result<SharedLedgerData> {
    return try {
      val root = JSONObject(jsonString)

      // 1. User Account / Owner
      var ownerName = "Partner"
      var ownerRole = "Shared User"
      var exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

      if (root.has("userAccount")) {
        val uaObj = root.getJSONObject("userAccount")
        ownerName = uaObj.optString("displayName", ownerName)
        ownerRole = uaObj.optString("role", ownerRole)
        if (uaObj.has("lastDriveSyncTimestamp")) {
          exportTimestamp = uaObj.optLong("lastDriveSyncTimestamp", exportTimestamp)
        }
      }

      // 2. Preferences
      var currencySymbol = "$"
      var savingsGoalName = ""
      var savingsGoalTotal = 0.0
      var savingsMonthlyContribution = 0.0

      if (root.has("preferences")) {
        val prefObj = root.getJSONObject("preferences")
        currencySymbol = prefObj.optString("currencySymbol", "$")
        savingsGoalName = prefObj.optString("savingsGoalName", "")
        savingsGoalTotal = prefObj.optDouble("savingsGoalTotal", 0.0)
        savingsMonthlyContribution = prefObj.optDouble("savingsMonthlyContribution", 0.0)
      }

      // 3. Pay Cycle
      var payCycleFrequency = "MONTHLY"
      var payCycleIncome = 0.0
      var payCycleStartDate = System.currentTimeMillis()

      if (root.has("payCycle")) {
        val pcObj = root.getJSONObject("payCycle")
        payCycleFrequency = pcObj.optString("frequency", "MONTHLY")
        payCycleIncome = pcObj.optDouble("income", 0.0)
        payCycleStartDate = pcObj.optLong("startDate", System.currentTimeMillis())
      }

      // 4. Categories Map
      val categoryMap = mutableMapOf<String, Triple<String, String, Double>>() // id -> (name, colorHex, budget)
      val categoryIcons = mutableMapOf<String, String>()

      if (root.has("categories")) {
        val catArray = root.getJSONArray("categories")
        for (i in 0 until catArray.length()) {
          val catObj = catArray.getJSONObject(i)
          val id = catObj.optString("id", "cat_$i")
          val name = catObj.optString("name", "Category")
          val color = catObj.optString("colorHex", "#64748B")
          val budget = catObj.optDouble("budgetAmount", 0.0)
          val icon = catObj.optString("iconName", "Category")
          categoryMap[id] = Triple(name, color, budget)
          categoryIcons[id] = icon
        }
      }

      // 5. Transactions
      val txList = mutableListOf<SharedTransactionItem>()
      val categorySpentMap = mutableMapOf<String, Double>()
      var totalIncome = 0.0
      var totalSpent = 0.0
      var savingsSaved = 0.0

      if (root.has("transactions")) {
        val txArray = root.getJSONArray("transactions")
        for (i in 0 until txArray.length()) {
          val txObj = txArray.getJSONObject(i)
          val id = txObj.optString("id", "tx_$i")
          val amount = txObj.optDouble("amount", 0.0)
          val type = txObj.optString("type", "EXPENSE")
          val catId = txObj.optString("categoryId", "")
          val timestamp = txObj.optLong("timestamp", System.currentTimeMillis())
          val note = txObj.optString("note", "")

          val catTriple = categoryMap[catId] ?: Triple("General", "#64748B", 0.0)
          val catName = catTriple.first
          val catColor = catTriple.second

          if (type.equals("INCOME", ignoreCase = true)) {
            totalIncome += amount
          } else {
            totalSpent += amount
            val current = categorySpentMap[catId] ?: 0.0
            categorySpentMap[catId] = current + amount

            if (catId == "cat_savings" || catName.equals("Savings", ignoreCase = true)) {
              savingsSaved += amount
            }
          }

          txList.add(
            SharedTransactionItem(
              id = id,
              amount = amount,
              type = type,
              categoryId = catId,
              categoryName = catName,
              categoryColorHex = catColor,
              timestamp = timestamp,
              note = note
            )
          )
        }
      }

      // Sort transactions newest first
      txList.sortByDescending { it.timestamp }

      // 6. Category Items
      val categoryItems = categoryMap.map { (catId, triple) ->
        val spent = categorySpentMap[catId] ?: 0.0
        val budget = triple.third
        val progress = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1.5f) else 0f
        SharedCategoryItem(
          id = catId,
          name = triple.first,
          iconName = categoryIcons[catId] ?: "Category",
          colorHex = triple.second,
          budgetAmount = budget,
          spentAmount = spent,
          progress = progress
        )
      }

      // 7. Recurring Rules & Bills
      var recurringRulesCount = 0
      var fixedBillsTotal = 0.0
      if (root.has("recurringRules")) {
        val rrArray = root.getJSONArray("recurringRules")
        recurringRulesCount = rrArray.length()
        for (i in 0 until rrArray.length()) {
          val rObj = rrArray.getJSONObject(i)
          val amount = rObj.optDouble("amount", 0.0)
          fixedBillsTotal += amount
        }
      }

      // 8. Calculations for Safe to spend & Days remaining
      val now = System.currentTimeMillis()
      val daysRemaining = calculateDaysRemaining(payCycleFrequency, payCycleStartDate, now)
      val effectiveIncome = if (payCycleIncome > 0) payCycleIncome else totalIncome
      val remainingDisposable = (effectiveIncome - totalSpent - savingsMonthlyContribution).coerceAtLeast(0.0)
      val safeToSpendToday = if (daysRemaining > 0) remainingDisposable / daysRemaining else remainingDisposable
      val netBalance = (totalIncome - totalSpent)

      val sharedLedger = SharedLedgerData(
        ownerName = ownerName,
        ownerRole = ownerRole,
        exportTimestamp = exportTimestamp,
        currencySymbol = currencySymbol,
        totalIncome = totalIncome,
        totalSpent = totalSpent,
        netBalance = netBalance,
        safeToSpendToday = safeToSpendToday,
        daysRemainingInCycle = daysRemaining,
        savingsGoalName = savingsGoalName,
        savingsGoalTotal = savingsGoalTotal,
        savingsTotalSaved = savingsSaved,
        savingsMonthlyContribution = savingsMonthlyContribution,
        categories = categoryItems,
        recentTransactions = txList.take(20),
        recurringRulesCount = recurringRulesCount,
        fixedBillsTotal = fixedBillsTotal,
        payCycleFrequency = payCycleFrequency,
        payCycleIncome = payCycleIncome,
        sourceFileId = fileId,
        sourceFileName = fileName ?: "spent_backup.json"
      )

      Result.success(sharedLedger)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  private fun calculateDaysRemaining(frequency: String, startDate: Long, now: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = startDate }
    val nowCal = Calendar.getInstance().apply { timeInMillis = now }

    val diffDays = ((now - startDate) / (1000 * 60 * 60 * 24)).toInt()

    return when (frequency.uppercase()) {
      "WEEKLY" -> {
        val cycleDay = (diffDays % 7).coerceAtLeast(0)
        (7 - cycleDay).coerceAtLeast(1)
      }
      "BIWEEKLY" -> {
        val cycleDay = (diffDays % 14).coerceAtLeast(0)
        (14 - cycleDay).coerceAtLeast(1)
      }
      "SEMIMONTHLY" -> {
        val cycleDay = (diffDays % 15).coerceAtLeast(0)
        (15 - cycleDay).coerceAtLeast(1)
      }
      else -> { // MONTHLY
        val maxDays = nowCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)
        (maxDays - currentDay + 1).coerceAtLeast(1)
      }
    }
  }

  fun generateSampleLedgerJson(): String {
    val now = System.currentTimeMillis()
    val dayMillis = 24 * 60 * 60 * 1000L

    return """
    {
      "app": "Spent",
      "version": 1,
      "exportTimestamp": $now,
      "userAccount": {
        "id": "partner_shared_account",
        "displayName": "Alex (Partner)",
        "role": "Shared Partner",
        "lastDriveSyncTimestamp": $now
      },
      "preferences": {
        "currencySymbol": "$",
        "savingsGoalName": "Vacation Fund",
        "savingsGoalTotal": 2500.0,
        "savingsMonthlyContribution": 200.0
      },
      "payCycle": {
        "id": "partner_cycle",
        "frequency": "MONTHLY",
        "startDate": ${now - (12 * dayMillis)},
        "income": 3200.0
      },
      "categories": [
        { "id": "cat_food", "name": "Food & Groceries", "iconName": "Restaurant", "colorHex": "#10B981", "budgetAmount": 600.0, "displayOrder": 0 },
        { "id": "cat_housing", "name": "Rent & Housing", "iconName": "Home", "colorHex": "#3B82F6", "budgetAmount": 1100.0, "displayOrder": 1 },
        { "id": "cat_utilities", "name": "Utilities & Bills", "iconName": "Bolt", "colorHex": "#F59E0B", "budgetAmount": 250.0, "displayOrder": 2 },
        { "id": "cat_transport", "name": "Transport & Gas", "iconName": "DirectionsCar", "colorHex": "#8B5CF6", "budgetAmount": 200.0, "displayOrder": 3 },
        { "id": "cat_savings", "name": "Savings", "iconName": "Savings", "colorHex": "#059669", "budgetAmount": 500.0, "displayOrder": 4 }
      ],
      "transactions": [
        { "id": "tx_sample_1", "amount": 3200.0, "type": "INCOME", "categoryId": "cat_income", "timestamp": ${now - (12 * dayMillis)}, "note": "Monthly Salary" },
        { "id": "tx_sample_2", "amount": 1100.0, "type": "EXPENSE", "categoryId": "cat_housing", "timestamp": ${now - (10 * dayMillis)}, "note": "Apartment Rent" },
        { "id": "tx_sample_3", "amount": 142.50, "type": "EXPENSE", "categoryId": "cat_food", "timestamp": ${now - (3 * dayMillis)}, "note": "Weekly Supermarket" },
        { "id": "tx_sample_4", "amount": 65.0, "type": "EXPENSE", "categoryId": "cat_utilities", "timestamp": ${now - (2 * dayMillis)}, "note": "Internet & Electricity" },
        { "id": "tx_sample_5", "amount": 45.0, "type": "EXPENSE", "categoryId": "cat_transport", "timestamp": ${now - (1 * dayMillis)}, "note": "Gas refill" },
        { "id": "tx_sample_6", "amount": 200.0, "type": "EXPENSE", "categoryId": "cat_savings", "timestamp": ${now - (5 * dayMillis)}, "note": "Vacation Fund Deposit" }
      ],
      "recurringRules": [
        { "id": "rr_1", "amount": 1100.0, "categoryId": "cat_housing", "frequency": "MONTHLY", "startDate": $now, "note": "Rent" },
        { "id": "rr_2", "amount": 65.0, "categoryId": "cat_utilities", "frequency": "MONTHLY", "startDate": $now, "note": "Wifi Bill" }
      ]
    }
    """.trimIndent()
  }
}

object HouseholdAggregator {

  fun combine(
    localTransactions: List<TransactionEntity>,
    localCategories: List<CategoryEntity>,
    localPayCycle: PayCycleEntity?,
    currencySymbol: String,
    partnerLedger: SharedLedgerData?,
    myDisplayName: String = "You"
  ): HouseholdUnifiedData {
    // 1. Local User Calculations
    val categoryMap = localCategories.associateBy { it.id }
    var myIncomeSum = 0.0
    var mySpentSum = 0.0
    val myCatSpentMap = mutableMapOf<String, Double>()

    for (tx in localTransactions) {
      if (tx.type.equals("INCOME", ignoreCase = true)) {
        myIncomeSum += tx.amount
      } else {
        mySpentSum += tx.amount
        myCatSpentMap[tx.categoryId] = (myCatSpentMap[tx.categoryId] ?: 0.0) + tx.amount
      }
    }

    val myEffectiveIncome = if (localPayCycle != null && localPayCycle.income > 0) localPayCycle.income else myIncomeSum
    val now = System.currentTimeMillis()
    val frequency = localPayCycle?.frequency ?: "MONTHLY"
    val startDate = localPayCycle?.startDate ?: now
    val daysRemaining = calculateDaysRemaining(frequency, startDate, now)

    val myRemaining = (myEffectiveIncome - mySpentSum).coerceAtLeast(0.0)
    val mySafeToSpendToday = if (daysRemaining > 0) myRemaining / daysRemaining else myRemaining

    // 2. Partner Data
    val partnerIncome = partnerLedger?.totalIncome ?: 0.0
    val partnerSpent = partnerLedger?.totalSpent ?: 0.0
    val partnerSafeToSpend = partnerLedger?.safeToSpendToday ?: 0.0
    val partnerName = partnerLedger?.ownerName ?: "Partner"

    // 3. Combined Summary
    val combinedIncome = myEffectiveIncome + partnerIncome
    val combinedSpent = mySpentSum + partnerSpent
    val combinedNetBalance = combinedIncome - combinedSpent
    val combinedSafeToSpend = mySafeToSpendToday + partnerSafeToSpend

    val summary = HouseholdSummary(
      combinedIncome = combinedIncome,
      combinedSpent = combinedSpent,
      combinedNetBalance = combinedNetBalance,
      combinedSafeToSpendToday = combinedSafeToSpend,
      myTotalIncome = myEffectiveIncome,
      myTotalSpent = mySpentSum,
      mySafeToSpendToday = mySafeToSpendToday,
      partnerTotalIncome = partnerIncome,
      partnerTotalSpent = partnerSpent,
      partnerSafeToSpendToday = partnerSafeToSpend,
      daysRemainingInCycle = daysRemaining,
      currencySymbol = currencySymbol
    )

    // 4. Combined Categories
    val envelopeList = mutableListOf<HouseholdCategoryEnvelope>()
    val partnerCatMap = partnerLedger?.categories?.associateBy { it.name.trim().lowercase() } ?: emptyMap()
    val processedPartnerCats = mutableSetOf<String>()

    for (cat in localCategories) {
      val key = cat.name.trim().lowercase()
      val partnerCat = partnerCatMap[key]
      if (partnerCat != null) {
        processedPartnerCats.add(key)
      }

      val myCatSpent = myCatSpentMap[cat.id] ?: 0.0
      val partnerCatSpent = partnerCat?.spentAmount ?: 0.0
      val combinedCatSpent = myCatSpent + partnerCatSpent
      val totalBudget = cat.budgetAmount + (partnerCat?.budgetAmount ?: 0.0)
      val progress = if (totalBudget > 0) (combinedCatSpent / totalBudget).toFloat().coerceIn(0f, 1.5f) else 0f

      envelopeList.add(
        HouseholdCategoryEnvelope(
          name = cat.name,
          iconName = cat.iconName,
          colorHex = cat.colorHex,
          totalBudget = totalBudget,
          totalSpent = combinedCatSpent,
          mySpent = myCatSpent,
          partnerSpent = partnerCatSpent,
          progress = progress
        )
      )
    }

    // Add remaining partner categories that were not in local categories
    partnerLedger?.categories?.forEach { pCat ->
      val key = pCat.name.trim().lowercase()
      if (!processedPartnerCats.contains(key)) {
        val progress = if (pCat.budgetAmount > 0) (pCat.spentAmount / pCat.budgetAmount).toFloat().coerceIn(0f, 1.5f) else 0f
        envelopeList.add(
          HouseholdCategoryEnvelope(
            name = pCat.name,
            iconName = pCat.iconName,
            colorHex = pCat.colorHex,
            totalBudget = pCat.budgetAmount,
            totalSpent = pCat.spentAmount,
            mySpent = 0.0,
            partnerSpent = pCat.spentAmount,
            progress = progress
          )
        )
      }
    }

    // 5. Unified Transactions
    val feedList = mutableListOf<HouseholdTransactionItem>()

    // Local items
    localTransactions.take(30).forEach { tx ->
      val cat = categoryMap[tx.categoryId]
      val catName = cat?.name ?: "General"
      val catColor = cat?.colorHex ?: "#64748B"
      feedList.add(
        HouseholdTransactionItem(
          id = tx.id,
          amount = tx.amount,
          type = tx.type,
          categoryName = catName,
          categoryColorHex = catColor,
          timestamp = tx.timestamp,
          note = tx.note.ifBlank { catName },
          source = HouseholdSource.YOU,
          authorName = myDisplayName
        )
      )
    }

    // Partner items
    partnerLedger?.recentTransactions?.forEach { pTx ->
      feedList.add(
        HouseholdTransactionItem(
          id = pTx.id,
          amount = pTx.amount,
          type = pTx.type,
          categoryName = pTx.categoryName,
          categoryColorHex = pTx.categoryColorHex,
          timestamp = pTx.timestamp,
          note = pTx.note.ifBlank { pTx.categoryName },
          source = HouseholdSource.PARTNER,
          authorName = partnerName
        )
      )
    }

    feedList.sortByDescending { it.timestamp }

    return HouseholdUnifiedData(
      summary = summary,
      categories = envelopeList,
      transactions = feedList.take(40),
      partnerName = partnerName,
      partnerLastUpdated = partnerLedger?.exportTimestamp ?: 0L,
      isPartnerActive = partnerLedger != null
    )
  }

  private fun calculateDaysRemaining(frequency: String, startDate: Long, now: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = startDate }
    val nowCal = Calendar.getInstance().apply { timeInMillis = now }

    val diffDays = ((now - startDate) / (1000 * 60 * 60 * 24)).toInt()

    return when (frequency.uppercase()) {
      "WEEKLY" -> {
        val daysPassedInWeek = (diffDays % 7).coerceAtLeast(0)
        (7 - daysPassedInWeek).coerceAtLeast(1)
      }
      "BIWEEKLY" -> {
        val daysPassedInBiweek = (diffDays % 14).coerceAtLeast(0)
        (14 - daysPassedInBiweek).coerceAtLeast(1)
      }
      "SEMIMONTHLY" -> {
        val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)
        if (currentDay <= 15) {
          (15 - currentDay + 1).coerceAtLeast(1)
        } else {
          val maxDays = nowCal.getActualMaximum(Calendar.DAY_OF_MONTH)
          (maxDays - currentDay + 1).coerceAtLeast(1)
        }
      }
      else -> {
        val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)
        val maxDays = nowCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        (maxDays - currentDay + 1).coerceAtLeast(1)
      }
    }
  }
}

