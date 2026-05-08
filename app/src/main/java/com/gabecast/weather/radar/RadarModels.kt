package com.gabecast.weather.radar

import java.time.Instant

data class RadarBoundingBox(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double
)

data class RadarImageCacheEntry(
    val locationId: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMiles: Int,
    val imageWidth: Int,
    val imageHeight: Int,
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
    val imageFilePath: String,
    val radarTimestamp: Instant?,
    val fetchedAt: Instant,
    val expiresAt: Instant
)

data class RadarImageResult(
    val locationName: String,
    val imageFilePath: String?,
    val radarTimestamp: Instant?,
    val fetchedAt: Instant?,
    val expiresAt: Instant?,
    val isStale: Boolean,
    val errorMessage: String? = null
)

fun radarBoundingBox(latitude: Double, longitude: Double, radiusMiles: Int): RadarBoundingBox {
    val latDelta = radiusMiles / 69.0
    val lonMiles = 69.0 * kotlin.math.cos(Math.toRadians(latitude)).coerceAtLeast(0.15)
    val lonDelta = radiusMiles / lonMiles
    return RadarBoundingBox(
        minLat = (latitude - latDelta).coerceIn(-90.0, 90.0),
        minLon = (longitude - lonDelta).coerceIn(-180.0, 180.0),
        maxLat = (latitude + latDelta).coerceIn(-90.0, 90.0),
        maxLon = (longitude + lonDelta).coerceIn(-180.0, 180.0)
    )
}
