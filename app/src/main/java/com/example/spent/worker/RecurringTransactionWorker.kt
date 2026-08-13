package com.example.spent.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.spent.data.local.db.SpentDatabase
import com.example.spent.data.preferences.UserPreferencesRepository
import com.example.spent.data.repository.SpentRepositoryImpl

class RecurringTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = SpentDatabase.getInstance(applicationContext)
            val prefs = UserPreferencesRepository(applicationContext)
            val repository = SpentRepositoryImpl(db.spentDao(), prefs)

            repository.executePendingRecurringRules()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
