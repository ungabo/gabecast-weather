package com.gabecast.weather.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gabecast.weather.domain.model.TemperatureUnit
import com.gabecast.weather.domain.model.ThemeMode
import com.gabecast.weather.domain.model.UserSettings
import com.gabecast.weather.domain.model.WindUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserSettingsRepository(context: Context) {
    private val store = context.settingsStore
    private val requestPrefs = context.getSharedPreferences("request_contact", Context.MODE_PRIVATE)

    val settings: Flow<UserSettings> = store.data.map { prefs ->
        val confirmed = prefs[NWS_CONTACT_CONFIRMED] ?: false
        val email = if (confirmed) prefs[NWS_CONTACT_EMAIL].orEmpty() else ""
        requestPrefs.edit()
            .putString(CONTACT_PREF_KEY, email)
            .putBoolean(CONTACT_CONFIRMED_PREF_KEY, confirmed && email.isUsableContactEmail())
            .apply()
        UserSettings(
            temperatureUnit = prefs[TEMP_UNIT].toEnumOrDefault(TemperatureUnit.Fahrenheit),
            windUnit = prefs[WIND_UNIT].toEnumOrDefault(WindUnit.Mph),
            themeMode = prefs[THEME_MODE].toEnumOrDefault(ThemeMode.System),
            nwsContactEmail = email,
            nwsContactEmailConfirmed = confirmed && email.isUsableContactEmail(),
            selectedLocationId = prefs[SELECTED_LOCATION],
            backgroundRefreshEnabled = prefs[BACKGROUND_REFRESH] ?: true
        )
    }

    init {
        requestPrefs.edit()
            .remove(CONTACT_PREF_KEY)
            .putBoolean(CONTACT_CONFIRMED_PREF_KEY, false)
            .apply()
    }

    suspend fun setSelectedLocation(locationId: String?) {
        store.edit { prefs ->
            if (locationId == null) prefs.remove(SELECTED_LOCATION) else prefs[SELECTED_LOCATION] = locationId
        }
    }

    suspend fun settingsSnapshot(): UserSettings = settings.first()

    suspend fun settingsSnapshotSelectedId(): String? = settingsSnapshot().selectedLocationId

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        store.edit { it[TEMP_UNIT] = unit.name }
    }

    suspend fun setWindUnit(unit: WindUnit) {
        store.edit { it[WIND_UNIT] = unit.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setNwsContactEmail(email: String) {
        val sanitized = email.trim()
        val confirmed = sanitized.isUsableContactEmail()
        store.edit { prefs ->
            prefs[NWS_CONTACT_EMAIL] = if (confirmed) sanitized else ""
            prefs[NWS_CONTACT_CONFIRMED] = confirmed
        }
        requestPrefs.edit()
            .putString(CONTACT_PREF_KEY, if (confirmed) sanitized else "")
            .putBoolean(CONTACT_CONFIRMED_PREF_KEY, confirmed)
            .apply()
    }

    suspend fun setBackgroundRefreshEnabled(enabled: Boolean) {
        store.edit { it[BACKGROUND_REFRESH] = enabled }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T {
        return this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default
    }

    companion object {
        val TEMP_UNIT = stringPreferencesKey("temperature_unit")
        val WIND_UNIT = stringPreferencesKey("wind_unit")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        const val CONTACT_PREF_KEY = "nws_contact_email"
        const val CONTACT_CONFIRMED_PREF_KEY = "nws_contact_confirmed"
        val NWS_CONTACT_EMAIL = stringPreferencesKey(CONTACT_PREF_KEY)
        val NWS_CONTACT_CONFIRMED = booleanPreferencesKey(CONTACT_CONFIRMED_PREF_KEY)
        val SELECTED_LOCATION = stringPreferencesKey("selected_location")
        val BACKGROUND_REFRESH = booleanPreferencesKey("background_refresh")
    }
}

fun String.isUsableContactEmail(): Boolean {
    val trimmed = trim()
    val at = trimmed.indexOf('@')
    val dot = trimmed.lastIndexOf('.')
    return at > 0 && dot > at + 1 && dot < trimmed.lastIndex - 1 && !trimmed.contains(' ')
}
