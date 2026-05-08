package com.gabecast.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class RadarWeatherWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WeatherWidgetSupport.updateRadarWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            WeatherWidgetSupport.updateProvider(context, RadarWeatherWidget::class.java, WeatherWidgetSupport::updateRadarWidget)
        }
    }
}
