package com.app.spent

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.spent.data.repository.SpentRepository
import com.app.spent.di.appModule
import com.app.spent.worker.RecurringTransactionWorker
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class SpentApplication : Application() {

    val repository: SpentRepository by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SpentApplication)
            modules(appModule)
        }

        scheduleRecurringTransactionsWorker()
    }

    private fun scheduleRecurringTransactionsWorker() {
        val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "recurring_transactions_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

