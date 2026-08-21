package com.app.spent.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.spent.data.repository.SpentRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RecurringTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: SpentRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            repository.executePendingRecurringRules()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

