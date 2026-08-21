
## 📋 Recommended Action Plan

| Priority | Task | Target Component |
|---|---|---|
| **P0** | Fix package folder path mismatch (`com.example.spent` ➔ `com.app.spent`) | Folder tree |
| **P0** | Remove `Context` from ViewModels & Intent classes to eliminate memory leaks | `SharedLedgerViewModel`, `AddTransactionViewModel` |
| **P1** | Add Hilt / Koin Dependency Injection to eliminate `ViewModelProvider.Factory` boilerplate | `NavGraph.kt`, `SpentApplication.kt` |
| **P1** | Move heavy database aggregations (sums, cycle totals) from in-memory Kotlin to Room SQL queries | `SpentDao.kt`, `DashboardViewModel.kt` |
| **P2** | Switch financial amounts from `Double` to integer cents (`Long`) | Room Entities |
| **P2** | Migrate from `Calendar` and fixed 30-day math to `java.time.LocalDate` / `java.time.Period` | `SpentRepositoryImpl.kt`, `DashboardViewModel.kt` |
| **P2** | Change R8 / Minification to true for .abb generation
