package com.gabecast.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

class CurrentWeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WeatherWidgetSupport.updateCurrentWidget(context, appWidgetManager, it, allowRefresh = true) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        WeatherWidgetSupport.updateCurrentWidget(context, appWidgetManager, appWidgetId, allowRefresh = false)
    }

    companion object {
        fun updateAll(context: Context, allowRefresh: Boolean = false) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CurrentWeatherWidget::class.java))
            ids.forEach { WeatherWidgetSupport.updateCurrentWidget(context, manager, it, allowRefresh) }
        }
    }
}
