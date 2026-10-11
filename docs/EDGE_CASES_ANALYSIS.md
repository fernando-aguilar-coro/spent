# Deep-Dive Edge Cases Analysis: Data Integrity & Synchronization

This document provides a comprehensive technical breakdown of potential failure modes and edge cases discovered across the Spent codebase, with specific focus on user data integrity, background synchronization, and math calculations.

---

## 1. Google Drive Sync & OAuth Token Expiration

### A. OAuth Token Lifecycle & Silent Auto-Sync Failure (High Probability)
- **Relevant Files**:
  - `GoogleDriveRestService.kt` (lines 45-60)
  - `DriveSyncManager.kt` (lines 230-270)
- **The Problem**:
  In `GoogleDriveRestService.kt`, the Google Drive API client is initialized using:
  ```kotlin
  val credential = GoogleAccountCredential.usingOAuth2(context.applicationContext, listOf(DriveScopes.DRIVE_FILE, ...))
  credential.selectedAccount = account.account ?: ...
  ```
  `GoogleSignIn.getLastSignedInAccount(context)` only returns the cached `GoogleSignInAccount` object on disk. However, Google OAuth access tokens expire after **60 minutes**. When `execute()` is called on a Google Drive REST request in the background:
  1. The Google Play Services library attempts to refresh the token via refresh token.
  2. If the user changed their Google account password, revoked permissions in their Google account settings, or if Google invalidates the token family, Google Play Services throws a `UserRecoverableAuthIOException` or `GoogleAuthException`.
  3. In `DriveSyncManager.kt`, `triggerAutoSync()` wraps this with a generic `catch (e: Exception)` that only prints a log and sets `_isSyncing.value = false`.
- **Impact**:
  The user assumes their data is continuously backing up in the background. In reality, background auto-sync permanently and silently stops working. The user never receives a notification or prompt to re-authenticate until they manually inspect the Settings screen.
- **Recommended Fix**:
  Detect `UserRecoverableAuthException` or HTTP `401 Unauthorized`. When encountered, flag `isDriveConnected = false` or emit a UI notification/intent containing `intent.getIntent()` so the user can re-grant permissions with a single tap.

### B. Multi-Device Overwrites & Zombie Merges (Context / Low Probability for Single-Device)
- In a single-device scenario, this is low risk. However, if the user connects a second device (e.g. tablet), `syncNow()` and `triggerAutoSync()` currently perform a blind one-way upload without downloading first, and `mergeCloudAndLocal()` lacks deletion tombstones (causing deleted records to reappear if merged).

---

## 2. Backup, Restore & "Reset All Data" Omissions

### A. Variable Assets & Multi-Currency Holdings Erased on Cloud Restore
- **Relevant Files**:
  - `DriveBackupManager.kt` (lines 30-100)
  - `SpentRepositoryImpl.kt` (lines 490-510)
- **The Problem**:
  `DriveBackupManager.generateBackupJson()` serializes:
  - `categories`
  - `transactions`
  - `loans`
  - `payCycle`
  - `recurringRules`
  - `userAccount`
  - `preferences`
  
  It completely omits `AssetHoldingEntity` (`asset_holdings` table).
  Similarly, `DriveBackupManager.restoreFromJson()` calls `repository.restoreAllData(...)`, which replaces categories, transactions, and loans, but does not parse or restore assets.
- **Impact**:
  If a user has foreign currency cash (e.g. 1,000 EUR, 50,000 JPY), stocks, or crypto, backs up their data to Google Drive, and restores onto a new device, **all their asset holdings vanish completely**.
- **Recommended Fix**:
  Add an `"assetHoldings"` array to `generateBackupJson()` and update `restoreFromJson()` and `restoreAllData()` to serialize and restore `AssetHoldingEntity` records.

### B. Orphaned Holdings on "Reset All Data"
- **Relevant Files**:
  - `SpentRepositoryImpl.kt` (lines 512-527)
- **The Problem**:
  When a user invokes "Reset All Data" in Settings:
  ```kotlin
  override suspend fun resetAllData(deleteDriveImages: Boolean) {
      dao.deleteAllTransactions()
      dao.deleteAllCategories()
      dao.deleteAllRecurringRules()
      dao.deleteAllPayCycles()
      dao.deleteAllLoans()
      ...
  }
  ```
  `assetDao.deleteHoldingById` or `deleteAllHoldings()` is never called.
- **Impact**:
  The user expects a clean slate, but their portfolio holdings and foreign currencies remain alive in the database and appear in their Dashboard Net Balance.
- **Recommended Fix**:
  Add `@Query("DELETE FROM asset_holdings")` and `@Query("DELETE FROM market_prices")` in `AssetDao.kt` and invoke them in `resetAllData()`.

---

## 3. Database Relationships & Cascading Deletions

### A. Accidental Mass-Deletion of Transactions via Room `CASCADE`
- **Relevant Files**:
  - `TransactionEntity.kt` (lines 11-18)
- **The Problem**:
  `TransactionEntity.kt` defines:
  ```kotlin
  foreignKeys = [
      ForeignKey(
          entity = CategoryEntity::class,
          parentColumns = ["id"],
          childColumns = ["categoryId"],
          onDelete = ForeignKey.CASCADE
      )
  ]
  ```
  `onDelete = ForeignKey.CASCADE` instructs SQLite to automatically purge all child records whenever the parent record is deleted.
- **Impact**:
  Suppose a user creates a category called "Home Renovation", logs 60 transactions over 4 months, and later decides to clean up their category list by deleting "Home Renovation".
  SQLite **silently and permanently destroys all 60 transactions** from history without warning. The user's historical spend analysis and account balance are permanently altered.
