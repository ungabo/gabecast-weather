package com.gabecast.weather.radar

import android.content.Context
import android.graphics.BitmapFactory
import com.gabecast.weather.settings.UserSettingsRepository
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private const val RADAR_EXPORT_URL =
    "https://mapservices.weather.noaa.gov/eventdriven/rest/services/radar/radar_base_reflectivity_time/ImageServer/exportImage"

class RadarImageService(context: Context) {
    private val contactPrefs = context.getSharedPreferences("request_contact", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http_radar"), 20L * 1024L * 1024L))
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(24, TimeUnit.SECONDS)
        .build()

    fun buildRadarUrl(box: RadarBoundingBox, width: Int, height: Int): String {
        return RADAR_EXPORT_URL.toHttpUrl().newBuilder()
            .addQueryParameter("bbox", "${box.minLon},${box.minLat},${box.maxLon},${box.maxLat}")
            .addQueryParameter("bboxSR", "4326")
            .addQueryParameter("imageSR", "4326")
            .addQueryParameter("size", "$width,$height")
            .addQueryParameter("format", "png32")
            .addQueryParameter("transparent", "true")
            .addQueryParameter("f", "image")
            .build()
            .toString()
    }

    fun downloadRadarImage(box: RadarBoundingBox, width: Int, height: Int): RadarDownload {
        val confirmed = contactPrefs.getBoolean(UserSettingsRepository.CONTACT_CONFIRMED_PREF_KEY, false)
        val contact = contactPrefs
            .getString(UserSettingsRepository.CONTACT_PREF_KEY, "")
            ?.takeIf { it.isNotBlank() }
        if (!confirmed || contact == null) {
            throw IOException("Enter a contact email before requesting radar data.")
        }
        val request = Request.Builder()
            .url(buildRadarUrl(box, width, height))
            .header("User-Agent", "GabeCast/1.0 ($contact)")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Radar request failed with HTTP ${response.code}")
            val body = response.body ?: throw IOException("Radar response was empty")
            val bytes = body.bytes()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IOException("Radar response was not a valid image")
            bitmap.recycle()
            val timestamp = response.header("Last-Modified")?.let {
                runCatching { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull()
            }
            return RadarDownload(bytes = bytes, radarTimestamp = timestamp)
        }
    }
}

data class RadarDownload(
    val bytes: ByteArray,
    val radarTimestamp: Instant?
)
