package com.gabecast.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class HourlyWeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WeatherWidgetSupport.updateHourlyWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            WeatherWidgetSupport.updateProvider(context, HourlyWeatherWidget::class.java, WeatherWidgetSupport::updateHourlyWidget)
        }
    }
}