- **Recommended Fix**:
  Change `onDelete` to `ForeignKey.SET_DEFAULT` or `ForeignKey.NO_ACTION`, or in the repository before deleting a category, reassign all associated transactions to the default "General" category (`cat_general`):
  ```kotlin
  @Query("UPDATE transactions SET categoryId = 'cat_general' WHERE categoryId = :deletedCategoryId")
  suspend fun reassignTransactionsToGeneral(deletedCategoryId: String)
  ```

---

## 4. Mathematical Edge Cases & Market Pricing

### A. Division by Zero on 100% Market Drops (`NaN` Corrupting Compose)
- **Relevant Files**:
  - `AssetRepository.kt` (lines 79-85)
- **The Problem**:
  In `AssetRepositoryImpl`:
  ```kotlin
  val totalDayChangeAmount = valuedAssets.sumOf { asset ->
      val prevPrice = asset.effectiveUnitPrice / (1.0 + (asset.change24hPct / 100.0))
      val prevVal = asset.holding.quantity * prevPrice
      asset.totalValuation - prevVal
  }
  ```
  If a cryptocurrency or stock experiences a `-100.0%` decline:
  $$1.0 + \left(\frac{-100.0}{100.0}\right) = 1.0 - 1.0 = 0.0$$
  In Kotlin, `Double / 0.0` produces `Double.POSITIVE_INFINITY` or `Double.NaN`.
- **Impact**:
  When `NaN` enters `totalDayChangeAmount` and `totalDayChangePct`, it propagates to `PortfolioSummary` and `DashboardUiState`.
  Any `String.format("%.2f", amount)` or Compose UI drawing text with `NaN` will render `"NaN"` or cause `IllegalFormatException`.
- **Recommended Fix**:
  Guard the denominator:
  ```kotlin
  val denominator = (1.0 + (asset.change24hPct / 100.0)).coerceAtLeast(0.0001)
  val prevPrice = asset.effectiveUnitPrice / denominator
  ```

### B. Currency Symbol Change without Normalizing Data
- **Relevant Files**:
  - `UserPreferencesRepository.kt` (lines 140-150)
- **The Problem**:
  `setCurrencySymbol(symbol: String)` only changes the visual prefix in DataStore preferences. If the user had recorded $10,000 USD in expenses and switches their symbol to `€`, all ledger entries simply display `€10,000` without any historical conversion factor.

---

## 5. Recurring Rules, Pay Cycles & Concurrency

### A. Inactivity Avalanche on App Launch
- **Relevant Files**:
  - `SpentRepositoryImpl.kt` (lines 380-428)
- **The Problem**:
  In `executePendingRecurringRules()`, the app iterates over all active recurring rules and generates occurrences from `rule.lastExecuted` up to `today`.
  If a user has daily or weekly recurring rules and does not open the app for 6–12 months, `calculatePendingDueDates()` can return hundreds of due dates.
  The current code inserts them in a sequential `for` loop:
  ```kotlin
  for (occurrence in pendingOccurrences) {
      dao.insertTransaction(...)
  }
  ```
- **Impact**:
  Each single insertion triggers Room's SQLite table observer, causing reactive Flow recalculations on `DashboardViewModel`. This can freeze the main thread on cold start and trigger an Android ANR (Application Not Responding).
- **Recommended Fix**:
  Collect all occurrences and perform a single batch insertion:
  ```kotlin
  dao.insertTransactions(newTransactionsList)
  ```

### B. Division by Zero in Pay Cycle Safe-to-Spend
- **Relevant Files**:
  - `DashboardViewModel.kt` (lines 137-154)
- **The Problem**:
  ```kotlin
  val remainingDiscretionary = cycleTotalIncome - pendingFixedBills - monthlySavings - spentInCycle
  (remainingDiscretionary.coerceAtLeast(0.0)) / (daysRemaining + 1)
  ```
  If `daysRemaining == -1` (e.g. pay cycle expired at midnight and has not rolled over yet), `daysRemaining + 1 == 0`.
- **Impact**:
  `0.0 / 0` yields `Double.NaN`, while `positive / 0` yields `Double.POSITIVE_INFINITY`. The "Safe to spend today" card displays `Infinity` or `$NaN`.
- **Recommended Fix**:
  Ensure divisor is at least 1:
  ```kotlin
  val divisor = (daysRemaining + 1).coerceAtLeast(1)
  ```

---

## 6. Receipt Image Storage Decoupling

### A. Absolute Path Serialization Leading to Broken Images on New Devices
- **Relevant Files**:
  - `ImageStorageHelper.kt` (lines 80-103)
  - `DriveBackupManager.kt` (line 66)
- **The Problem**:
  When a user saves receipt images locally (`IN_APP` or `DEVICE` mode), `ImageStorageHelper.kt` stores an absolute URI in the database:
  `file:///data/user/0/com.app.spent/files/receipt_images/spent_receipt_xxxx.jpg`
  During Google Drive backup, `DriveBackupManager` exports `imageUri` as a plain string inside the JSON.
  However, the actual `.jpg` binary file is never uploaded to Google Drive in local storage mode.
- **Impact**:
  When restoring this backup on a new device or after an app reinstall, the database points to a sandbox path that does not exist on the new file system. Every receipt attachment card fails to load, showing broken placeholders.
- **Recommended Fix**:
  Either:
  1. If Google Drive sync is active, require receipts to sync to the user's hidden Drive AppData folder; or
  2. In local mode, gracefully handle missing local files with a distinct "Receipt only available on original device" badge instead of a generic crash or empty error.
