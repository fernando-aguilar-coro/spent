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
    single { get<SpentDatabase>().assetDao() }

    // DataStore Preferences
    single { UserPreferencesRepository(androidContext()) }

    // Remote & Simulation Market Price Providers
    single { com.app.spent.data.remote.ExchangeRateApiPriceProvider() }
    single { com.app.spent.data.remote.CryptoPriceProvider(exchangeRateApiProvider = get()) }
    single { com.app.spent.data.remote.StockPriceProvider(exchangeRateApiProvider = get()) }
    single<com.app.spent.domain.asset.MarketPriceProvider> {
        com.app.spent.domain.asset.CompositeMarketPriceProvider(
            exchangeRateApiProvider = get(),
            simulatedEngine = com.app.spent.domain.asset.SimulatedMarketPriceEngine(),
            cryptoPriceProvider = get(),
            stockPriceProvider = get()
        )
    }

    // Repository Single Source of Truth
    single<SpentRepository> {
        SpentRepositoryImpl(
            context = androidContext(),
            dao = get(),
            preferencesRepository = get(),
            assetDao = get()
        )
    }

    single<com.app.spent.data.repository.AssetRepository> {
        com.app.spent.data.repository.AssetRepositoryImpl(
            assetDao = get(),
            priceProvider = get()
        )
    }

    // ViewModels
    viewModel { DashboardViewModel(repository = get(), assetRepository = get()) }

    viewModel { AnalyticsViewModel(repository = get()) }
    viewModel { SettingsViewModel(repository = get()) }
    viewModel { (initialType: String, transactionId: String?) ->
        AddTransactionViewModel(repository = get(), initialType = initialType, transactionId = transactionId)
    }
    viewModel { FixedBillsViewModel(repository = get()) }
    viewModel { LoansTrackerViewModel(repository = get()) }
    viewModel { SavingsViewModel(repository = get()) }
    viewModel { SharedLedgerViewModel(repository = get()) }
    viewModel { OnboardingViewModel(repository = get()) }
    viewModel { TransactionHistoryViewModel(repository = get()) }
    viewModel { com.app.spent.ui.portfolio.PortfolioViewModel(repository = get(), spentRepository = get()) }
}

