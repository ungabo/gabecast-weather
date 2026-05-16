package com.gabecast.weather.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.gabecast.weather.ServiceLocator
import com.gabecast.weather.worker.WeatherRefreshWorker

class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ServiceLocator.initialize(context.applicationContext)
        val request = OneTimeWorkRequestBuilder<WeatherRefreshWorker>()
            .setInputData(workDataOf(WeatherRefreshWorker.KEY_MANUAL_REFRESH to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "weather-widget-refresh-now",
            ExistingWorkPolicy.REPLACE,
            request
        )
        WeatherWidgets.updateAll(context.applicationContext)
    }
}
