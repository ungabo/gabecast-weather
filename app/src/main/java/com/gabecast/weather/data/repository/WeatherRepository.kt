package com.gabecast.weather.data.repository

import com.gabecast.weather.data.local.dao.WeatherDao
import com.gabecast.weather.data.local.mapper.toDomain
import com.gabecast.weather.data.local.mapper.toEntity
import com.gabecast.weather.data.remote.NwsApi
import com.gabecast.weather.data.remote.NwsFallbackTtl
import com.gabecast.weather.data.remote.expiresAtOrFallback
import com.gabecast.weather.data.remote.mapper.toCurrentDomain
import com.gabecast.weather.data.remote.mapper.toDailyDomain
import com.gabecast.weather.data.remote.mapper.toDomain
import com.gabecast.weather.data.remote.mapper.toHourlyDomain
import com.gabecast.weather.data.remote.mapper.toSavedLocation
import com.gabecast.weather.domain.model.CurrentConditions
import com.gabecast.weather.domain.model.DailyForecastPeriod
import com.gabecast.weather.domain.model.HourlyForecastPeriod
import com.gabecast.weather.domain.model.SavedLocation
import com.gabecast.weather.domain.model.WeatherAlert
import com.gabecast.weather.domain.model.WeatherDashboard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.UUID

