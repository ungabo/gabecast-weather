package com.gabecast.weather.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gabecast.weather.MainActivity
import com.gabecast.weather.R
import com.gabecast.weather.domain.model.WeatherAlert
import kotlin.math.absoluteValue

class AlertNotificationHelper(private val context: Context) {
    private val prefs = context.getSharedPreferences("alert_notifications", Context.MODE_PRIVATE)

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Weather alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "First-time notifications for newly active weather alerts."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notifyNewAlerts(alerts: List<WeatherAlert>) {
        if (alerts.isEmpty() || !canNotify()) return
        ensureChannel()
        val notified = prefs.getStringSet(KEY_NOTIFIED_ALERT_IDS, emptySet()).orEmpty().toMutableSet()
        val activeIds = alerts.map { it.id }.toSet()
        val freshAlerts = alerts.filter { it.id !in notified }
        if (freshAlerts.isEmpty()) {
            trimStoredIds(notified, activeIds)
            return
        }

        val manager = NotificationManagerCompat.from(context)
        freshAlerts.forEach { alert ->
            manager.notify(alert.id.hashCode().absoluteValue, buildNotification(alert))
            notified += alert.id
        }
        trimStoredIds(notified, activeIds)
    }

    private fun buildNotification(alert: WeatherAlert): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = alert.event.ifBlank { "Weather alert" }
        val text = alert.headline ?: alert.areaDescription ?: "New weather alert for your saved location."
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    private fun canNotify(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun trimStoredIds(notified: MutableSet<String>, activeIds: Set<String>) {
        notified.retainAll(activeIds)
        prefs.edit().putStringSet(KEY_NOTIFIED_ALERT_IDS, notified).apply()
    }

    companion object {
        private const val CHANNEL_ID = "weather_alerts"
        private const val KEY_NOTIFIED_ALERT_IDS = "notified_alert_ids"
    }
}
