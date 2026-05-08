package com.gabecast.weather.data.local.mapper

import com.gabecast.weather.data.local.entity.CurrentConditionsEntity
import com.gabecast.weather.data.local.entity.DailyForecastPeriodEntity
import com.gabecast.weather.data.local.entity.HourlyForecastPeriodEntity
import com.gabecast.weather.data.local.entity.SavedLocationEntity
import com.gabecast.weather.data.local.entity.WeatherAlertEntity
import com.gabecast.weather.domain.model.CurrentConditions
import com.gabecast.weather.domain.model.DailyForecastPeriod
import com.gabecast.weather.domain.model.HourlyForecastPeriod
import com.gabecast.weather.domain.model.SavedLocation
import com.gabecast.weather.domain.model.WeatherAlert
import com.gabecast.weather.util.toEpochMillisOrNull
import com.gabecast.weather.util.toInstantOrNull
import java.time.Instant

fun SavedLocationEntity.toDomain(): SavedLocation = SavedLocation(
    id = id,
    displayName = displayName,
    latitude = latitude,
    longitude = longitude,
    state = state,
    country = country,
    nwsGridId = nwsGridId,
    nwsGridX = nwsGridX,
    nwsGridY = nwsGridY,
    forecastUrl = forecastUrl,
    hourlyForecastUrl = hourlyForecastUrl,
    observationStationsUrl = observationStationsUrl,
    forecastZoneUrl = forecastZoneUrl,
    countyZoneUrl = countyZoneUrl,
    nearestStationId = nearestStationId,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtMillis)
)

fun SavedLocation.toEntity(): SavedLocationEntity = SavedLocationEntity(
    id = id,
    displayName = displayName,
    latitude = latitude,
    longitude = longitude,
    state = state,
    country = country,
    nwsGridId = nwsGridId,
    nwsGridX = nwsGridX,
    nwsGridY = nwsGridY,
    forecastUrl = forecastUrl,
    hourlyForecastUrl = hourlyForecastUrl,
    observationStationsUrl = observationStationsUrl,
    forecastZoneUrl = forecastZoneUrl,
    countyZoneUrl = countyZoneUrl,
    nearestStationId = nearestStationId,
    createdAtMillis = createdAt.toEpochMilli(),
    updatedAtMillis = updatedAt.toEpochMilli()
)

fun CurrentConditionsEntity.toDomain(): CurrentConditions = CurrentConditions(
    locationId = locationId,
    stationId = stationId,
    temperatureF = temperatureF,
    conditionText = conditionText,
    humidityPercent = humidityPercent,
    windSpeedMph = windSpeedMph,
    windDirection = windDirection,
    pressureInHg = pressureInHg,
    visibilityMiles = visibilityMiles,
    observedAt = observedAtMillis.toInstantOrNull(),
    fetchedAt = Instant.ofEpochMilli(fetchedAtMillis),
    expiresAt = expiresAtMillis.toInstantOrNull()
)

fun CurrentConditions.toEntity(): CurrentConditionsEntity = CurrentConditionsEntity(
    locationId = locationId,
    stationId = stationId,
    temperatureF = temperatureF,
    conditionText = conditionText,
    humidityPercent = humidityPercent,
    windSpeedMph = windSpeedMph,
    windDirection = windDirection,
    pressureInHg = pressureInHg,
    visibilityMiles = visibilityMiles,
    observedAtMillis = observedAt.toEpochMillisOrNull(),
    fetchedAtMillis = fetchedAt.toEpochMilli(),
    expiresAtMillis = expiresAt.toEpochMillisOrNull()
)

fun DailyForecastPeriodEntity.toDomain(): DailyForecastPeriod = DailyForecastPeriod(
    locationId = locationId,
    periodNumber = periodNumber,
    name = name,
    startTime = Instant.ofEpochMilli(startTimeMillis),
    endTime = Instant.ofEpochMilli(endTimeMillis),
    isDaytime = isDaytime,
    temperatureF = temperatureF,
    windSpeed = windSpeed,
    windDirection = windDirection,
    shortForecast = shortForecast,
    detailedForecast = detailedForecast,
    probabilityOfPrecipitationPercent = probabilityOfPrecipitationPercent,
    fetchedAt = Instant.ofEpochMilli(fetchedAtMillis),
    expiresAt = expiresAtMillis.toInstantOrNull()
)

fun DailyForecastPeriod.toEntity(): DailyForecastPeriodEntity = DailyForecastPeriodEntity(
    locationId = locationId,
    periodNumber = periodNumber,
    name = name,
    startTimeMillis = startTime.toEpochMilli(),
    endTimeMillis = endTime.toEpochMilli(),
    isDaytime = isDaytime,
    temperatureF = temperatureF,
    windSpeed = windSpeed,
    windDirection = windDirection,
    shortForecast = shortForecast,
    detailedForecast = detailedForecast,
    probabilityOfPrecipitationPercent = probabilityOfPrecipitationPercent,
    fetchedAtMillis = fetchedAt.toEpochMilli(),
    expiresAtMillis = expiresAt.toEpochMillisOrNull()
)

fun HourlyForecastPeriodEntity.toDomain(): HourlyForecastPeriod = HourlyForecastPeriod(
    locationId = locationId,
    startTime = Instant.ofEpochMilli(startTimeMillis),
    endTime = Instant.ofEpochMilli(endTimeMillis),
    temperatureF = temperatureF,
    shortForecast = shortForecast,
    windSpeed = windSpeed,
    windDirection = windDirection,
    probabilityOfPrecipitationPercent = probabilityOfPrecipitationPercent,
    fetchedAt = Instant.ofEpochMilli(fetchedAtMillis),
    expiresAt = expiresAtMillis.toInstantOrNull()
)

fun HourlyForecastPeriod.toEntity(): HourlyForecastPeriodEntity = HourlyForecastPeriodEntity(
    locationId = locationId,
    startTimeMillis = startTime.toEpochMilli(),
    endTimeMillis = endTime.toEpochMilli(),
    temperatureF = temperatureF,
    shortForecast = shortForecast,
    windSpeed = windSpeed,
    windDirection = windDirection,
    probabilityOfPrecipitationPercent = probabilityOfPrecipitationPercent,
    fetchedAtMillis = fetchedAt.toEpochMilli(),
    expiresAtMillis = expiresAt.toEpochMillisOrNull()
)

fun WeatherAlertEntity.toDomain(): WeatherAlert = WeatherAlert(
    id = id,
    locationId = locationId,
    event = event,
    headline = headline,
    description = description,
    instruction = instruction,
    severity = severity,
    urgency = urgency,
    certainty = certainty,
    areaDescription = areaDescription,
    effective = effectiveMillis.toInstantOrNull(),
    onset = onsetMillis.toInstantOrNull(),
    expires = expiresMillis.toInstantOrNull(),
    ends = endsMillis.toInstantOrNull(),
    fetchedAt = Instant.ofEpochMilli(fetchedAtMillis)
)

fun WeatherAlert.toEntity(): WeatherAlertEntity = WeatherAlertEntity(
    id = id,
    locationId = locationId,
    event = event,
    headline = headline,
    description = description,
    instruction = instruction,
    severity = severity,
    urgency = urgency,
    certainty = certainty,
    areaDescription = areaDescription,
    effectiveMillis = effective.toEpochMillisOrNull(),
    onsetMillis = onset.toEpochMillisOrNull(),
    expiresMillis = expires.toEpochMillisOrNull(),
    endsMillis = ends.toEpochMillisOrNull(),
    fetchedAtMillis = fetchedAt.toEpochMilli()
)
