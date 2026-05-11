package com.gabecast.weather.util

import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun Instant?.toEpochMillisOrNull(): Long? = this?.toEpochMilli()

fun Long?.toInstantOrNull(): Instant? = this?.let { Instant.ofEpochMilli(it) }

fun parseInstantOrNull(value: String?): Instant? = value?.let {
    runCatching { Instant.parse(it) }.getOrNull()
}

fun Instant.formatShortTime(zoneId: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(zoneId)
        .format(this)

fun Instant.formatShortDateTime(zoneId: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(zoneId)
        .format(this)

fun Instant?.formatUpdatedAge(now: Instant = Instant.now()): String {
    if (this == null) return "Updated --"
    val minutes = Duration.between(this, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "Updated now"
        minutes < 60 -> "Updated $minutes min ago"
        else -> {
            val hours = minutes / 60
            "Updated $hours hr ago"
        }
    }
}
