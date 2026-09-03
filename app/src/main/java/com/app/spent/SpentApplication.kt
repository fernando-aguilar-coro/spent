package com.app.spent

import android.app.Application
import com.app.spent.data.repository.SpentRepository
import com.app.spent.di.appModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpentApplication : Application() {

    val repository: SpentRepository by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SpentApplication)
            modules(appModule)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.executePendingRecurringRules()
            } catch (e: Exception) {}
        }
    }
}

