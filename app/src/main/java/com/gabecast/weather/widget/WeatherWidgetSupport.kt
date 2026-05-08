package com.gabecast.weather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import com.gabecast.weather.MainActivity
import com.gabecast.weather.R
import com.gabecast.weather.ServiceLocator
import com.gabecast.weather.domain.model.HourlyForecastPeriod
import com.gabecast.weather.domain.model.WeatherDashboard
import com.gabecast.weather.util.formatShortTime
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

    fun loadDashboard(): WeatherDashboard? = runBlocking(Dispatchers.IO) {
        val selectedId = ServiceLocator.settingsRepository.settingsSnapshotSelectedId()
        selectedId?.let { ServiceLocator.weatherRepository.loadCachedDashboard(it) }
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
        return precip?.let { "Rain $it%" } ?: "Rain --"
    }

    fun windText(dashboard: WeatherDashboard?): String {
        val current = dashboard?.currentConditions
        val speed = current?.windSpeedMph?.toInt()
        return if (speed != null) {
            "Wind $speed mph ${current.windDirection.orEmpty()}".trim()
        } else {
            "Wind --"
        }
    }

    fun conditionText(dashboard: WeatherDashboard?): String {
        return dashboard?.currentConditions?.conditionText
            ?: dashboard?.dailyForecast?.firstOrNull()?.shortForecast
            ?: "Open app to load weather"
    }

    fun updateCurrentWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val dashboard = loadDashboard()
        val views = RemoteViews(context.packageName, R.layout.widget_weather)
        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        views.setTextViewText(R.id.widget_location, dashboard?.location?.displayName ?: "GabeCast")
        views.setTextViewText(R.id.widget_temp, tempText(dashboard?.currentConditions?.temperatureF))
        views.setTextViewText(R.id.widget_condition, conditionText(dashboard))
        manager.updateAppWidget(appWidgetId, views)
    }

    fun updateCompactWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val dashboard = loadDashboard()
        val views = RemoteViews(context.packageName, R.layout.widget_compact_weather)
        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        views.setTextViewText(R.id.widget_location, dashboard?.location?.displayName ?: "GabeCast")
        views.setTextViewText(R.id.widget_temp, tempText(dashboard?.currentConditions?.temperatureF))
        views.setTextViewText(R.id.widget_high_low, highLowText(dashboard))
        manager.updateAppWidget(appWidgetId, views)
    }

    fun updateDetailedWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val dashboard = loadDashboard()
        val views = RemoteViews(context.packageName, R.layout.widget_detailed_weather)
        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        views.setTextViewText(R.id.widget_location, dashboard?.location?.displayName ?: "GabeCast")
        views.setTextViewText(R.id.widget_temp, tempText(dashboard?.currentConditions?.temperatureF))
        views.setTextViewText(R.id.widget_high_low, highLowText(dashboard))
        views.setTextViewText(R.id.widget_condition, conditionText(dashboard))
        views.setTextViewText(R.id.widget_precip_wind, "${precipText(dashboard)}  ${windText(dashboard)}")
        manager.updateAppWidget(appWidgetId, views)
    }

    fun updateHourlyWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val dashboard = loadDashboard()
        val views = RemoteViews(context.packageName, R.layout.widget_hourly_weather)
        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        views.setTextViewText(R.id.widget_location, dashboard?.location?.displayName ?: "Next 8 hours")
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

    fun updateRadarWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val dashboard = loadDashboard()
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

    fun updateProvider(context: Context, provider: Class<*>, updater: (Context, AppWidgetManager, Int) -> Unit) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, provider))
        ids.forEach { updater(context, manager, it) }
    }

    fun allHourly(): List<HourlyForecastPeriod> {
        return loadDashboard()?.hourlyForecast.orEmpty().take(8)
    }
}
