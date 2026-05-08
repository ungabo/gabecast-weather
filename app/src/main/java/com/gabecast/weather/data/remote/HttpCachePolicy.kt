package com.gabecast.weather.data.remote

import okhttp3.Headers
import retrofit2.Response
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

fun <T> Response<T>.expiresAtOrFallback(fallback: Duration, now: Instant = Instant.now()): Instant {
    return headers().cacheExpiresAt(now) ?: now.plusMillis(fallback.inWholeMilliseconds)
}

fun Headers.cacheExpiresAt(now: Instant = Instant.now()): Instant? {
    val cacheControl = this["Cache-Control"]
    val maxAge = cacheControl
        ?.split(",")
        ?.map { it.trim() }
        ?.firstNotNullOfOrNull { part ->
            val pieces = part.split("=")
            if (pieces.size == 2 && pieces[0].equals("max-age", ignoreCase = true)) {
                pieces[1].toLongOrNull()
            } else {
                null
            }
        }
    if (maxAge != null) return now.plusSeconds(maxAge)

    val expires = this["Expires"] ?: return null
    return runCatching {
        ZonedDateTime.parse(expires, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
    }.getOrNull()
}

object NwsFallbackTtl {
    val Point = 7.hours * 24
    val Daily = 2.hours
    val Hourly = 1.hours
    val Current = 15.minutes
    val Alerts = 5.minutes
    val Stations = 24.hours
}
