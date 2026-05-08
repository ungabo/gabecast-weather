package com.gabecast.weather.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.gabecast.weather.R
import com.gabecast.weather.domain.model.HourlyForecastPeriod
import com.gabecast.weather.util.formatShortTime

class HourlyWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = HourlyFactory()

    private inner class HourlyFactory : RemoteViewsFactory {
        private var rows: List<HourlyForecastPeriod> = emptyList()

        override fun onCreate() = Unit

        override fun onDataSetChanged() {
            rows = WeatherWidgetSupport.allHourly()
        }

        override fun onDestroy() {
            rows = emptyList()
        }

        override fun getCount(): Int = rows.size

        override fun getViewAt(position: Int): RemoteViews {
            val item = rows[position]
            val views = RemoteViews(packageName, R.layout.widget_hourly_item)
            views.setTextViewText(R.id.widget_hour_time, item.startTime.formatShortTime())
            views.setTextViewText(R.id.widget_hour_temp, item.temperatureF?.let { "$it\u00B0" } ?: "--")
            val rain = item.probabilityOfPrecipitationPercent?.let { "$it%" } ?: "--"
            views.setTextViewText(R.id.widget_hour_detail, "Rain $rain  ${item.shortForecast.orEmpty()}")
            return views
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = position.toLong()
        override fun hasStableIds(): Boolean = false
    }
}
