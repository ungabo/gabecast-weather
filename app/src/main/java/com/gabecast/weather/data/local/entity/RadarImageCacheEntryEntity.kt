package com.gabecast.weather.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "radar_image_cache",
    primaryKeys = ["locationId", "radiusMiles", "imageWidth", "imageHeight"],
    indices = [Index("locationId")]
)
data class RadarImageCacheEntryEntity(
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
    val radarTimestampMillis: Long?,
    val fetchedAtMillis: Long,
    val expiresAtMillis: Long
)
