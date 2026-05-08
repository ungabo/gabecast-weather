package com.gabecast.weather.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gabecast.weather.data.local.dao.WeatherDao
import com.gabecast.weather.data.local.entity.CurrentConditionsEntity
import com.gabecast.weather.data.local.entity.DailyForecastPeriodEntity
import com.gabecast.weather.data.local.entity.HourlyForecastPeriodEntity
import com.gabecast.weather.data.local.entity.RadarImageCacheEntryEntity
import com.gabecast.weather.data.local.entity.SavedLocationEntity
import com.gabecast.weather.data.local.entity.WeatherAlertEntity

@Database(
    entities = [
        SavedLocationEntity::class,
        CurrentConditionsEntity::class,
        DailyForecastPeriodEntity::class,
        HourlyForecastPeriodEntity::class,
        WeatherAlertEntity::class,
        RadarImageCacheEntryEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}
