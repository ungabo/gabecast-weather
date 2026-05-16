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
        val manualRefresh = inputData.getBoolean(KEY_MANUAL_REFRESH, false)
        val settings = ServiceLocator.settingsRepository.settings.first()
        if (!manualRefresh && !settings.backgroundRefreshEnabled) return Result.success()
        if (!settings.nwsContactEmailConfirmed) return Result.success()
        val selectedId = settings.selectedLocationId ?: return Result.success()
        return runCatching {
            val dashboard = ServiceLocator.weatherRepository.loadDashboardForDisplay(selectedId, forceRefresh = false)
            ServiceLocator.alertNotificationHelper.notifyNewAlerts(dashboard.alerts)
            WeatherWidgets.updateAll(applicationContext)
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        const val KEY_MANUAL_REFRESH = "manual_refresh"
    }
}
