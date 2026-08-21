package com.app.spent.data.sync

import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.TransactionEntity
import org.json.JSONObject

data class SharedMemberInfo(
  val fileId: String,
  val name: String,
  val role: String = "Member",
  val lastSyncTimestamp: Long = 0L,
  val isLocal: Boolean = false,
  val customNickname: String? = null
) {
  val effectiveName: String
    get() = customNickname?.takeIf { it.isNotBlank() } ?: name
}

data class SharedMemberContribution(
  val memberName: String,
  val spent: Double,
  val income: Double,
  val safeToSpend: Double
)

data class SharedUnifiedTransactionItem(
  val id: String,
  val amount: Double,
  val type: String, // INCOME, EXPENSE
  val categoryName: String,
  val categoryColorHex: String,
  val timestamp: Long,
  val note: String,
  val authorName: String,
  val isLocal: Boolean
)

data class SharedCategoryEnvelope(
  val name: String,
  val iconName: String,
  val colorHex: String,
  val totalBudget: Double,
  val totalSpent: Double,
  val mySpent: Double,
  val progress: Float,
  val memberBreakdown: Map<String, Double> = emptyMap()
)

data class SharedFinancesSummary(
  val combinedIncome: Double,
  val combinedSpent: Double,
  val combinedNetBalance: Double,
  val combinedSafeToSpendToday: Double,
  val myTotalIncome: Double,
  val myTotalSpent: Double,
  val mySafeToSpendToday: Double,
  val daysRemainingInCycle: Int,
  val currencySymbol: String,
  val memberContributions: List<SharedMemberContribution> = emptyList()
)

data class SharedUnifiedData(
  val summary: SharedFinancesSummary,
  val categories: List<SharedCategoryEnvelope>,
  val transactions: List<SharedUnifiedTransactionItem>,
  val members: List<SharedMemberInfo>
)

