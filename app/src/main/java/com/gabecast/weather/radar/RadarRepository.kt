package com.gabecast.weather.radar

import android.content.Context
import com.gabecast.weather.data.local.dao.WeatherDao
import com.gabecast.weather.data.local.mapper.toDomain
import com.gabecast.weather.data.local.mapper.toEntity
import com.gabecast.weather.domain.model.SavedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class RadarRepository(
    context: Context,
    private val dao: WeatherDao,
    private val radarImageService: RadarImageService
) {
    private val radarDirectory = File(context.cacheDir, "radar_images")

    suspend fun getLatestRadarImage(
        location: SavedLocation,
        radiusMiles: Int = 75,
        imageWidth: Int = 1024,
        imageHeight: Int = 1024,
        forceRefresh: Boolean = false
    ): RadarImageResult = withContext(Dispatchers.IO) {
        radarDirectory.mkdirs()
        cleanupOldRadarImages()
        val now = Instant.now()
        val cached = dao.getRadarImage(location.id, radiusMiles, imageWidth, imageHeight)?.toDomain()
            ?.takeIf { File(it.imageFilePath).exists() }
        if (!forceRefresh && cached != null && cached.expiresAt.isAfter(now)) {
            return@withContext cached.toResult(location.displayName, now)
        }

        runCatching {
            val box = radarBoundingBox(location.latitude, location.longitude, radiusMiles)
            val download = radarImageService.downloadRadarImage(box, imageWidth, imageHeight)
            val imageFile = File(
                radarDirectory,
                "radar_${location.id}_${radiusMiles}_${imageWidth}x$imageHeight.png"
            )
            imageFile.writeBytes(download.bytes)
            val fetchedAt = Instant.now()
            val entry = RadarImageCacheEntry(
                locationId = location.id,
                latitude = location.latitude,
                longitude = location.longitude,
                radiusMiles = radiusMiles,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                minLat = box.minLat,
                minLon = box.minLon,
                maxLat = box.maxLat,
                maxLon = box.maxLon,
                imageFilePath = imageFile.absolutePath,
                radarTimestamp = download.radarTimestamp,
                fetchedAt = fetchedAt,
                expiresAt = fetchedAt.plusMillis(10.minutes.inWholeMilliseconds)
            )
            dao.upsertRadarImage(entry.toEntity())
            entry.toResult(location.displayName, fetchedAt)
        }.getOrElse { error ->
            if (cached != null) {
                cached.toResult(
                    locationName = location.displayName,
                    now = now,
                    errorMessage = "Could not refresh radar. Showing last available image."
                )
            } else {
                RadarImageResult(
                    locationName = location.displayName,
                    imageFilePath = null,
                    radarTimestamp = null,
                    fetchedAt = null,
                    expiresAt = null,
                    isStale = false,
                    errorMessage = error.message?.takeIf { it.isNotBlank() } ?: "Radar image unavailable right now."
                )
            }
        }
    }

    suspend fun getCachedRadarForWidget(location: SavedLocation): RadarImageResult? = withContext(Dispatchers.IO) {
        dao.getRadarImage(location.id, 75, 1024, 1024)
            ?.toDomain()
            ?.takeIf { File(it.imageFilePath).exists() }
            ?.toResult(location.displayName, Instant.now())
    }

    private fun RadarImageCacheEntry.toResult(
        locationName: String,
        now: Instant,
        errorMessage: String? = null
    ): RadarImageResult {
        val stale = fetchedAt.plusMillis(30.minutes.inWholeMilliseconds).isBefore(now)
        return RadarImageResult(
            locationName = locationName,
            imageFilePath = imageFilePath,
            radarTimestamp = radarTimestamp,
            fetchedAt = fetchedAt,
            expiresAt = expiresAt,
            isStale = stale || errorMessage != null,
            errorMessage = errorMessage
        )
    }

    private suspend fun cleanupOldRadarImages() {
        val olderThan = Instant.now().minusMillis(24.hours.inWholeMilliseconds).toEpochMilli()
        dao.getOldRadarImagePaths(olderThan).forEach { path ->
            runCatching { File(path).delete() }
        }
        dao.deleteOldRadarImages(olderThan)
    }
}
