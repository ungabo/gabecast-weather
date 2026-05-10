package com.gabecast.weather.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gabecast.weather.ServiceLocator
import com.gabecast.weather.widget.WeatherWidgets
import kotlinx.coroutines.flow.first

class WeatherRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val settings = ServiceLocator.settingsRepository.settings.first()
        if (!settings.backgroundRefreshEnabled) return Result.success()
        if (!settings.nwsContactEmailConfirmed) return Result.success()
        val selectedId = settings.selectedLocationId ?: return Result.success()
        return runCatching {
            ServiceLocator.weatherRepository.loadDashboardForDisplay(selectedId, forceRefresh = false)
            WeatherWidgets.updateAll(applicationContext)
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
