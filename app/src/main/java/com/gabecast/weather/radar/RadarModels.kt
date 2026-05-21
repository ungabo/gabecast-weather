package com.gabecast.weather.radar

import java.time.Instant
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

data class RadarBoundingBox(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double
)

data class RadarProjectedExtent(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double,
    val spatialReference: Int = WEB_MERCATOR_WKID
) {
    fun toBboxParameter(): String = "$minX,$minY,$maxX,$maxY"
}

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

fun radarProjectedExtent(latitude: Double, longitude: Double, radiusMiles: Int): RadarProjectedExtent {
    val clampedLat = latitude.coerceIn(MIN_WEB_MERCATOR_LATITUDE, MAX_WEB_MERCATOR_LATITUDE)
    val center = webMercatorPoint(clampedLat, longitude.coerceIn(-180.0, 180.0))
    val localScale = cos(Math.toRadians(clampedLat)).coerceAtLeast(0.15)
    val projectedRadiusMeters = radiusMiles * METERS_PER_MILE / localScale
    return RadarProjectedExtent(
        minX = center.first - projectedRadiusMeters,
        minY = center.second - projectedRadiusMeters,
        maxX = center.first + projectedRadiusMeters,
        maxY = center.second + projectedRadiusMeters
    )
}

private fun webMercatorPoint(latitude: Double, longitude: Double): Pair<Double, Double> {
    val x = EARTH_RADIUS_METERS * Math.toRadians(longitude)
    val latitudeRadians = Math.toRadians(latitude)
    val y = EARTH_RADIUS_METERS * 0.5 * ln((1 + sin(latitudeRadians)) / (1 - sin(latitudeRadians)))
    return x to y
}

private const val EARTH_RADIUS_METERS = 6378137.0
private const val METERS_PER_MILE = 1609.344
private const val MIN_WEB_MERCATOR_LATITUDE = -85.05112878
private const val MAX_WEB_MERCATOR_LATITUDE = 85.05112878
private const val WEB_MERCATOR_WKID = 3857
