package com.gabecast.weather.data.remote.dto

import com.squareup.moshi.Json

data class NwsPointResponse(
    val properties: NwsPointProperties?
)

data class NwsPointProperties(
    val gridId: String?,
    val gridX: Int?,
    val gridY: Int?,
    val forecast: String?,
    val forecastHourly: String?,
    val observationStations: String?,
    val forecastZone: String?,
    val county: String?,
    val relativeLocation: NwsRelativeLocation?
)

data class NwsRelativeLocation(
    val properties: NwsRelativeLocationProperties?
)

data class NwsRelativeLocationProperties(
    val city: String?,
    val state: String?
)

data class NwsForecastResponse(
    val properties: NwsForecastProperties?
)

data class NwsForecastProperties(
    val updated: String?,
    val generatedAt: String?,
    val periods: List<NwsForecastPeriod>?
)

data class NwsForecastPeriod(
    val number: Int?,
    val name: String?,
    val startTime: String?,
    val endTime: String?,
    val isDaytime: Boolean?,
    val temperature: Int?,
    val temperatureUnit: String?,
    val windSpeed: String?,
    val windDirection: String?,
    val shortForecast: String?,
    val detailedForecast: String?,
    val probabilityOfPrecipitation: NwsQuantitativeValue?
)

data class NwsStationsResponse(
    val features: List<NwsStationFeature>?
)

data class NwsStationFeature(
    val properties: NwsStationProperties?
)

data class NwsStationProperties(
    val stationIdentifier: String?,
    val name: String?
)

data class NwsObservationResponse(
    val properties: NwsObservationProperties?
)

data class NwsObservationProperties(
    val timestamp: String?,
    val textDescription: String?,
    val temperature: NwsQuantitativeValue?,
    val windChill: NwsQuantitativeValue?,
    val heatIndex: NwsQuantitativeValue?,
    val relativeHumidity: NwsQuantitativeValue?,
    val windSpeed: NwsQuantitativeValue?,
    val windDirection: NwsQuantitativeValue?,
    val barometricPressure: NwsQuantitativeValue?,
    val visibility: NwsQuantitativeValue?
)

data class NwsQuantitativeValue(
    val value: Double?,
    val unitCode: String?
)

data class NwsAlertsResponse(
    val features: List<NwsAlertFeature>?
)

data class NwsAlertFeature(
    val id: String?,
    val properties: NwsAlertProperties?
)

data class NwsAlertProperties(
    @Json(name = "@id") val atId: String?,
    val id: String?,
    val event: String?,
    val headline: String?,
    val description: String?,
    val instruction: String?,
    val severity: String?,
    val urgency: String?,
    val certainty: String?,
    val areaDesc: String?,
    val effective: String?,
    val onset: String?,
    val expires: String?,
    val ends: String?
)
