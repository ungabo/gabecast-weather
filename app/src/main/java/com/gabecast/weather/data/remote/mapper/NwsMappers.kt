package com.gabecast.weather.data.remote.mapper

import com.gabecast.weather.data.remote.dto.NwsAlertFeature
import com.gabecast.weather.data.remote.dto.NwsForecastPeriod
import com.gabecast.weather.data.remote.dto.NwsObservationProperties
import com.gabecast.weather.data.remote.dto.NwsPointProperties
import com.gabecast.weather.domain.model.CurrentConditions
import com.gabecast.weather.domain.model.DailyForecastPeriod
import com.gabecast.weather.domain.model.HourlyForecastPeriod
import com.gabecast.weather.domain.model.SavedLocation
import com.gabecast.weather.domain.model.WeatherAlert
import com.gabecast.weather.domain.util.celsiusToFahrenheit
import com.gabecast.weather.domain.util.metersPerSecondToMph
import com.gabecast.weather.domain.util.metersToMiles
import com.gabecast.weather.domain.util.pascalsToInHg
import com.gabecast.weather.util.parseInstantOrNull
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt

fun NwsPointProperties.toSavedLocation(
    id: String,
    latitude: Double,
    longitude: Double,
    existing: SavedLocation? = null,
    nearestStationId: String? = existing?.nearestStationId
): SavedLocation {
    val city = relativeLocation?.properties?.city
    val state = relativeLocation?.properties?.state
    val display = listOfNotNull(city, state).joinToString(", ").ifBlank { existing?.displayName ?: "Saved location" }
    val now = Instant.now()
    return SavedLocation(
        id = id,
        displayName = display,
        latitude = latitude,
        longitude = longitude,
        state = state,
        country = "US",
        nwsGridId = gridId,
        nwsGridX = gridX,
        nwsGridY = gridY,
        forecastUrl = forecast,
        hourlyForecastUrl = forecastHourly,
        observationStationsUrl = observationStations,
        forecastZoneUrl = forecastZone,
        countyZoneUrl = county,
        nearestStationId = nearestStationId,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now
    )
}

fun NwsForecastPeriod.toDailyDomain(locationId: String, fetchedAt: Instant, expiresAt: Instant): DailyForecastPeriod? {
    val start = parseInstantOrNull(startTime) ?: return null
    val end = parseInstantOrNull(endTime) ?: return null
    return DailyForecastPeriod(
        locationId = locationId,
        periodNumber = number ?: start.epochSecond.toInt(),
        name = name.orEmpty().ifBlank { "Forecast" },
        startTime = start,
        endTime = end,
        isDaytime = isDaytime ?: true,
        temperatureF = temperature.toFahrenheitInt(temperatureUnit),
        windSpeed = windSpeed,
        windDirection = windDirection,
        shortForecast = shortForecast,
        detailedForecast = detailedForecast,
        probabilityOfPrecipitationPercent = probabilityOfPrecipitation?.value?.roundToInt(),
        fetchedAt = fetchedAt,
        expiresAt = expiresAt
    )
}

fun NwsForecastPeriod.toHourlyDomain(locationId: String, fetchedAt: Instant, expiresAt: Instant): HourlyForecastPeriod? {
    val start = parseInstantOrNull(startTime) ?: return null
    val end = parseInstantOrNull(endTime) ?: return null
    return HourlyForecastPeriod(
        locationId = locationId,
        startTime = start,
        endTime = end,
        temperatureF = temperature.toFahrenheitInt(temperatureUnit),
        shortForecast = shortForecast,
        windSpeed = windSpeed,
        windDirection = windDirection,
        probabilityOfPrecipitationPercent = probabilityOfPrecipitation?.value?.roundToInt(),
        fetchedAt = fetchedAt,
        expiresAt = expiresAt
    )
}

fun NwsObservationProperties.toCurrentDomain(
    locationId: String,
    stationId: String?,
    fetchedAt: Instant,
    expiresAt: Instant
): CurrentConditions = CurrentConditions(
    locationId = locationId,
    stationId = stationId,
    temperatureF = preferredTemperatureF(),
    conditionText = textDescription?.ifBlank { null },
    humidityPercent = relativeHumidity?.value,
    windSpeedMph = windSpeed?.value?.let { value ->
        if (windSpeed.unitCode?.contains("m_s", ignoreCase = true) == true) {
            metersPerSecondToMph(value)
        } else {
            value
        }
    },
    windDirection = windDirection?.value?.let(::degreesToCompass),
    pressureInHg = barometricPressure?.value?.let(::pascalsToInHg),
    visibilityMiles = visibility?.value?.let(::metersToMiles),
    observedAt = parseInstantOrNull(timestamp),
    fetchedAt = fetchedAt,
    expiresAt = expiresAt
)

fun NwsAlertFeature.toDomain(locationId: String, fetchedAt: Instant): WeatherAlert? {
    val props = properties ?: return null
    val alertId = props.id ?: props.atId ?: id ?: return null
    return WeatherAlert(
        id = alertId,
        locationId = locationId,
        event = props.event.orEmpty().ifBlank { "Weather alert" },
        headline = props.headline,
        description = props.description,
        instruction = props.instruction,
        severity = props.severity,
        urgency = props.urgency,
        certainty = props.certainty,
        areaDescription = props.areaDesc,
        effective = parseInstantOrNull(props.effective),
        onset = parseInstantOrNull(props.onset),
        expires = parseInstantOrNull(props.expires),
        ends = parseInstantOrNull(props.ends),
        fetchedAt = fetchedAt
    )
}

private fun Int?.toFahrenheitInt(unit: String?): Int? {
    val value = this ?: return null
    return when (unit?.uppercase(Locale.US)) {
        "C", "CELSIUS" -> celsiusToFahrenheit(value.toDouble()).roundToInt()
        else -> value
    }
}

private fun NwsObservationProperties.preferredTemperatureF(): Double? {
    val preferred = heatIndex?.value ?: windChill?.value ?: temperature?.value ?: return null
    val unit = heatIndex?.unitCode ?: windChill?.unitCode ?: temperature?.unitCode
    return if (unit?.contains("degC", ignoreCase = true) == true) {
        celsiusToFahrenheit(preferred)
    } else {
        preferred
    }
}

private fun degreesToCompass(degrees: Double): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((degrees / 45.0) + 0.5).toInt() % 8
    return directions[index]
}