// Legacy aliases for backward compatibility if referenced
typealias HouseholdUnifiedData = SharedUnifiedData
typealias HouseholdSummary = SharedFinancesSummary
typealias HouseholdCategoryEnvelope = SharedCategoryEnvelope
typealias HouseholdTransactionItem = SharedUnifiedTransactionItem

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
      var ownerName = "Shared Member"
      var ownerRole = "Member"
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

  fun calculateDaysRemaining(frequency: String, startDate: Long, now: Long): Int {
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
        "id": "member_demo_account",
        "displayName": "Demo Member",
        "role": "Shared Member",
        "lastDriveSyncTimestamp": $now
      },
      "preferences": {
        "currencySymbol": "$",
        "savingsGoalName": "Group Trip",
        "savingsGoalTotal": 2000.0,
        "savingsMonthlyContribution": 150.0
      },
      "payCycle": {
        "id": "demo_cycle",
        "frequency": "MONTHLY",
        "startDate": ${now - (10 * dayMillis)},
        "income": 2800.0
      },
      "categories": [
        { "id": "cat_food", "name": "Food & Groceries", "iconName": "Restaurant", "colorHex": "#10B981", "budgetAmount": 550.0, "displayOrder": 0 },
        { "id": "cat_housing", "name": "Rent & Housing", "iconName": "Home", "colorHex": "#3B82F6", "budgetAmount": 950.0, "displayOrder": 1 },
        { "id": "cat_utilities", "name": "Utilities & Bills", "iconName": "Bolt", "colorHex": "#F59E0B", "budgetAmount": 200.0, "displayOrder": 2 },
        { "id": "cat_transport", "name": "Transport", "iconName": "DirectionsCar", "colorHex": "#8B5CF6", "budgetAmount": 180.0, "displayOrder": 3 },
        { "id": "cat_entertainment", "name": "Entertainment", "iconName": "Movie", "colorHex": "#EC4899", "budgetAmount": 150.0, "displayOrder": 4 }
      ],
      "transactions": [
        { "id": "tx_demo_1", "amount": 2800.0, "type": "INCOME", "categoryId": "cat_income", "timestamp": ${now - (10 * dayMillis)}, "note": "Monthly Income" },
        { "id": "tx_demo_2", "amount": 950.0, "type": "EXPENSE", "categoryId": "cat_housing", "timestamp": ${now - (8 * dayMillis)}, "note": "Shared Housing" },
        { "id": "tx_demo_3", "amount": 125.0, "type": "EXPENSE", "categoryId": "cat_food", "timestamp": ${now - (4 * dayMillis)}, "note": "Weekly Groceries" },
        { "id": "tx_demo_4", "amount": 60.0, "type": "EXPENSE", "categoryId": "cat_utilities", "timestamp": ${now - (2 * dayMillis)}, "note": "Electric Bill" },
        { "id": "tx_demo_5", "amount": 40.0, "type": "EXPENSE", "categoryId": "cat_transport", "timestamp": ${now - (1 * dayMillis)}, "note": "Metro pass" }
      ],
      "recurringRules": [
        { "id": "rr_demo_1", "amount": 950.0, "categoryId": "cat_housing", "frequency": "MONTHLY", "startDate": $now, "note": "Housing" }
      ]
    }
    """.trimIndent()
  }
}

object SharedFinancesAggregator {

  fun combine(
    localTransactions: List<TransactionEntity>,
    localCategories: List<CategoryEntity>,
    localPayCycle: PayCycleEntity?,
    currencySymbol: String,
    memberLedgers: List<SharedLedgerData> = emptyList(),
    myDisplayName: String = "You",
    knownMembers: List<SharedMemberInfo> = emptyList()
  ): SharedUnifiedData {
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
    val daysRemaining = SharedLedgerParser.calculateDaysRemaining(frequency, startDate, now)

    val myRemaining = (myEffectiveIncome - mySpentSum).coerceAtLeast(0.0)
    val mySafeToSpendToday = if (daysRemaining > 0) myRemaining / daysRemaining else myRemaining

    // 2. Members Calculations
    var membersIncomeSum = 0.0
    var membersSpentSum = 0.0
    var membersSafeToSpendSum = 0.0
    val memberContributions = mutableListOf<SharedMemberContribution>()
    val membersInfoList = mutableListOf<SharedMemberInfo>()

    // Add local user as first member
    membersInfoList.add(
      SharedMemberInfo(
        fileId = "local_user",
        name = myDisplayName,
        role = "Owner",
        lastSyncTimestamp = now,
        isLocal = true
      )
    )
    memberContributions.add(
      SharedMemberContribution(
        memberName = myDisplayName,
        spent = mySpentSum,
        income = myEffectiveIncome,
        safeToSpend = mySafeToSpendToday
      )
    )

    for (ledger in memberLedgers) {
      val matchingKnown = knownMembers.find { it.fileId == ledger.sourceFileId }
      val memberDisplayName = matchingKnown?.effectiveName ?: ledger.ownerName

      membersIncomeSum += ledger.totalIncome
      membersSpentSum += ledger.totalSpent
      membersSafeToSpendSum += ledger.safeToSpendToday

      memberContributions.add(
        SharedMemberContribution(
          memberName = memberDisplayName,
          spent = ledger.totalSpent,
          income = ledger.totalIncome,
          safeToSpend = ledger.safeToSpendToday
        )
      )

      membersInfoList.add(
        matchingKnown?.copy(
          lastSyncTimestamp = ledger.exportTimestamp
        ) ?: SharedMemberInfo(
          fileId = ledger.sourceFileId ?: ledger.ownerName,
          name = ledger.ownerName,
          role = ledger.ownerRole,
          lastSyncTimestamp = ledger.exportTimestamp,
          isLocal = false
        )
      )
    }

    // 3. Combined Summary
    val combinedIncome = myEffectiveIncome + membersIncomeSum
    val combinedSpent = mySpentSum + membersSpentSum
    val combinedNetBalance = combinedIncome - combinedSpent
    val combinedSafeToSpend = mySafeToSpendToday + membersSafeToSpendSum

    val summary = SharedFinancesSummary(
      combinedIncome = combinedIncome,
      combinedSpent = combinedSpent,
      combinedNetBalance = combinedNetBalance,
      combinedSafeToSpendToday = combinedSafeToSpend,
      myTotalIncome = myEffectiveIncome,
      myTotalSpent = mySpentSum,
      mySafeToSpendToday = mySafeToSpendToday,
      daysRemainingInCycle = daysRemaining,
      currencySymbol = currencySymbol,
      memberContributions = memberContributions
    )

    // 4. Combined Category Envelopes
    val envelopeList = mutableListOf<SharedCategoryEnvelope>()
    val processedRemoteCats = mutableSetOf<String>()

    for (cat in localCategories) {
      val key = cat.name.trim().lowercase()
      var totalBudget = cat.budgetAmount
      var combinedCatSpent = myCatSpentMap[cat.id] ?: 0.0
      val breakdown = mutableMapOf<String, Double>()
      if ((myCatSpentMap[cat.id] ?: 0.0) > 0) {
        breakdown[myDisplayName] = myCatSpentMap[cat.id] ?: 0.0
      }

      for (ledger in memberLedgers) {
        val matchingKnown = knownMembers.find { it.fileId == ledger.sourceFileId }
        val memberDisplayName = matchingKnown?.effectiveName ?: ledger.ownerName
        val matchCat = ledger.categories.find { it.name.trim().lowercase() == key }
        if (matchCat != null) {
          processedRemoteCats.add(key)
          totalBudget += matchCat.budgetAmount
          combinedCatSpent += matchCat.spentAmount
          if (matchCat.spentAmount > 0) {
            breakdown[memberDisplayName] = matchCat.spentAmount
          }
        }
      }

      val progress = if (totalBudget > 0) (combinedCatSpent / totalBudget).toFloat().coerceIn(0f, 1.5f) else 0f

      envelopeList.add(
        SharedCategoryEnvelope(
          name = cat.name,
          iconName = cat.iconName,
          colorHex = cat.colorHex,
          totalBudget = totalBudget,
          totalSpent = combinedCatSpent,
          mySpent = myCatSpentMap[cat.id] ?: 0.0,
          progress = progress,
          memberBreakdown = breakdown
        )
      )
    }

    // Add remaining categories present only in remote members
    for (ledger in memberLedgers) {
      val matchingKnown = knownMembers.find { it.fileId == ledger.sourceFileId }
      val memberDisplayName = matchingKnown?.effectiveName ?: ledger.ownerName
      for (rCat in ledger.categories) {
        val key = rCat.name.trim().lowercase()
        if (!processedRemoteCats.contains(key) && localCategories.none { it.name.trim().lowercase() == key }) {
          processedRemoteCats.add(key)
          val breakdown = mutableMapOf(memberDisplayName to rCat.spentAmount)
          val progress = if (rCat.budgetAmount > 0) (rCat.spentAmount / rCat.budgetAmount).toFloat().coerceIn(0f, 1.5f) else 0f
          envelopeList.add(
            SharedCategoryEnvelope(
              name = rCat.name,
              iconName = rCat.iconName,
              colorHex = rCat.colorHex,
              totalBudget = rCat.budgetAmount,
              totalSpent = rCat.spentAmount,
              mySpent = 0.0,
              progress = progress,
              memberBreakdown = breakdown
            )
          )
        }
      }
    }

    // 5. Unified Transactions
    val feedList = mutableListOf<SharedUnifiedTransactionItem>()

    // Local items
    localTransactions.take(30).forEach { tx ->
      val cat = categoryMap[tx.categoryId]
      val catName = cat?.name ?: "General"
      val catColor = cat?.colorHex ?: "#64748B"
      feedList.add(
        SharedUnifiedTransactionItem(
          id = tx.id,
          amount = tx.amount,
          type = tx.type,
          categoryName = catName,
          categoryColorHex = catColor,
          timestamp = tx.timestamp,
          note = tx.note.ifBlank { catName },
          authorName = myDisplayName,
          isLocal = true
        )
      )
    }

    // Remote member items
    for (ledger in memberLedgers) {
      val matchingKnown = knownMembers.find { it.fileId == ledger.sourceFileId }
      val memberDisplayName = matchingKnown?.effectiveName ?: ledger.ownerName
      ledger.recentTransactions.forEach { rTx ->
        feedList.add(
          SharedUnifiedTransactionItem(
            id = rTx.id,
            amount = rTx.amount,
            type = rTx.type,
            categoryName = rTx.categoryName,
            categoryColorHex = rTx.categoryColorHex,
            timestamp = rTx.timestamp,
            note = rTx.note.ifBlank { rTx.categoryName },
            authorName = memberDisplayName,
            isLocal = false
          )
        )
      }
    }

    feedList.sortByDescending { it.timestamp }

    return SharedUnifiedData(
      summary = summary,
      categories = envelopeList,
      transactions = feedList.take(50),
      members = membersInfoList
    )
  }
}

// Backward compatible object alias
object HouseholdAggregator {
  fun combine(
    localTransactions: List<TransactionEntity>,
    localCategories: List<CategoryEntity>,
    localPayCycle: PayCycleEntity?,
    currencySymbol: String,
    partnerLedger: SharedLedgerData?,
    myDisplayName: String = "You"
  ): SharedUnifiedData {
    return SharedFinancesAggregator.combine(
      localTransactions = localTransactions,
      localCategories = localCategories,
      localPayCycle = localPayCycle,
      currencySymbol = currencySymbol,
      memberLedgers = if (partnerLedger != null) listOf(partnerLedger) else emptyList(),
      myDisplayName = myDisplayName
    )
  }
}
