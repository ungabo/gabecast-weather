package com.gabecast.weather.domain.model

import java.time.Instant

data class SavedLocation(
    val id: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val state: String?,
    val country: String = "US",
    val nwsGridId: String?,
    val nwsGridX: Int?,
    val nwsGridY: Int?,
    val forecastUrl: String?,
    val hourlyForecastUrl: String?,
    val observationStationsUrl: String?,
    val forecastZoneUrl: String?,
    val countyZoneUrl: String?,
    val nearestStationId: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CurrentConditions(
    val locationId: String,
    val stationId: String?,
    val temperatureF: Double?,
    val conditionText: String?,
    val humidityPercent: Double?,
    val windSpeedMph: Double?,
    val windDirection: String?,
    val pressureInHg: Double?,
    val visibilityMiles: Double?,
    val observedAt: Instant?,
    val fetchedAt: Instant,
    val expiresAt: Instant?
)

data class DailyForecastPeriod(
    val locationId: String,
    val periodNumber: Int,
    val name: String,
    val startTime: Instant,
    val endTime: Instant,
    val isDaytime: Boolean,
    val temperatureF: Int?,
    val windSpeed: String?,
    val windDirection: String?,
    val shortForecast: String?,
    val detailedForecast: String?,
    val probabilityOfPrecipitationPercent: Int?,
    val fetchedAt: Instant,
    val expiresAt: Instant?
)

data class HourlyForecastPeriod(
    val locationId: String,
    val startTime: Instant,
    val endTime: Instant,
    val temperatureF: Int?,
    val shortForecast: String?,
    val windSpeed: String?,
    val windDirection: String?,
    val probabilityOfPrecipitationPercent: Int?,
    val fetchedAt: Instant,
    val expiresAt: Instant?
)

data class WeatherAlert(
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
    val effective: Instant?,
    val onset: Instant?,
    val expires: Instant?,
    val ends: Instant?,
    val fetchedAt: Instant
)

data class WeatherDashboard(
    val location: SavedLocation?,
    val currentConditions: CurrentConditions?,
    val dailyForecast: List<DailyForecastPeriod>,
    val hourlyForecast: List<HourlyForecastPeriod>,
    val alerts: List<WeatherAlert>,
    val fetchedAt: Instant?,
    val isStale: Boolean,
    val refreshMessage: String? = null
)

enum class TemperatureUnit {
    Fahrenheit,
    Celsius
}

enum class WindUnit {
    Mph,
    Kmh
}

enum class ThemeMode {
    System,
    Light,
    Dark
}

data class UserSettings(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.Fahrenheit,
    val windUnit: WindUnit = WindUnit.Mph,
    val themeMode: ThemeMode = ThemeMode.System,
    val nwsContactEmail: String = "",
    val nwsContactEmailConfirmed: Boolean = false,
    val selectedLocationId: String? = null,
    val backgroundRefreshEnabled: Boolean = true
)

data class LocationSearchResult(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val state: String?
)
