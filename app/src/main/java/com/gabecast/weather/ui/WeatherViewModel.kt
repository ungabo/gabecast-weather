package com.gabecast.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gabecast.weather.ServiceLocator
import com.gabecast.weather.data.repository.UnsupportedLocationException
import com.gabecast.weather.data.repository.WeatherRepository
import com.gabecast.weather.domain.model.LocationSearchResult
import com.gabecast.weather.domain.model.SavedLocation
import com.gabecast.weather.domain.model.TemperatureUnit
import com.gabecast.weather.domain.model.ThemeMode
import com.gabecast.weather.domain.model.UserSettings
import com.gabecast.weather.domain.model.WeatherDashboard
import com.gabecast.weather.domain.model.WindUnit
import com.gabecast.weather.location.LocationRepository
import com.gabecast.weather.radar.RadarImageResult
import com.gabecast.weather.radar.RadarRepository
import com.gabecast.weather.settings.isUsableContactEmail
import com.gabecast.weather.settings.UserSettingsRepository
import com.gabecast.weather.widget.WeatherWidgets
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

data class WeatherUiState(
    val settings: UserSettings = UserSettings(),
    val locations: List<SavedLocation> = emptyList(),
    val dashboard: WeatherDashboard = WeatherDashboard(null, null, emptyList(), emptyList(), emptyList(), null, false),
    val selectedTab: AppTab = AppTab.Dashboard,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<LocationSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val contactEmailDraft: String = "",
    val hourlyLimit: Int = 24,
    val radar: RadarUiState = RadarUiState(),
    val errorMessage: String? = null
) {
    val hasLocation: Boolean get() = settings.selectedLocationId != null && dashboard.location != null
    val hasContactEmail: Boolean get() = settings.nwsContactEmailConfirmed && settings.nwsContactEmail.isNotBlank()
}

data class RadarUiState(
    val isLoading: Boolean = false,
    val locationName: String? = null,
    val imageFilePath: String? = null,
    val radarTimestamp: Instant? = null,
    val fetchedAt: Instant? = null,
    val isStale: Boolean = false,
    val errorMessage: String? = null,
    val canRefresh: Boolean = true
)

enum class AppTab(val label: String) {
    Dashboard("Home"),
    Hourly("Hourly"),
    Daily("Daily"),
    Alerts("Alerts"),
    Radar("Radar"),
    Locations("Places"),
    Settings("Settings"),
    About("About")
}

