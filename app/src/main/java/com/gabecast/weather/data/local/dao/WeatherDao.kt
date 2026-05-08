package com.gabecast.weather.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gabecast.weather.data.local.entity.CurrentConditionsEntity
import com.gabecast.weather.data.local.entity.DailyForecastPeriodEntity
import com.gabecast.weather.data.local.entity.HourlyForecastPeriodEntity
import com.gabecast.weather.data.local.entity.RadarImageCacheEntryEntity
import com.gabecast.weather.data.local.entity.SavedLocationEntity
import com.gabecast.weather.data.local.entity.WeatherAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Query("SELECT * FROM saved_locations ORDER BY createdAtMillis ASC")
    fun observeLocations(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations ORDER BY createdAtMillis ASC")
    suspend fun getLocations(): List<SavedLocationEntity>

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    suspend fun getLocation(id: String): SavedLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(location: SavedLocationEntity)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocation(id: String)

    @Query("SELECT * FROM current_conditions WHERE locationId = :locationId")
    suspend fun getCurrentConditions(locationId: String): CurrentConditionsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrentConditions(entity: CurrentConditionsEntity)

    @Query("SELECT * FROM daily_forecast_periods WHERE locationId = :locationId ORDER BY periodNumber ASC")
    suspend fun getDailyForecast(locationId: String): List<DailyForecastPeriodEntity>

    @Query("DELETE FROM daily_forecast_periods WHERE locationId = :locationId")
    suspend fun clearDailyForecast(locationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyForecast(periods: List<DailyForecastPeriodEntity>)

    @Transaction
    suspend fun replaceDailyForecast(locationId: String, periods: List<DailyForecastPeriodEntity>) {
        clearDailyForecast(locationId)
        insertDailyForecast(periods)
    }

    @Query("SELECT * FROM hourly_forecast_periods WHERE locationId = :locationId ORDER BY startTimeMillis ASC")
    suspend fun getHourlyForecast(locationId: String): List<HourlyForecastPeriodEntity>

    @Query("DELETE FROM hourly_forecast_periods WHERE locationId = :locationId")
    suspend fun clearHourlyForecast(locationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyForecast(periods: List<HourlyForecastPeriodEntity>)

    @Transaction
    suspend fun replaceHourlyForecast(locationId: String, periods: List<HourlyForecastPeriodEntity>) {
        clearHourlyForecast(locationId)
        insertHourlyForecast(periods)
    }

    @Query("SELECT * FROM weather_alerts WHERE locationId = :locationId ORDER BY COALESCE(onsetMillis, effectiveMillis, fetchedAtMillis) DESC")
    suspend fun getAlerts(locationId: String): List<WeatherAlertEntity>

    @Query("DELETE FROM weather_alerts WHERE locationId = :locationId")
    suspend fun clearAlerts(locationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<WeatherAlertEntity>)

    @Transaction
    suspend fun replaceAlerts(locationId: String, alerts: List<WeatherAlertEntity>) {
        clearAlerts(locationId)
        if (alerts.isNotEmpty()) insertAlerts(alerts)
    }

    @Query("DELETE FROM weather_alerts WHERE expiresMillis IS NOT NULL AND expiresMillis < :olderThanMillis")
    suspend fun deleteOldAlerts(olderThanMillis: Long)

    @Query("DELETE FROM hourly_forecast_periods WHERE endTimeMillis < :olderThanMillis")
    suspend fun deleteOldHourly(olderThanMillis: Long)

    @Query(
        "SELECT * FROM radar_image_cache WHERE locationId = :locationId AND radiusMiles = :radiusMiles AND imageWidth = :imageWidth AND imageHeight = :imageHeight"
    )
    suspend fun getRadarImage(
        locationId: String,
        radiusMiles: Int,
        imageWidth: Int,
        imageHeight: Int
    ): RadarImageCacheEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRadarImage(entry: RadarImageCacheEntryEntity)

    @Query("SELECT imageFilePath FROM radar_image_cache WHERE fetchedAtMillis < :olderThanMillis")
    suspend fun getOldRadarImagePaths(olderThanMillis: Long): List<String>

    @Query("DELETE FROM radar_image_cache WHERE fetchedAtMillis < :olderThanMillis")
    suspend fun deleteOldRadarImages(olderThanMillis: Long)
}
