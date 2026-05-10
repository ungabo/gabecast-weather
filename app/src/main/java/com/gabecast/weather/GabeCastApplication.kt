package com.gabecast.weather

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gabecast.weather.worker.WeatherRefreshWorker
import java.util.concurrent.TimeUnit

class GabeCastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
        val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weather-refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
