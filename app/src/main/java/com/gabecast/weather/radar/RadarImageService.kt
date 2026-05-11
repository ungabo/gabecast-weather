package com.gabecast.weather.radar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import com.gabecast.weather.settings.UserSettingsRepository
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private const val RADAR_EXPORT_URL =
    "https://mapservices.weather.noaa.gov/eventdriven/rest/services/radar/radar_base_reflectivity_time/ImageServer/exportImage"
private const val BASEMAP_EXPORT_URL =
    "https://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/export"

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
            val radarBytes = body.bytes()
            val bitmap = BitmapFactory.decodeByteArray(radarBytes, 0, radarBytes.size)
                ?: throw IOException("Radar response was not a valid image")
            bitmap.recycle()
            val timestamp = response.header("Last-Modified")?.let {
                runCatching { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull()
            }
            val compositedBytes = runCatching {
                val basemapBytes = downloadBaseMapImage(box, width, height, contact)
                compositeRadarOverBasemap(basemapBytes, radarBytes, width, height)
            }.getOrDefault(radarBytes)
            return RadarDownload(bytes = compositedBytes, radarTimestamp = timestamp)
        }
    }

    private fun buildBaseMapUrl(box: RadarBoundingBox, width: Int, height: Int): String {
        return BASEMAP_EXPORT_URL.toHttpUrl().newBuilder()
            .addQueryParameter("bbox", "${box.minLon},${box.minLat},${box.maxLon},${box.maxLat}")
            .addQueryParameter("bboxSR", "4326")
            .addQueryParameter("imageSR", "4326")
            .addQueryParameter("size", "$width,$height")
            .addQueryParameter("format", "png32")
            .addQueryParameter("transparent", "false")
            .addQueryParameter("f", "image")
            .build()
            .toString()
    }

    private fun downloadBaseMapImage(box: RadarBoundingBox, width: Int, height: Int, contact: String): ByteArray {
        val request = Request.Builder()
            .url(buildBaseMapUrl(box, width, height))
            .header("User-Agent", "GabeCast/1.0 ($contact)")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Basemap request failed with HTTP ${response.code}")
            return response.body?.bytes() ?: throw IOException("Basemap response was empty")
        }
    }

    private fun compositeRadarOverBasemap(baseMapBytes: ByteArray, radarBytes: ByteArray, width: Int, height: Int): ByteArray {
        val base = BitmapFactory.decodeByteArray(baseMapBytes, 0, baseMapBytes.size)
            ?: throw IOException("Basemap response was not a valid image")
        val radar = BitmapFactory.decodeByteArray(radarBytes, 0, radarBytes.size)
            ?: throw IOException("Radar response was not a valid image")
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val target = Rect(0, 0, width, height)
        canvas.drawColor(Color.rgb(235, 241, 238))
        canvas.drawBitmap(base, null, target, null)
        canvas.drawBitmap(radar, null, target, null)
        val stream = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, stream)
        base.recycle()
        radar.recycle()
        output.recycle()
        return stream.toByteArray()
    }
}

data class RadarDownload(
    val bytes: ByteArray,
    val radarTimestamp: Instant?
)
