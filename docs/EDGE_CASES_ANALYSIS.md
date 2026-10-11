# Action Plan: Critical Edge Cases & Stability Fixes

This document outlines the high-impact fixes needed across the app. For each item, it explains **what will be done** and **why it is needed** in plain, simple terms.

---

### 1. Google Drive OAuth Token Expiration
- **What will be done**:
  Detect when Google Drive credentials expire or fail in the background, update the UI status to "Action Required", and let the user re-link with a single tap.
- **Why**:
  Google login tokens expire. Currently, when a token expires, the background auto-sync fails silently. The user believes their data is continuously backed up to the cloud, but in reality, no backups are being made.

---

### 2. Assets & Currencies in Cloud Backup and Reset
- **What will be done**:
  1. Add asset holdings (foreign currencies, stocks, crypto) to the Google Drive backup JSON and the restore process.
  2. Clear asset holdings when the user taps "Reset All Data" in Settings.
- **Why**:
  Currently, backups only save transactions and categories. If a user restores their backup on a new phone, all foreign currency cash balances and investments are lost. Additionally, resetting the app currently leaves old asset holdings orphaned in the database.

---

### 3. Safe Category Deletion (Prevent Transaction Wipes)
- **What will be done**:
  Remove `CASCADE` delete from categories in the database. When a category is deleted, automatically reassign its existing transactions to "General".
- **Why**:
  Currently, if a user deletes an old category in Settings (e.g. "Work Project"), the database silently deletes every transaction ever logged under that category, causing accidental data loss across past months or years.

---

### 4. Market Price Crash Protection (Division by Zero)
- **What will be done**:
  Add safety checks so price calculations never divide by zero, even if an asset crashes by -100%.
- **Why**:
  In formulas calculating daily price changes, a -100% drop causes a division by zero. This produces `NaN` ("Not a Number"), which crashes the user interface formatting (`String.format("%.2f")`) and breaks the dashboard cards.

---

### 5. Recurring Rules & Pay Cycle Safeguards
- **What will be done**:
  1. Save pending recurring transactions in a single batch rather than one-by-one in a loop.
  2. Ensure the "Safe to Spend Today" formula never divides by zero if a pay cycle expired at midnight.
- **Why**:
  If the user hasn't opened the app in a few months, inserting dozens of recurring transactions individually freezes the app on startup. Similarly, when a cycle expires, dividing by zero causes the dashboard to display `Infinity` instead of a real monetary amount.

---

### 6. Local Receipt Images on Restored Devices
- **What will be done**:
  Check if a receipt image file actually exists on the device before trying to load it. If missing, show a clean "Photo saved on original device" indicator instead of a broken error view.
- **Why**:
  Local receipt photos are saved only inside the original phone's storage. A cloud backup only stores the file path text. When restored on a new device, the image file is not there, causing broken image placeholders and image loading errors.
