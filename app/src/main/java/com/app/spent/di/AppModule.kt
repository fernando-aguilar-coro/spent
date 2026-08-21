package com.app.spent.di

import com.app.spent.data.local.db.SpentDatabase
import com.app.spent.data.preferences.UserPreferencesRepository
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.repository.SpentRepositoryImpl
import com.app.spent.ui.analytics.AnalyticsViewModel
import com.app.spent.ui.dashboard.DashboardViewModel
import com.app.spent.ui.fixedbills.FixedBillsViewModel
import com.app.spent.ui.history.TransactionHistoryViewModel
import com.app.spent.ui.loanstracker.LoansTrackerViewModel
import com.app.spent.ui.onboarding.OnboardingViewModel
import com.app.spent.ui.savings.SavingsViewModel
import com.app.spent.ui.settings.SettingsViewModel
import com.app.spent.ui.sharedledger.SharedLedgerViewModel
import com.app.spent.ui.transaction.AddTransactionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Local Room DB & DAO
    single { SpentDatabase.getInstance(androidContext()) }
    single { get<SpentDatabase>().spentDao() }

    // DataStore Preferences
    single { UserPreferencesRepository(androidContext()) }

    // Repository Single Source of Truth
    single<SpentRepository> {
        SpentRepositoryImpl(
            context = androidContext(),
            dao = get(),
            preferencesRepository = get()
        )
    }

    // ViewModels
    viewModel { DashboardViewModel(repository = get()) }
    viewModel { AnalyticsViewModel(repository = get()) }
    viewModel { SettingsViewModel(repository = get()) }
    viewModel { (initialType: String) -> AddTransactionViewModel(repository = get(), initialType = initialType) }
    viewModel { FixedBillsViewModel(repository = get()) }
    viewModel { LoansTrackerViewModel(repository = get()) }
    viewModel { SavingsViewModel(repository = get()) }
    viewModel { SharedLedgerViewModel(repository = get()) }
    viewModel { OnboardingViewModel(repository = get()) }
    viewModel { TransactionHistoryViewModel(repository = get()) }
}
