package com.gabecast.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

class RadarWeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WeatherWidgetSupport.updateRadarWidget(context, appWidgetManager, it, allowRefresh = true) }
    }

    companion object {
        fun updateAll(context: Context, allowRefresh: Boolean = false) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, RadarWeatherWidget::class.java))
            ids.forEach { WeatherWidgetSupport.updateRadarWidget(context, manager, it, allowRefresh) }
        }
    }
}
