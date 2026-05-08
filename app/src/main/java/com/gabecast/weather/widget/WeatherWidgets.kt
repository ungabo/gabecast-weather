package com.gabecast.weather.widget

import android.content.Context

object WeatherWidgets {
    fun updateAll(context: Context) {
        CurrentWeatherWidget.updateAll(context)
        CompactWeatherWidget.updateAll(context)
        DetailedWeatherWidget.updateAll(context)
        HourlyWeatherWidget.updateAll(context)
        RadarWeatherWidget.updateAll(context)
    }
}
