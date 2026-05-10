package com.gabecast.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

class HourlyWeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WeatherWidgetSupport.updateHourlyWidget(context, appWidgetManager, it, allowRefresh = true) }
    }

    companion object {
        fun updateAll(context: Context, allowRefresh: Boolean = false) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, HourlyWeatherWidget::class.java))
            ids.forEach { WeatherWidgetSupport.updateHourlyWidget(context, manager, it, allowRefresh) }
        }
    }
}
