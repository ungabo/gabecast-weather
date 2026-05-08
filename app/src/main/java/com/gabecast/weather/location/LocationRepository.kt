package com.gabecast.weather.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.gabecast.weather.domain.model.LocationSearchResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationRepository(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        return coarse == PackageManager.PERMISSION_GRANTED || fine == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentCoordinates(): Pair<Double, Double>? {
        if (!hasLocationPermission()) return null
        val last = runCatching { fusedLocationClient.lastLocation.await() }.getOrNull()
        if (last != null) return last.latitude to last.longitude
        val token = CancellationTokenSource()
        val current = runCatching {
            fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
                .await()
        }.getOrNull()
        return current?.let { it.latitude to it.longitude }
    }

    suspend fun search(query: String): List<LocationSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        val records = loadZipRecords()
        val exactZip = ZIP_PATTERN.matchEntire(trimmed)?.groupValues?.getOrNull(1)
        if (exactZip != null) {
            val zipResults = records
                .filter { it.zip == exactZip }
                .distinctBy { "${it.city},${it.state}" }
                .map { it.toSearchResult() }
            return@withContext zipResults
        }

        val localResults = searchLocalPlaces(trimmed, records)
        if (localResults.isNotEmpty()) return@withContext localResults

        val geocoder = Geocoder(context, Locale.US)
        @Suppress("DEPRECATION")
        val results = runCatching { geocoder.getFromLocationName(trimmed, 8).orEmpty() }.getOrDefault(emptyList())
        results
            .filter { it.latitude in 15.0..72.0 && it.longitude in -180.0..-60.0 }
            .distinctBy { "${it.latitude.formatKey()},${it.longitude.formatKey()}" }
            .map { address ->
                val city = address.locality ?: address.subAdminArea ?: address.featureName
                val state = address.adminArea
                val postal = address.postalCode
                val displayName = listOfNotNull(city, state, postal)
                    .distinct()
                    .joinToString(", ")
                    .ifBlank { address.getAddressLine(0) ?: trimmed }
                LocationSearchResult(
                    displayName = displayName,
                    latitude = address.latitude,
                    longitude = address.longitude,
                    state = state
                )
            }
    }

    private fun Double.formatKey(): String = "%.3f".format(Locale.US, this)

    private fun searchLocalPlaces(query: String, records: List<ZipRecord>): List<LocationSearchResult> {
        val normalized = query.lowercase(Locale.US)
            .replace(",", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.length < 3 || normalized.any { it.isDigit() }) return emptyList()
        val tokens = normalized.split(" ")
        val possibleState = tokens.lastOrNull()?.uppercase(Locale.US)?.takeIf { it.length == 2 }
        val cityQuery = if (possibleState != null) tokens.dropLast(1).joinToString(" ") else normalized
        if (cityQuery.length < 3) return emptyList()
        return records
            .asSequence()
            .filter { possibleState == null || it.state == possibleState }
            .filter { it.city.lowercase(Locale.US).startsWith(cityQuery) }
            .distinctBy { "${it.city},${it.state}" }
            .take(8)
            .map { it.toSearchResult() }
            .toList()
    }

    private fun loadZipRecords(): List<ZipRecord> {
        cachedZipRecords?.let { return it }
        return synchronized(LocationRepository::class.java) {
            cachedZipRecords?.let { return@synchronized it }
            val loaded = context.assets.open("us_zipcodes.tsv").bufferedReader().useLines { lines ->
                lines.drop(1).mapNotNull { line ->
                    val columns = line.split('\t')
                    if (columns.size < 5) return@mapNotNull null
                    val lat = columns[3].toDoubleOrNull() ?: return@mapNotNull null
                    val lon = columns[4].toDoubleOrNull() ?: return@mapNotNull null
                    ZipRecord(
                        zip = columns[0],
                        city = columns[1],
                        state = columns[2],
                        latitude = lat,
                        longitude = lon
                    )
                }.toList()
            }
            cachedZipRecords = loaded
            loaded
        }
    }

    private fun ZipRecord.toSearchResult(): LocationSearchResult = LocationSearchResult(
        displayName = "$city, $state $zip",
        latitude = latitude,
        longitude = longitude,
        state = state
    )

    private companion object {
        val ZIP_PATTERN = Regex("^\\s*(\\d{5})(?:-\\d{4})?\\s*$")
        @Volatile var cachedZipRecords: List<ZipRecord>? = null
    }
}

private data class ZipRecord(
    val zip: String,
    val city: String,
    val state: String,
    val latitude: Double,
    val longitude: Double
)
