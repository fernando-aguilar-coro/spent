# Estructura del Proyecto Spent (Optimizada para Desarrollo / IA)
```text
Spent/
|-- .github/
|   +-- workflows/
|       +-- build.yml
|-- app/
|   |-- src/
|   |   |-- androidTest/
|   |   |   +-- java/com/example/spent/
|   |   |       +-- ExampleInstrumentedTest.kt
|   |   |-- main/
|   |   |   |-- java/com/example/spent/
|   |   |   |   |-- data/
|   |   |   |   |   |-- local/
|   |   |   |   |   |   |-- dao/
|   |   |   |   |   |   |   +-- SpentDao.kt
|   |   |   |   |   |   |-- db/
|   |   |   |   |   |   |   +-- SpentDatabase.kt
|   |   |   |   |   |   +-- entity/
|   |   |   |   |   |       |-- CategoryEntity.kt
|   |   |   |   |   |       |-- FamilyMemberEntity.kt
|   |   |   |   |   |       |-- ParentalControlConfigEntity.kt
|   |   |   |   |   |       |-- PayCycleEntity.kt
|   |   |   |   |   |       |-- RecurringRuleEntity.kt
|   |   |   |   |   |       |-- TransactionEntity.kt
|   |   |   |   |   |       +-- UserAccountEntity.kt
|   |   |   |   |   |-- preferences/
|   |   |   |   |   |   +-- UserPreferencesRepository.kt
|   |   |   |   |   |-- repository/
|   |   |   |   |   |   |-- SpentRepository.kt
|   |   |   |   |   |   +-- SpentRepositoryImpl.kt
|   |   |   |   |   +-- sync/
|   |   |   |   |       |-- DriveBackupManager.kt
|   |   |   |   |       |-- DriveSyncManager.kt
|   |   |   |   |       |-- GoogleDriveRestService.kt
|   |   |   |   |       |-- SharedFinancesAggregator.kt
|   |   |   |   |       |-- SharedLedgerModels.kt
|   |   |   |   |       +-- SharedLedgerParser.kt
|   |   |   |   |-- ui/
|   |   |   |   |   |-- analytics/
|   |   |   |   |   |   |-- components/
|   |   |   |   |   |   |   |-- CategoryDistributionItem.kt
|   |   |   |   |   |   |   |-- IncomeExpenseChart.kt
|   |   |   |   |   |   |   +-- SavingsSummaryCard.kt
|   |   |   |   |   |   |-- AnalyticsContract.kt
|   |   |   |   |   |   |-- AnalyticsScreen.kt
|   |   |   |   |   |   +-- AnalyticsViewModel.kt
|   |   |   |   |   |-- components/
|   |   |   |   |   |   |-- CategoryIconHelper.kt
|   |   |   |   |   |   |-- CustomNumericKeypad.kt
|   |   |   |   |   |   +-- WalkthroughBanner.kt
|   |   |   |   |   |-- dashboard/
|   |   |   |   |   |   |-- components/
|   |   |   |   |   |   |   |-- dialogs/
|   |   |   |   |   |   |   |   |-- DeleteTransactionDialog.kt
|   |   |   |   |   |   |   |   +-- TransactionDetailsDialog.kt
|   |   |   |   |   |   |   |-- CategoryEnvelopeRow.kt
|   |   |   |   |   |   |   |-- DashboardHeaderCard.kt
|   |   |   |   |   |   |   |-- DashboardProfileHeader.kt
|   |   |   |   |   |   |   |-- DashboardQuickActions.kt
|   |   |   |   |   |   |   |-- DashboardQuickTools.kt
|   |   |   |   |   |   |   +-- TransactionItemRow.kt
|   |   |   |   |   |   |-- DashboardContract.kt
|   |   |   |   |   |   |-- DashboardScreen.kt
|   |   |   |   |   |   +-- DashboardViewModel.kt
|   |   |   |   |   |-- fixedbills/
|   |   |   |   |   |   |-- components/
|   |   |   |   |   |   |   |-- AddFixedBillFormCard.kt
|   |   |   |   |   |   |   |-- FixedBillItemCard.kt
|   |   |   |   |   |   |   +-- FixedBillsEmptyState.kt
|   |   |   |   |   |   |-- FixedBillsContract.kt
|   |   |   |   |   |   |-- FixedBillsScreen.kt
|   |   |   |   |   |   +-- FixedBillsViewModel.kt
|   |   |   |   |   |-- loanstracker/
|   |   |   |   |   |   |-- components/
|   |   |   |   |   |   |   |-- AddLoanOrPaymentFormCard.kt
|   |   |   |   |   |   |   |-- LoanTransactionItemRow.kt
|   |   |   |   |   |   |   |-- LoansEmptyState.kt
|   |   |   |   |   |   |   +-- LoansSummaryHeroCard.kt
|   |   |   |   |   |   |-- LoansTrackerContract.kt
|   |   |   |   |   |   |-- LoansTrackerScreen.kt
|   |   |   |   |   |   +-- LoansTrackerViewModel.kt
|   |   |   |   |   |-- mvi/
|   |   |   |   |   |   +-- MviContract.kt
|   |   |   |   |   |-- navigation/
|   |   |   |   |   |   |-- BottomNavBar.kt
|   |   |   |   |   |   |-- NavGraph.kt
|   |   |   |   |   |   +-- Screen.kt
|   |   |   |   |   |-- onboarding/
|   |   |   |   |   |   |-- components/
|   |   |   |   |   |   |   |-- PayScheduleStep.kt
|   |   |   |   |   |   |   |-- ProfileSelectionStep.kt
|   |   |   |   |   |   |   +-- WelcomeStep.kt
|   |   |   |   |   |   |-- OnboardingContract.kt
|   |   |   |   |   |   |-- OnboardingScreen.kt
|   |   |   |   |   |   +-- OnboardingViewModel.kt
|   |   |   |   |   |-- savings/
|   |   |   |   |   |   |-- components/
|   |   |   |   |   |   |   |-- SavingsActiveGoalHeroCard.kt
|   |   |   |   |   |   |   |-- SavingsDepositHistoryItem.kt
|   |   |   |   |   |   |   |-- SavingsDepositSection.kt
|   |   |   |   |   |   |   |-- SavingsEmptyHistoryPlaceholder.kt
|   |   |   |   |   |   |   +-- SavingsGoalFormCard.kt
|   |   |   |   |   |   |-- SavingsContract.kt
|   |   |   |   |   |   |-- SavingsScreen.kt
|   |   |   |   |   |   +-- SavingsViewModel.kt
|   |   |   |   |   |-- settings/
|   |   |   |   |   |   |-- components/
|   |   |   |   |   |   |   |-- AppInfoCard.kt
|   |   |   |   |   |   |   |-- CurrencySelectionCard.kt
|   |   |   |   |   |   |   |-- ExportCsvCard.kt
|   |   |   |   |   |   |   |-- GoogleDriveSyncCard.kt
|   |   |   |   |   |   |   |-- ImageStorageLocationCard.kt
|   |   |   |   |   |   |   |-- LanguageSelectionCard.kt
|   |   |   |   |   |   |   |-- PayCycleCard.kt
|   |   |   |   |   |   |   |-- ResetDataButton.kt
|   |   |   |   |   |   |   +-- ThemeSelectionCard.kt
|   |   |   |   |   |   |-- SettingsContract.kt
|   |   |   |   |   |   |-- SettingsScreen.kt
|   |   |   |   |   |   +-- SettingsViewModel.kt
|   |   |   |   |   |-- sharedledger/
|   |   |   |   |   |   |-- components/
|   |   |   |   |   |   |   |-- dialogs/
|   |   |   |   |   |   |   |   |-- AddMemberDialog.kt
|   |   |   |   |   |   |   |   |-- EditMemberNameDialog.kt
|   |   |   |   |   |   |   |   +-- SharedFinancesGuideDialog.kt
|   |   |   |   |   |   |   |-- DriveStatusBar.kt
|   |   |   |   |   |   |   |-- InviteAndJoinSection.kt
|   |   |   |   |   |   |   |-- MembersPanelSection.kt
|   |   |   |   |   |   |   |-- SharedCategoryEnvelopeRow.kt
|   |   |   |   |   |   |   |-- SharedFinancesTabRow.kt
|   |   |   |   |   |   |   |-- SharedTransactionRow.kt
|   |   |   |   |   |   |   |-- StatisticsHeroCard.kt
|   |   |   |   |   |   |   +-- StatisticsKpiRow.kt
|   |   |   |   |   |   |-- SharedLedgerContract.kt
|   |   |   |   |   |   |-- SharedLedgerScreen.kt
|   |   |   |   |   |   +-- SharedLedgerViewModel.kt
|   |   |   |   |   |-- theme/
|   |   |   |   |   |   |-- Color.kt
|   |   |   |   |   |   |-- Theme.kt
|   |   |   |   |   |   +-- Type.kt
|   |   |   |   |   +-- transaction/
|   |   |   |   |       |-- components/
|   |   |   |   |       |   |-- AddCategoryDialog.kt
|   |   |   |   |       |   |-- CategoryEnvelopeSelector.kt
|   |   |   |   |       |   |-- DateTimePickerField.kt
|   |   |   |   |       |   |-- RecurringOptionsSection.kt
|   |   |   |   |       |   |-- TransactionImageAttachmentSection.kt
|   |   |   |   |       |   +-- TransactionTypeSelector.kt
|   |   |   |   |       |-- AddTransactionContract.kt
|   |   |   |   |       |-- AddTransactionScreen.kt
|   |   |   |   |       +-- AddTransactionViewModel.kt
|   |   |   |   |-- util/
|   |   |   |   |   |-- DataExportHelper.kt
|   |   |   |   |   |-- ImageCompressor.kt
|   |   |   |   |   |-- ImageStorageHelper.kt
|   |   |   |   |   |-- ImageUriResolver.kt
|   |   |   |   |   +-- LocaleHelper.kt
|   |   |   |   |-- worker/
|   |   |   |   |   +-- RecurringTransactionWorker.kt
|   |   |   |   |-- MainActivity.kt
|   |   |   |   +-- SpentApplication.kt
|   |   |   |-- res/
|   |   |   |   |-- drawable/
|   |   |   |   |   |-- ic_launcher_background.xml
|   |   |   |   |   +-- ic_launcher_foreground.xml
|   |   |   |   |-- mipmap-anydpi-v26/
|   |   |   |   |   |-- ic_launcher.xml
|   |   |   |   |   +-- ic_launcher_round.xml
|   |   |   |   |-- values/
|   |   |   |   |   |-- colors.xml
|   |   |   |   |   |-- strings.xml
|   |   |   |   |   +-- themes.xml
|   |   |   |   |-- values-es/
|   |   |   |   |   +-- strings.xml
|   |   |   |   |-- values-pt/
|   |   |   |   |   +-- strings.xml
|   |   |   |   +-- xml/
|   |   |   |       |-- backup_rules.xml
|   |   |   |       |-- data_extraction_rules.xml
|   |   |   |       +-- file_paths.xml
|   |   |   +-- AndroidManifest.xml
|   |   +-- test/
|   |       +-- java/com/example/spent/
|   |           +-- ExampleUnitTest.kt
|   |-- build.gradle.kts
|   +-- proguard-rules.pro
|-- gradle/
|   +-- libs.versions.toml
|-- build.gradle.kts
|-- design_spec.md
|-- gradle.properties
|-- README.md
+-- settings.gradle.kts
```

---

### Elementos Omitidos y Justificación

1. **Configuraciones de IDE (`.idea/`)**:
   - Contienen ajustes de inspección, layouts de ventanas y proyectos locales de Android Studio (`compiler.xml`, `misc.xml`, `runConfigurations.xml`). No aportan contexto lógico ni arquitectónico para una IA.
2. **Archivos Binarios / Compilados**:
   - `app/release/app-release.aab` y `release-keystore.jks`: Binarios opacos que no son legibles ni editables como texto.
   - `gradle/wrapper/gradle-wrapper.jar`: Binario de Gradle.
3. **Logs temporales (`.kotlin/errors/`)**:
   - Reportes de fallos temporales del compilador.
4. **Recursos de imagen rasterizada repetitivos (`mipmap-*/*.webp`)**:
   - Variantes por densidad de resolución de iconos del lanzador. Se conservan únicamente las definiciones vectoriales/XML en `mipmap-anydpi-v26/` y `drawable/`.
5. **Scripts ejecutables de wrapper (`gradlew`, `gradlew.bat`) y archivos `.gitignore` secundarios**:
   - Scripts genéricos de Gradle que no contienen lógica propia del proyecto.
