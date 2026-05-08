package com.gabecast.weather.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val state: String?,
    val country: String,
    val nwsGridId: String?,
    val nwsGridX: Int?,
    val nwsGridY: Int?,
    val forecastUrl: String?,
    val hourlyForecastUrl: String?,
    val observationStationsUrl: String?,
    val forecastZoneUrl: String?,
    val countyZoneUrl: String?,
    val nearestStationId: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(tableName = "current_conditions")
data class CurrentConditionsEntity(
    @PrimaryKey val locationId: String,
    val stationId: String?,
    val temperatureF: Double?,
    val conditionText: String?,
    val humidityPercent: Double?,
    val windSpeedMph: Double?,
    val windDirection: String?,
    val pressureInHg: Double?,
    val visibilityMiles: Double?,
    val observedAtMillis: Long?,
    val fetchedAtMillis: Long,
    val expiresAtMillis: Long?
)

@Entity(
    tableName = "daily_forecast_periods",
    primaryKeys = ["locationId", "periodNumber"],
    indices = [Index("locationId")]
)
data class DailyForecastPeriodEntity(
    val locationId: String,
    val periodNumber: Int,
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isDaytime: Boolean,
    val temperatureF: Int?,
    val windSpeed: String?,
    val windDirection: String?,
    val shortForecast: String?,
    val detailedForecast: String?,
    val probabilityOfPrecipitationPercent: Int?,
    val fetchedAtMillis: Long,
    val expiresAtMillis: Long?
)

@Entity(
    tableName = "hourly_forecast_periods",
    primaryKeys = ["locationId", "startTimeMillis"],
    indices = [Index("locationId")]
)
data class HourlyForecastPeriodEntity(
    val locationId: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val temperatureF: Int?,
    val shortForecast: String?,
    val windSpeed: String?,
    val windDirection: String?,
    val probabilityOfPrecipitationPercent: Int?,
    val fetchedAtMillis: Long,
    val expiresAtMillis: Long?
)

@Entity(
    tableName = "weather_alerts",
    primaryKeys = ["id", "locationId"],
    indices = [Index("locationId")]
)
data class WeatherAlertEntity(
    val id: String,
    val locationId: String,
    val event: String,
    val headline: String?,
    val description: String?,
    val instruction: String?,
    val severity: String?,
    val urgency: String?,
    val certainty: String?,
    val areaDescription: String?,
    val effectiveMillis: Long?,
    val onsetMillis: Long?,
    val expiresMillis: Long?,
    val endsMillis: Long?,
    val fetchedAtMillis: Long
)
