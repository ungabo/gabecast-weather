package com.gabecast.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class DetailedWeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WeatherWidgetSupport.updateDetailedWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            WeatherWidgetSupport.updateProvider(context, DetailedWeatherWidget::class.java, WeatherWidgetSupport::updateDetailedWidget)
        }
    }
}