class WeatherRepository(
    private val dao: WeatherDao,
    private val api: NwsApi
) {
    private val refreshMutex = Mutex()

    fun observeLocations(): Flow<List<SavedLocation>> = dao.observeLocations().map { rows ->
        rows.map { it.toDomain() }
    }

    suspend fun getLocations(): List<SavedLocation> = dao.getLocations().map { it.toDomain() }

    suspend fun getLocation(id: String): SavedLocation? = dao.getLocation(id)?.toDomain()

    suspend fun deleteLocation(id: String) {
        dao.deleteLocation(id)
    }

    suspend fun addOrUpdateLocation(
        latitude: Double,
        longitude: Double,
        preferredName: String? = null,
        preferredState: String? = null
    ): SavedLocation {
        val id = stableLocationId(latitude, longitude)
        val existing = dao.getLocation(id)?.toDomain()
        val pointResponse = api.getPoint(latitude.coordinate(), longitude.coordinate())
        if (pointResponse.code() == 404) throw UnsupportedLocationException()
        val point = pointResponse.requireBody().properties ?: throw IOException("Point metadata missing")
        val location = point.toSavedLocation(id, latitude, longitude, existing)
        val namedLocation = if (!preferredName.isNullOrBlank()) {
            location.copy(displayName = preferredName, state = preferredState ?: location.state)
        } else {
            location
        }
        dao.upsertLocation(namedLocation.toEntity())
        return namedLocation
    }

    suspend fun loadCachedDashboard(locationId: String): WeatherDashboard {
        val location = dao.getLocation(locationId)?.toDomain()
        val current = dao.getCurrentConditions(locationId)?.toDomain()
        val daily = dao.getDailyForecast(locationId).map { it.toDomain() }
        val hourly = dao.getHourlyForecast(locationId).map { it.toDomain() }
        val alerts = dao.getAlerts(locationId).map { it.toDomain() }
        return WeatherDashboard(
            location = location,
            currentConditions = current,
            dailyForecast = daily,
            hourlyForecast = hourly,
            alerts = alerts,
            fetchedAt = listOfNotNull(
                current?.fetchedAt,
                daily.maxOfOrNull { it.fetchedAt },
                hourly.maxOfOrNull { it.fetchedAt },
                alerts.maxOfOrNull { it.fetchedAt }
            ).maxOrNull(),
            isStale = listOfNotNull(
                current?.expiresAt,
                daily.minOfOrNull { it.expiresAt ?: Instant.EPOCH },
                hourly.minOfOrNull { it.expiresAt ?: Instant.EPOCH }
            ).any { it.isBefore(Instant.now()) }
        )
    }

    suspend fun loadDashboardForDisplay(locationId: String, forceRefresh: Boolean = false): WeatherDashboard {
        val cached = loadCachedDashboard(locationId)
        return if (forceRefresh || shouldRefreshForDisplay(cached, Instant.now())) {
            refreshDashboard(locationId, forceRefresh = forceRefresh)
        } else {
            cached
        }
    }

    suspend fun refreshDashboard(locationId: String, forceRefresh: Boolean = false): WeatherDashboard {
        return refreshMutex.withLock {
            val location = dao.getLocation(locationId)?.toDomain() ?: throw IOException("Location missing")
            val now = Instant.now()
            val metadataLocation = if (forceRefresh || location.updatedAt.plusSeconds(7 * 24 * 60 * 60).isBefore(now)) {
                refreshLocationMetadata(location)
            } else {
                location
            }

            val cached = loadCachedDashboard(metadataLocation.id)
            val dailyExpired = cached.dailyForecast.firstOrNull()?.expiresAt?.isBefore(now) ?: true
            val hourlyExpired = cached.hourlyForecast.firstOrNull()?.expiresAt?.isBefore(now) ?: true
            val currentExpired = cached.currentConditions
                ?.fetchedAt
                ?.plus(CURRENT_CONDITIONS_MAX_AGE)
                ?.isBefore(now)
                ?: true
            val alertsExpired = cached.alerts.maxOfOrNull { it.fetchedAt }?.plusSeconds(300)?.isBefore(now) ?: true

            runCatching {
                if (forceRefresh || dailyExpired) refreshDailyForecast(metadataLocation)
                if (forceRefresh || hourlyExpired) refreshHourlyForecast(metadataLocation)
                if (forceRefresh || currentExpired) refreshCurrentConditions(metadataLocation)
                if (forceRefresh || alertsExpired) refreshAlerts(metadataLocation)
                cleanup()
            }.getOrElse { throwable ->
                val stale = loadCachedDashboard(metadataLocation.id)
                if (stale.dailyForecast.isNotEmpty() || stale.currentConditions != null || stale.hourlyForecast.isNotEmpty()) {
                    return@withLock stale.copy(
                        isStale = true,
                        refreshMessage = throwable.userFriendlyMessage()
                    )
                }
                throw throwable
            }
            loadCachedDashboard(metadataLocation.id)
        }
    }

    suspend fun refreshLocationMetadata(location: SavedLocation): SavedLocation {
        val response = api.getPoint(location.latitude.coordinate(), location.longitude.coordinate())
        if (response.code() == 404) throw UnsupportedLocationException()
        response.expiresAtOrFallback(NwsFallbackTtl.Point)
        val point = response.requireBody().properties ?: throw IOException("Point metadata missing")
        val updated = point.toSavedLocation(
            id = location.id,
            latitude = location.latitude,
            longitude = location.longitude,
            existing = location,
            nearestStationId = location.nearestStationId
        )
        dao.upsertLocation(updated.toEntity())
        return updated
    }

    private suspend fun refreshDailyForecast(location: SavedLocation): List<DailyForecastPeriod> {
        val url = location.forecastUrl ?: throw IOException("Forecast URL missing")
        val response = api.getForecast(url)
        val fetchedAt = Instant.now()
        val expiresAt = response.expiresAtOrFallback(NwsFallbackTtl.Daily, fetchedAt)
        val periods = response.requireBody().properties?.periods.orEmpty()
            .mapNotNull { it.toDailyDomain(location.id, fetchedAt, expiresAt) }
        dao.replaceDailyForecast(location.id, periods.map { it.toEntity() })
        return periods
    }

    private suspend fun refreshHourlyForecast(location: SavedLocation): List<HourlyForecastPeriod> {
        val url = location.hourlyForecastUrl ?: throw IOException("Hourly forecast URL missing")
        val response = api.getHourlyForecast(url)
        val fetchedAt = Instant.now()
        val expiresAt = response.expiresAtOrFallback(NwsFallbackTtl.Hourly, fetchedAt)
        val periods = response.requireBody().properties?.periods.orEmpty()
            .mapNotNull { it.toHourlyDomain(location.id, fetchedAt, expiresAt) }
        dao.replaceHourlyForecast(location.id, periods.map { it.toEntity() })
        return periods
    }

    private suspend fun refreshCurrentConditions(location: SavedLocation): CurrentConditions? {
        val stationCandidates = buildList {
            location.nearestStationId?.let(::add)
            addAll(fetchStationIds(location))
        }.distinct().take(6)
        var newestLocation = location
        for (stationId in stationCandidates) {
            val response = runCatching { api.getLatestObservation(stationId) }.getOrNull() ?: continue
            if (!response.isSuccessful) continue
            val fetchedAt = Instant.now()
            val expiresAt = response.expiresAtOrFallback(NwsFallbackTtl.Current, fetchedAt)
            val props = response.body()?.properties ?: continue
            val current = props.toCurrentDomain(location.id, stationId, fetchedAt, expiresAt)
            if (current.temperatureF != null || !current.conditionText.isNullOrBlank()) {
                dao.upsertCurrentConditions(current.toEntity())
                if (location.nearestStationId != stationId) {
                    newestLocation = location.copy(nearestStationId = stationId, updatedAt = Instant.now())
                    dao.upsertLocation(newestLocation.toEntity())
                }
                return current
            }
        }
        if (newestLocation.nearestStationId != location.nearestStationId) dao.upsertLocation(newestLocation.toEntity())
        return dao.getCurrentConditions(location.id)?.toDomain()
    }

    private suspend fun refreshAlerts(location: SavedLocation): List<WeatherAlert> {
        val point = "${location.latitude.coordinate()},${location.longitude.coordinate()}"
        val response = api.getActiveAlertsByPoint(point)
        response.expiresAtOrFallback(NwsFallbackTtl.Alerts)
        val fetchedAt = Instant.now()
        val alerts = response.requireBody().features.orEmpty()
            .mapNotNull { it.toDomain(location.id, fetchedAt) }
        dao.replaceAlerts(location.id, alerts.map { it.toEntity() })
        return alerts
    }

    private suspend fun fetchStationIds(location: SavedLocation): List<String> {
        val url = location.observationStationsUrl ?: return emptyList()
        val response = api.getObservationStations(url)
        response.expiresAtOrFallback(NwsFallbackTtl.Stations)
        return response.requireBody().features.orEmpty().mapNotNull { it.properties?.stationIdentifier }
    }

    private suspend fun cleanup() {
        val now = Instant.now().toEpochMilli()
        dao.deleteOldAlerts(now - 24L * 60L * 60L * 1000L)
        dao.deleteOldHourly(now - 6L * 60L * 60L * 1000L)
    }

    private fun stableLocationId(latitude: Double, longitude: Double): String {
        val key = "${latitude.coordinate()},${longitude.coordinate()}"
        return UUID.nameUUIDFromBytes(key.toByteArray()).toString()
    }

    private fun shouldRefreshForDisplay(dashboard: WeatherDashboard, now: Instant): Boolean {
        if (dashboard.location == null) return true
        if (dashboard.currentConditions == null) return true
        if (dashboard.dailyForecast.isEmpty() || dashboard.hourlyForecast.isEmpty()) return true
        if (dashboard.currentConditions.fetchedAt.plus(CURRENT_CONDITIONS_MAX_AGE).isBefore(now)) return true
        if (dashboard.dailyForecast.firstOrNull()?.expiresAt?.isBefore(now) == true) return true
        if (dashboard.hourlyForecast.firstOrNull()?.expiresAt?.isBefore(now) == true) return true
        return false
    }

    private fun Double.coordinate(): String = "%.4f".format(Locale.US, this)

    private fun Throwable.userFriendlyMessage(): String = when (this) {
        is UnsupportedLocationException -> "This app currently supports U.S. locations only."
        else -> "Could not refresh. Showing last available forecast."
    }

    private fun <T> Response<T>.requireBody(): T {
        if (!isSuccessful) {
            if (code() == 404) throw UnsupportedLocationException()
            throw IOException("NWS request failed with HTTP ${code()}: ${errorBody()?.string()?.take(160).orEmpty()}")
        }
        return body() ?: throw IOException("NWS response was empty")
    }

    companion object {
        val CURRENT_CONDITIONS_MAX_AGE: Duration = Duration.ofMinutes(20)
    }
}

class UnsupportedLocationException : IOException("This app currently supports U.S. locations only.")
