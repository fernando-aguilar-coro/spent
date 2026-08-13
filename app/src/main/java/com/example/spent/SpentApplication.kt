package com.example.spent

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.spent.data.local.db.SpentDatabase
import com.example.spent.data.preferences.UserPreferencesRepository
import com.example.spent.data.repository.SpentRepository
import com.example.spent.data.repository.SpentRepositoryImpl
import com.example.spent.worker.RecurringTransactionWorker
import java.util.concurrent.TimeUnit

class SpentApplication : Application() {

    lateinit var repository: SpentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = SpentDatabase.getInstance(this)
        val preferencesRepository = UserPreferencesRepository(this)
        repository = SpentRepositoryImpl(database.spentDao(), preferencesRepository)

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
