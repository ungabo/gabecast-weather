package com.gabecast.weather.data.local.mapper

import com.gabecast.weather.data.local.entity.RadarImageCacheEntryEntity
import com.gabecast.weather.radar.RadarImageCacheEntry
import com.gabecast.weather.util.toEpochMillisOrNull
import com.gabecast.weather.util.toInstantOrNull
import java.time.Instant

fun RadarImageCacheEntryEntity.toDomain(): RadarImageCacheEntry = RadarImageCacheEntry(
    locationId = locationId,
    latitude = latitude,
    longitude = longitude,
    radiusMiles = radiusMiles,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    minLat = minLat,
    minLon = minLon,
    maxLat = maxLat,
    maxLon = maxLon,
    imageFilePath = imageFilePath,
    radarTimestamp = radarTimestampMillis.toInstantOrNull(),
    fetchedAt = Instant.ofEpochMilli(fetchedAtMillis),
    expiresAt = Instant.ofEpochMilli(expiresAtMillis)
)

fun RadarImageCacheEntry.toEntity(): RadarImageCacheEntryEntity = RadarImageCacheEntryEntity(
    locationId = locationId,
    latitude = latitude,
    longitude = longitude,
    radiusMiles = radiusMiles,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    minLat = minLat,
    minLon = minLon,
    maxLat = maxLat,
    maxLon = maxLon,
    imageFilePath = imageFilePath,
    radarTimestampMillis = radarTimestamp.toEpochMillisOrNull(),
    fetchedAtMillis = fetchedAt.toEpochMilli(),
    expiresAtMillis = expiresAt.toEpochMilli()
)
