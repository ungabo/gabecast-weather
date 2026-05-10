package com.gabecast.weather.widget

import android.content.Context

object WeatherWidgets {
    fun updateAll(context: Context, allowRefresh: Boolean = false) {
        CurrentWeatherWidget.updateAll(context, allowRefresh)
        CompactWeatherWidget.updateAll(context, allowRefresh)
        DetailedWeatherWidget.updateAll(context, allowRefresh)
        HourlyWeatherWidget.updateAll(context, allowRefresh)
        RadarWeatherWidget.updateAll(context, allowRefresh)
    }
}
