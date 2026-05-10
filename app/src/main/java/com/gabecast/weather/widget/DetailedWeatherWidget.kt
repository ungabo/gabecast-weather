package com.gabecast.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

class DetailedWeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WeatherWidgetSupport.updateDetailedWidget(context, appWidgetManager, it, allowRefresh = true) }
    }

    companion object {
        fun updateAll(context: Context, allowRefresh: Boolean = false) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DetailedWeatherWidget::class.java))
            ids.forEach { WeatherWidgetSupport.updateDetailedWidget(context, manager, it, allowRefresh) }
        }
    }
}
