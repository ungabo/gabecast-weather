package com.gabecast.weather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.gabecast.weather.MainActivity
import com.gabecast.weather.R
import com.gabecast.weather.ServiceLocator
import com.gabecast.weather.domain.model.HourlyForecastPeriod
import com.gabecast.weather.domain.model.WeatherDashboard
import com.gabecast.weather.util.formatUpdatedAge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

internal object WeatherWidgetSupport {
    fun launchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun refreshPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetRefreshReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadDashboard(allowRefresh: Boolean): WeatherDashboard? = runBlocking(Dispatchers.IO) {
        val selectedId = ServiceLocator.settingsRepository.settingsSnapshotSelectedId()
        selectedId?.let {
            if (allowRefresh) {
                ServiceLocator.weatherRepository.loadDashboardForDisplay(it, forceRefresh = false)
            } else {
                ServiceLocator.weatherRepository.loadCachedDashboard(it)
            }
        }
    }

    fun tempText(valueF: Double?): String = valueF?.let { "${it.toInt()}\u00B0F" } ?: "--"

    fun highLowText(dashboard: WeatherDashboard?): String {
        val temps = dashboard?.dailyForecast.orEmpty().take(2).mapNotNull { it.temperatureF }
        val high = temps.maxOrNull()?.let { "H $it\u00B0" } ?: "H --"
        val low = temps.minOrNull()?.let { "L $it\u00B0" } ?: "L --"
        return "$high  $low"
    }

    fun precipText(dashboard: WeatherDashboard?): String {
        val precip = dashboard?.hourlyForecast.orEmpty()
            .take(8)
            .mapNotNull { it.probabilityOfPrecipitationPercent }
            .maxOrNull()
        return precip?.let { "$it%" } ?: "--"
    }

    fun windText(dashboard: WeatherDashboard?): String {
        val current = dashboard?.currentConditions
        val speed = current?.windSpeedMph?.toInt()
        return speed?.toString() ?: "--"
    }

    fun updatedText(dashboard: WeatherDashboard?): String {
        return (dashboard?.currentConditions?.fetchedAt ?: dashboard?.fetchedAt).formatUpdatedAge()
    }

    fun hourlyWindText(period: HourlyForecastPeriod): String {
        return period.windSpeed?.let { WIND_NUMBER.find(it)?.value } ?: "--"
    }

    fun hourlyPrecipText(period: HourlyForecastPeriod): String {
        return period.probabilityOfPrecipitationPercent?.let { "$it%" } ?: "--"
    }

    fun conditionText(dashboard: WeatherDashboard?): String {
        return dashboard?.currentConditions?.conditionText
            ?: dashboard?.dailyForecast?.firstOrNull()?.shortForecast
            ?: "Open app to load weather"
    }

    fun conditionIconRes(forecast: String?): Int {
        val text = forecast.orEmpty().lowercase()
        return when {
            "thunder" in text || "storm" in text || "lightning" in text -> R.drawable.ic_weather_storm
            "snow" in text || "sleet" in text || "ice" in text || "freezing" in text -> R.drawable.ic_weather_snow
            "rain" in text || "shower" in text || "drizzle" in text -> R.drawable.ic_weather_rain
            "fog" in text || "mist" in text || "haze" in text || "smoke" in text -> R.drawable.ic_weather_fog
            "wind" in text || "breezy" in text || "gust" in text -> R.drawable.ic_weather_wind
            "partly" in text || "mostly sunny" in text || "mostly clear" in text -> R.drawable.ic_weather_partly_cloudy
            "cloud" in text || "overcast" in text -> R.drawable.ic_weather_cloudy
            else -> R.drawable.ic_weather_sunny
        }
    }