class WeatherViewModel(
    private val weatherRepository: WeatherRepository,
    private val radarRepository: RadarRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: UserSettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var lastLoadedLocationId: String? = null
    private var lastManualRefresh: Instant? = null
    private var lastRadarManualRefresh: Instant? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        contactEmailDraft = if (settings.nwsContactEmailConfirmed) settings.nwsContactEmail else it.contactEmailDraft
                    )
                }
                if (settings.nwsContactEmailConfirmed && settings.selectedLocationId != null && settings.selectedLocationId != lastLoadedLocationId) {
                    loadLocation(settings.selectedLocationId)
                } else if (settings.selectedLocationId == null) {
                    _uiState.update { it.copy(isLoading = false, dashboard = WeatherDashboard(null, null, emptyList(), emptyList(), emptyList(), null, false)) }
                } else if (!settings.nwsContactEmailConfirmed) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
        viewModelScope.launch {
            weatherRepository.observeLocations().collect { locations ->
                _uiState.update { it.copy(locations = locations) }
                val selected = _uiState.value.settings.selectedLocationId
                if (selected != null && locations.none { it.id == selected }) {
                    settingsRepository.setSelectedLocation(locations.firstOrNull()?.id)
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == AppTab.Radar) loadRadar(forceRefresh = false)
    }

    fun setHourlyLimit(limit: Int) {
        _uiState.update { it.copy(hourlyLimit = limit) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateContactEmailDraft(email: String) {
        _uiState.update { it.copy(contactEmailDraft = email) }
    }

    fun saveContactEmail() {
        val email = _uiState.value.contactEmailDraft.trim()
        if (!email.isUsableContactEmail()) {
            _uiState.update { it.copy(errorMessage = "Enter a valid email before loading weather data.") }
            return
        }
        viewModelScope.launch { settingsRepository.setNwsContactEmail(email) }
    }

    fun searchLocations() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return
        if (!_uiState.value.hasContactEmail) {
            _uiState.update { it.copy(errorMessage = "Enter a contact email before searching for weather.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            val results = runCatching { locationRepository.search(query) }
                .getOrElse {
                    _uiState.update { state -> state.copy(errorMessage = "Location search is unavailable on this device.") }
                    emptyList()
                }
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResults = results,
                    errorMessage = if (results.isEmpty()) "No U.S. locations found for \"$query\"." else it.errorMessage
                )
            }
        }
    }

    fun addSearchResult(result: LocationSearchResult) {
        viewModelScope.launch {
            if (!_uiState.value.hasContactEmail) {
                _uiState.update { it.copy(errorMessage = "Enter a contact email before loading weather.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                weatherRepository.addOrUpdateLocation(
                    latitude = result.latitude,
                    longitude = result.longitude,
                    preferredName = result.displayName,
                    preferredState = result.state
                )
            }.onSuccess { location ->
                settingsRepository.setSelectedLocation(location.id)
                _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), selectedTab = AppTab.Dashboard) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            if (!_uiState.value.hasContactEmail) {
                _uiState.update { it.copy(errorMessage = "Enter a contact email before loading weather.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val coordinates = locationRepository.getCurrentCoordinates()
            if (coordinates == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Location permission is needed, or search manually.") }
                return@launch
            }
            runCatching {
                weatherRepository.addOrUpdateLocation(coordinates.first, coordinates.second)
            }.onSuccess { location ->
                settingsRepository.setSelectedLocation(location.id)
                _uiState.update { it.copy(selectedTab = AppTab.Dashboard) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun selectLocation(locationId: String) {
        viewModelScope.launch {
            settingsRepository.setSelectedLocation(locationId)
            _uiState.update { it.copy(selectedTab = AppTab.Dashboard) }
        }
    }

    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            weatherRepository.deleteLocation(locationId)
            val remaining = weatherRepository.getLocations().filterNot { it.id == locationId }
            if (_uiState.value.settings.selectedLocationId == locationId) {
                settingsRepository.setSelectedLocation(remaining.firstOrNull()?.id)
            }
        }
    }

    fun refresh(force: Boolean = true) {
        val selected = _uiState.value.settings.selectedLocationId ?: return
        if (!_uiState.value.hasContactEmail) {
            _uiState.update { it.copy(errorMessage = "Enter a contact email before refreshing weather.") }
            return
        }
        val last = lastManualRefresh
        val now = Instant.now()
        if (force && last != null && Duration.between(last, now).seconds < 45) {
            _uiState.update { it.copy(errorMessage = "Forecast already up to date. Try again in a moment.") }
            return
        }
        if (force) lastManualRefresh = now
        loadLocation(selected, forceRefresh = force)
    }

    fun refreshRadar() {
        if (!_uiState.value.hasContactEmail) {
            _uiState.update { it.copy(errorMessage = "Enter a contact email before loading radar.") }
            return
        }
        val last = lastRadarManualRefresh
        val now = Instant.now()
        if (last != null && Duration.between(last, now).seconds < 120) {
            _uiState.update {
                it.copy(
                    radar = it.radar.copy(canRefresh = false),
                    errorMessage = "Radar was refreshed recently. Try again in a minute."
                )
            }
            return
        }
        lastRadarManualRefresh = now
        loadRadar(forceRefresh = true)
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { settingsRepository.setTemperatureUnit(unit) }
    }

    fun setWindUnit(unit: WindUnit) {
        viewModelScope.launch { settingsRepository.setWindUnit(unit) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setNwsContactEmail(email: String) {
        viewModelScope.launch { settingsRepository.setNwsContactEmail(email) }
    }

    fun setBackgroundRefresh(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBackgroundRefreshEnabled(enabled) }
    }

    private fun loadLocation(locationId: String, forceRefresh: Boolean = false) {
        if (!_uiState.value.hasContactEmail) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            lastLoadedLocationId = locationId
            _uiState.update { it.copy(isLoading = !it.hasLocation, isRefreshing = true, errorMessage = null) }
            runCatching { weatherRepository.loadCachedDashboard(locationId) }
                .onSuccess { cached ->
                    if (cached.location != null || cached.dailyForecast.isNotEmpty()) {
                        _uiState.update { it.copy(dashboard = cached, isLoading = false) }
                    }
                }
            runCatching { weatherRepository.loadDashboardForDisplay(locationId, forceRefresh) }
                .onSuccess { dashboard ->
                    _uiState.update {
                        it.copy(
                            dashboard = dashboard,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = dashboard.refreshMessage
                        )
                    }
                    WeatherWidgets.updateAll(ServiceLocator.applicationContext())
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = throwable.userMessage()) }
                }
        }
    }

    private fun loadRadar(forceRefresh: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val selectedId = state.settings.selectedLocationId
            val location = state.dashboard.location ?: selectedId?.let { weatherRepository.getLocation(it) }
            if (location == null) {
                _uiState.update { it.copy(errorMessage = "Choose a location before loading radar.") }
                return@launch
            }
            if (!state.hasContactEmail) {
                _uiState.update { it.copy(errorMessage = "Enter a contact email before loading radar.") }
                return@launch
            }
            _uiState.update { it.copy(radar = it.radar.copy(isLoading = true, errorMessage = null)) }
            runCatching { radarRepository.getLatestRadarImage(location, forceRefresh = forceRefresh) }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            radar = result.toUiState(
                                isLoading = false,
                                canRefresh = true
                            ),
                            errorMessage = result.errorMessage
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            radar = it.radar.copy(
                                isLoading = false,
                                errorMessage = throwable.userMessage(),
                                canRefresh = true
                            ),
                            errorMessage = throwable.userMessage()
                        )
                    }
                }
        }
    }

    private fun RadarImageResult.toUiState(isLoading: Boolean, canRefresh: Boolean): RadarUiState = RadarUiState(
        isLoading = isLoading,
        locationName = locationName,
        imageFilePath = imageFilePath,
        radarTimestamp = radarTimestamp,
        fetchedAt = fetchedAt,
        isStale = isStale,
        errorMessage = errorMessage,
        canRefresh = canRefresh
    )

    private fun Throwable.userMessage(): String = when (this) {
        is UnsupportedLocationException -> "This app currently supports U.S. locations only."
        else -> message?.takeIf { it.isNotBlank() } ?: "Could not load weather."
    }

    companion object {
        fun factory(
            weatherRepository: WeatherRepository,
            radarRepository: RadarRepository,
            locationRepository: LocationRepository,
            settingsRepository: UserSettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WeatherViewModel(weatherRepository, radarRepository, locationRepository, settingsRepository) as T
            }
        }
    }
}
