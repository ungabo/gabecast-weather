package com.gabecast.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class CurrentWeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WeatherWidgetSupport.updateCurrentWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            WeatherWidgetSupport.updateProvider(context, CurrentWeatherWidget::class.java, WeatherWidgetSupport::updateCurrentWidget)
        }
    }
}
