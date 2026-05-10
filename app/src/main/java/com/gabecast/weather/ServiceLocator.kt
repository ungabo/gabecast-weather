package com.gabecast.weather

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gabecast.weather.data.local.WeatherDatabase
import com.gabecast.weather.data.remote.createNwsApi
import com.gabecast.weather.data.repository.WeatherRepository
import com.gabecast.weather.location.LocationRepository
import com.gabecast.weather.radar.RadarImageService
import com.gabecast.weather.radar.RadarRepository
import com.gabecast.weather.settings.UserSettingsRepository

object ServiceLocator {
    private lateinit var appContext: Context

    val database: WeatherDatabase by lazy {
        Room.databaseBuilder(appContext, WeatherDatabase::class.java, "gabecast.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    val settingsRepository: UserSettingsRepository by lazy {
        UserSettingsRepository(appContext)
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepository(appContext)
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(
            dao = database.weatherDao(),
            api = createNwsApi(appContext)
        )
    }

    val radarRepository: RadarRepository by lazy {
        RadarRepository(
            context = appContext,
            dao = database.weatherDao(),
            radarImageService = RadarImageService(appContext)
        )
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun applicationContext(): Context = appContext

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `radar_image_cache` (
                    `locationId` TEXT NOT NULL,
                    `latitude` REAL NOT NULL,
                    `longitude` REAL NOT NULL,
                    `radiusMiles` INTEGER NOT NULL,
                    `imageWidth` INTEGER NOT NULL,
                    `imageHeight` INTEGER NOT NULL,
                    `minLat` REAL NOT NULL,
                    `minLon` REAL NOT NULL,
                    `maxLat` REAL NOT NULL,
                    `maxLon` REAL NOT NULL,
                    `imageFilePath` TEXT NOT NULL,
                    `radarTimestampMillis` INTEGER,
                    `fetchedAtMillis` INTEGER NOT NULL,
                    `expiresAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`locationId`, `radiusMiles`, `imageWidth`, `imageHeight`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_radar_image_cache_locationId` ON `radar_image_cache` (`locationId`)")
        }
    }
}