    private fun currentLayoutId(options: Bundle): Int {
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            .coerceAtLeast(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH))
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            .coerceAtLeast(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT))
        return if (height >= width) R.layout.widget_weather_vertical else R.layout.widget_weather
    }

    fun updateCurrentWidget(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        allowRefresh: Boolean = false
    ) {
        val dashboard = loadDashboard(allowRefresh)
        val layoutId = currentLayoutId(manager.getAppWidgetOptions(appWidgetId))
        val views = RemoteViews(context.packageName, layoutId)
        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        views.setTextViewText(R.id.widget_location, dashboard?.location?.displayName ?: "GabeCast")
        views.setTextViewText(R.id.widget_temp, tempText(dashboard?.currentConditions?.temperatureF))
        views.setImageViewResource(R.id.widget_icon, conditionIconRes(conditionText(dashboard)))
        views.setTextViewText(R.id.widget_high_low, highLowText(dashboard))
        views.setTextViewText(R.id.widget_updated, updatedText(dashboard))
        views.setOnClickPendingIntent(R.id.widget_updated, refreshPendingIntent(context))
        manager.updateAppWidget(appWidgetId, views)
    }

    fun updateCompactWidget(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        allowRefresh: Boolean = false
    ) {
        val dashboard = loadDashboard(allowRefresh)
        val views = RemoteViews(context.packageName, R.layout.widget_compact_weather)
        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        views.setTextViewText(R.id.widget_location, dashboard?.location?.displayName ?: "GabeCast")
        views.setTextViewText(R.id.widget_temp, tempText(dashboard?.currentConditions?.temperatureF))
        views.setTextViewText(R.id.widget_high_low, highLowText(dashboard))
        views.setTextViewText(R.id.widget_updated, updatedText(dashboard))
        views.setOnClickPendingIntent(R.id.widget_updated, refreshPendingIntent(context))
        views.setImageViewResource(R.id.widget_icon, conditionIconRes(conditionText(dashboard)))
        manager.updateAppWidget(appWidgetId, views)
    }

    fun updateDetailedWidget(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        allowRefresh: Boolean = false
    ) {
        val dashboard = loadDashboard(allowRefresh)
        val views = RemoteViews(context.packageName, R.layout.widget_detailed_weather)
        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        views.setTextViewText(R.id.widget_location, dashboard?.location?.displayName ?: "GabeCast")
        views.setTextViewText(R.id.widget_temp, tempText(dashboard?.currentConditions?.temperatureF))
        views.setTextViewText(R.id.widget_high_low, highLowText(dashboard))
        views.setTextViewText(R.id.widget_updated, updatedText(dashboard))
        views.setOnClickPendingIntent(R.id.widget_updated, refreshPendingIntent(context))
        views.setTextViewText(R.id.widget_precip_value, precipText(dashboard))
        views.setTextViewText(R.id.widget_wind_value, windText(dashboard))
        views.setImageViewResource(R.id.widget_icon, conditionIconRes(conditionText(dashboard)))
        manager.updateAppWidget(appWidgetId, views)
    }

    fun updateHourlyWidget(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        allowRefresh: Boolean = false
    ) {
        val dashboard = loadDashboard(allowRefresh)
        val views = RemoteViews(context.packageName, R.layout.widget_hourly_weather)
        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        views.setTextViewText(R.id.widget_location, dashboard?.location?.displayName ?: "Next 8 hours")
        views.setTextViewText(R.id.widget_updated, updatedText(dashboard))
        views.setOnClickPendingIntent(R.id.widget_updated, refreshPendingIntent(context))
        val intent = Intent(context, HourlyWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_hourly_list, intent)
        views.setEmptyView(R.id.widget_hourly_list, R.id.widget_empty)
        views.setViewVisibility(R.id.widget_empty, if (dashboard?.hourlyForecast.isNullOrEmpty()) View.VISIBLE else View.GONE)
        manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_hourly_list)
        manager.updateAppWidget(appWidgetId, views)
    }

    fun updateRadarWidget(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        allowRefresh: Boolean = false
    ) {
        val dashboard = loadDashboard(allowRefresh)
        val views = RemoteViews(context.packageName, R.layout.widget_radar_weather)
        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        val radar = runBlocking(Dispatchers.IO) {
            dashboard?.location?.let { ServiceLocator.radarRepository.getCachedRadarForWidget(it) }
        }
        val bitmap = radar?.imageFilePath?.let { BitmapFactory.decodeFile(it) }
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.widget_radar_image, bitmap)
            views.setTextViewText(R.id.widget_radar_label, "Radar: ${radar.locationName}")
        } else {
            views.setImageViewResource(R.id.widget_radar_image, R.drawable.gabecast_radar_placeholder)
            views.setTextViewText(R.id.widget_radar_label, "Open Radar tab to load")
        }
        manager.updateAppWidget(appWidgetId, views)
    }

    fun allHourly(): List<HourlyForecastPeriod> {
        return loadDashboard(allowRefresh = false)?.hourlyForecast.orEmpty().take(8)
    }

    private val WIND_NUMBER = Regex("""\d+""")
}
