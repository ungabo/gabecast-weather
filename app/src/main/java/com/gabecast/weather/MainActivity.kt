package com.gabecast.weather

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabecast.weather.domain.model.DailyForecastPeriod
import com.gabecast.weather.domain.model.HourlyForecastPeriod
import com.gabecast.weather.domain.model.SavedLocation
import com.gabecast.weather.domain.model.TemperatureUnit
import com.gabecast.weather.domain.model.ThemeMode
import com.gabecast.weather.domain.model.UserSettings
import com.gabecast.weather.domain.model.WeatherAlert
import com.gabecast.weather.domain.model.WeatherDashboard
import com.gabecast.weather.domain.model.WindUnit
import com.gabecast.weather.domain.util.fahrenheitToCelsius
import com.gabecast.weather.domain.util.mphToKmh
import com.gabecast.weather.domain.util.roundToSingleDecimal
import com.gabecast.weather.ui.AppTab
import com.gabecast.weather.ui.RadarUiState
import com.gabecast.weather.ui.WeatherUiState
import com.gabecast.weather.ui.WeatherViewModel
import com.gabecast.weather.settings.isUsableContactEmail
import com.gabecast.weather.util.formatShortDateTime
import com.gabecast.weather.util.formatShortTime
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: WeatherViewModel = viewModel(
                factory = WeatherViewModel.factory(
                    ServiceLocator.weatherRepository,
                    ServiceLocator.radarRepository,
                    ServiceLocator.locationRepository,
                    ServiceLocator.settingsRepository
                )
            )
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            GabeCastTheme(uiState.settings.themeMode) {
                GabeCastApp(uiState, vm)
            }
        }
    }
}

@Composable
private fun GabeCastTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.System -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFF79D0C5),
            secondary = Color(0xFFF6C85F),
            tertiary = Color(0xFFFF9B85),
            surface = Color(0xFF11191B),
            background = Color(0xFF0C1112),
            error = Color(0xFFFFB4AB)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF28656A),
            secondary = Color(0xFF7A5C00),
            tertiary = Color(0xFF9B432F),
            surface = Color(0xFFF7FAF7),
            background = Color(0xFFFDFCF6),
            error = Color(0xFFB3261E)
        )
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(8.dp)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GabeCastApp(uiState: WeatherUiState, vm: WeatherViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { grants ->
            if (grants.values.any { it }) vm.useCurrentLocation()
        }
    )

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            vm.dismissMessage()
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!uiState.hasContactEmail) {
            ContactSetupScreen(
                uiState = uiState,
                onEmailChange = vm::updateContactEmailDraft,
                onSaveEmail = vm::saveContactEmail
            )
            return@Surface
        }

        if (!uiState.hasLocation && !uiState.isLoading) {
            FirstLaunchScreen(
                uiState = uiState,
                onUseLocation = {
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                },
                onQueryChange = vm::updateSearchQuery,
                onSearch = vm::searchLocations,
                onAddResult = vm::addSearchResult
            )
            return@Surface
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("GabeCast", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        IconButton(onClick = { vm.refresh(true) }, enabled = !uiState.isRefreshing && uiState.hasLocation) {
                            if (uiState.isRefreshing) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    visibleTabs().forEach { tab ->
                        NavigationBarItem(
                            selected = uiState.selectedTab == tab,
                            onClick = { vm.selectTab(tab) },
                            icon = { Icon(tab.icon(), contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (uiState.isLoading && uiState.dashboard.location == null) {
                    LoadingState()
                } else {
                    when (uiState.selectedTab) {
                        AppTab.Dashboard -> DashboardScreen(uiState.dashboard, uiState.settings, vm::refresh)
                        AppTab.Hourly -> HourlyScreen(uiState.dashboard.hourlyForecast, uiState.settings, uiState.hourlyLimit, vm::setHourlyLimit)
                        AppTab.Daily -> DailyScreen(uiState.dashboard.dailyForecast, uiState.settings)
                        AppTab.Alerts -> AlertsScreen(uiState.dashboard.alerts)
                        AppTab.Radar -> RadarScreen(uiState.radar, vm::refreshRadar)
                        AppTab.Locations -> LocationsScreen(uiState, vm, onUseLocation = {
                            launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                        })
                        AppTab.Settings -> SettingsScreen(uiState, vm)
                        AppTab.About -> AboutScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactSetupScreen(
    uiState: WeatherUiState,
    onEmailChange: (String) -> Unit,
    onSaveEmail: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Image(
                painter = painterResource(R.drawable.gabecast_launcher_icon),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text("GabeCast", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Free. Ad-free.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Weather service contact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "NOAA/NWS asks apps to identify a contact in API requests. Your email is saved only on this device and sent only as part of weather/radar request headers.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = uiState.contactEmailDraft,
                        onValueChange = onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Email") }
                    )
                    Button(
                        onClick = onSaveEmail,
                        enabled = uiState.contactEmailDraft.isUsableContactEmail()
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstLaunchScreen(
    uiState: WeatherUiState,
    onUseLocation: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAddResult: (com.gabecast.weather.domain.model.LocationSearchResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Image(
                painter = painterResource(R.drawable.gabecast_launcher_icon),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text("GabeCast", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Free. Ad-free.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("U.S. weather data provided by the National Weather Service.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onUseLocation) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Use my location")
                }
                OutlinedButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Search")
                }
            }
        }
        item { SearchBox(uiState.searchQuery, uiState.isSearching, onQueryChange, onSearch) }
        items(uiState.searchResults) { result ->
            SearchResultCard(result.displayName, "${result.latitude.round4()}, ${result.longitude.round4()}") {
                onAddResult(result)
            }
        }
    }
}

@Composable
private fun DashboardScreen(dashboard: WeatherDashboard, settings: UserSettings, onRefresh: (Boolean) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (dashboard.isStale || dashboard.refreshMessage != null) {
            item { StaleBanner(dashboard.refreshMessage ?: "Showing last available forecast.") }
        }
        item { CurrentConditionsCard(dashboard, settings, onRefresh) }
        if (dashboard.alerts.isNotEmpty()) {
            item { AlertBanner(dashboard.alerts.first()) }
        }
        item { SectionTitle("Next hours") }
        item { HourlyStrip(dashboard.hourlyForecast.take(24), settings) }
        item { SectionTitle("7-day forecast") }
        items(dashboard.dailyForecast.take(14)) { period ->
            DailyPeriodCard(period, settings, expandable = false)
        }
    }
}

@Composable
private fun CurrentConditionsCard(dashboard: WeatherDashboard, settings: UserSettings, onRefresh: (Boolean) -> Unit) {
    val location = dashboard.location
    val current = dashboard.currentConditions
    val today = dashboard.dailyForecast.firstOrNull()
    val icon = weatherIconFor(current?.conditionText ?: today?.shortForecast)
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(location?.displayName ?: "Weather", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(today?.shortForecast ?: current?.conditionText ?: "Forecast loading", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onRefresh(true) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh forecast")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = current?.conditionText ?: today?.shortForecast ?: "Weather condition",
                    modifier = Modifier.size(76.dp)
                )
                Text(formatTemperature(current?.temperatureF, settings), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(current?.conditionText ?: "Current observation unavailable")
                    Text("Wind ${formatWind(current?.windSpeedMph, current?.windDirection, settings)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Humidity ${current?.humidityPercent?.roundToInt()?.let { "$it%" } ?: "--"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Last updated: ${dashboard.fetchedAt?.formatShortDateTime() ?: "--"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AlertBanner(alert: WeatherAlert) {
    val colors = severityColors(alert.severity)
    Card(colors = CardDefaults.cardColors(containerColor = colors.first)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = colors.second)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(alert.event, color = colors.second, fontWeight = FontWeight.Bold)
                Text(alert.headline ?: alert.areaDescription ?: "Active alert", color = colors.second, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun StaleBanner(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun HourlyStrip(periods: List<HourlyForecastPeriod>, settings: UserSettings) {
    if (periods.isEmpty()) {
        EmptyState("Hourly forecast unavailable.")
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(periods) { period ->
            Card(
                modifier = Modifier.width(118.dp).heightIn(min = 132.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(period.startTime.formatShortTime(), style = MaterialTheme.typography.labelLarge)
                    Image(
                        painter = painterResource(weatherIconFor(period.shortForecast)),
                        contentDescription = period.shortForecast ?: "Hourly forecast",
                        modifier = Modifier.size(34.dp)
                    )
                    Text(formatTemperature(period.temperatureF?.toDouble(), settings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(period.shortForecast ?: "--", maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Rain ${period.probabilityOfPrecipitationPercent?.let { "$it%" } ?: "--"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HourlyScreen(periods: List<HourlyForecastPeriod>, settings: UserSettings, limit: Int, onLimitChange: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = limit == 24, onClick = { onLimitChange(24) }, label = { Text("24h") })
                FilterChip(selected = limit == 48, onClick = { onLimitChange(48) }, label = { Text("48h") })
            }
        }
        items(periods.take(limit)) { period ->
            Card {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(weatherIconFor(period.shortForecast)),
                        contentDescription = period.shortForecast ?: "Hourly forecast",
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.width(82.dp)) {
                        Text(period.startTime.formatShortTime(), fontWeight = FontWeight.Bold)
                        Text(period.startTime.formatShortDateTime().substringBefore(","), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(formatTemperature(period.temperatureF?.toDouble(), settings), modifier = Modifier.width(72.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Column(Modifier.weight(1f)) {
                        Text(period.shortForecast ?: "--")
                        Text("Rain ${period.probabilityOfPrecipitationPercent?.let { "$it%" } ?: "--"}  Wind ${period.windSpeed ?: "--"} ${period.windDirection ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyScreen(periods: List<DailyForecastPeriod>, settings: UserSettings) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(periods) { period ->
            DailyPeriodCard(period, settings, expandable = true)
        }
    }
}

@Composable
private fun DailyPeriodCard(period: DailyForecastPeriod, settings: UserSettings, expandable: Boolean) {
    val expandedMap = remember { mutableStateMapOf<Int, Boolean>() }
    val expanded = expandedMap[period.periodNumber] ?: false
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(weatherIconFor(period.shortForecast)),
                    contentDescription = period.shortForecast ?: "Forecast",
                    modifier = Modifier.size(42.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(period.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(period.shortForecast ?: "--", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatTemperature(period.temperatureF?.toDouble(), settings), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text("Wind ${period.windSpeed ?: "--"} ${period.windDirection ?: ""}  Rain ${period.probabilityOfPrecipitationPercent?.let { "$it%" } ?: "--"}")
            if (expandable) {
                TextButton(onClick = { expandedMap[period.periodNumber] = !expanded }) {
                    Text(if (expanded) "Less" else "Details")
                }
            }
            if (expanded || !expandable) {
                val details = period.detailedForecast
                if (!details.isNullOrBlank()) Text(details, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AlertsScreen(alerts: List<WeatherAlert>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (alerts.isEmpty()) {
            item { EmptyState("No active alerts for this location.") }
        }
        items(alerts) { alert ->
            val colors = severityColors(alert.severity)
            Card(colors = CardDefaults.cardColors(containerColor = colors.first)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(alert.event, color = colors.second, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(alert.headline ?: alert.areaDescription ?: "Official NWS alert", color = colors.second)
                    Text("Severity ${alert.severity ?: "--"}  Urgency ${alert.urgency ?: "--"}  Certainty ${alert.certainty ?: "--"}", color = colors.second)
                    Text("Effective ${alert.effective?.formatShortDateTime() ?: "--"}  Expires ${alert.expires?.formatShortDateTime() ?: "--"}", color = colors.second)
                    alert.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = colors.second) }
                    alert.instruction?.takeIf { it.isNotBlank() }?.let { Text(it, color = colors.second, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun RadarScreen(radar: RadarUiState, onRefresh: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Radar near ${radar.locationName ?: "selected location"}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Approx. 75-mile radius", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onRefresh, enabled = radar.canRefresh && !radar.isLoading) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh")
                }
            }
        }
        if (radar.isStale || !radar.errorMessage.isNullOrBlank()) {
            item { StaleBanner(radar.errorMessage ?: "Showing older radar image.") }
        }
        item {
            Card {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(radar.imageFilePath) {
                        radar.imageFilePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "NOAA radar image",
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else if (radar.isLoading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(10.dp))
                            Text("Loading radar image...")
                        }
                    } else {
                        Image(
                            painter = painterResource(R.drawable.gabecast_radar_placeholder),
                            contentDescription = "Radar placeholder",
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Radar time: ${radar.radarTimestamp?.formatShortDateTime() ?: "Latest available"}")
                    Text("Updated in app: ${radar.fetchedAt?.formatShortDateTime() ?: "--"}")
                    Text(
                        "Radar imagery may be delayed. Imagery provided by NOAA/National Weather Service.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationsScreen(uiState: WeatherUiState, vm: WeatherViewModel, onUseLocation: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onUseLocation) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Use location")
                }
                OutlinedButton(onClick = vm::searchLocations, enabled = !uiState.isSearching) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Search")
                }
            }
        }
        item { SearchBox(uiState.searchQuery, uiState.isSearching, vm::updateSearchQuery, vm::searchLocations) }
        items(uiState.searchResults) { result ->
            SearchResultCard(result.displayName, "${result.latitude.round4()}, ${result.longitude.round4()}") {
                vm.addSearchResult(result)
            }
        }
        item { SectionTitle("Saved locations") }
        if (uiState.locations.isEmpty()) {
            item { EmptyState("No saved locations yet.") }
        }
        items(uiState.locations) { location ->
            SavedLocationRow(
                location = location,
                selected = uiState.settings.selectedLocationId == location.id,
                onSelect = { vm.selectLocation(location.id) },
                onDelete = { vm.deleteLocation(location.id) }
            )
        }
    }
}

@Composable
private fun SearchBox(query: String, searching: Boolean, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("City, ZIP, or address") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = { if (searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}

@Composable
private fun SearchResultCard(title: String, subtitle: String, onAdd: () -> Unit) {
    Card(onClick = onAdd) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SavedLocationRow(location: SavedLocation, selected: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onSelect, colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Place, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(location.displayName, fontWeight = FontWeight.Bold)
                Text("${location.latitude.round4()}, ${location.longitude.round4()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${location.displayName}")
            }
        }
    }
}

@Composable
private fun SettingsScreen(uiState: WeatherUiState, vm: WeatherViewModel) {
    val settings = uiState.settings
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionTitle("Units") }
        item {
            ChoiceRow("Temperature", TemperatureUnit.entries, settings.temperatureUnit, vm::setTemperatureUnit) { it.name }
        }
        item {
            ChoiceRow("Wind", WindUnit.entries, settings.windUnit, vm::setWindUnit) { if (it == WindUnit.Mph) "mph" else "km/h" }
        }
        item { SectionTitle("Theme") }
        item {
            ChoiceRow("Theme", ThemeMode.entries, settings.themeMode, vm::setThemeMode) { it.name }
        }
        item { SectionTitle("Data source") }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NWS contact email", fontWeight = FontWeight.Bold)
                    Text(
                        "Saved only on this device. NOAA/NWS requests a contact in weather API headers, so weather and radar calls stay off until this is set.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = uiState.contactEmailDraft,
                        onValueChange = vm::updateContactEmailDraft,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Email") }
                    )
                    Button(
                        onClick = vm::saveContactEmail,
                        enabled = uiState.contactEmailDraft.isUsableContactEmail()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
        item {
            Card {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Background refresh", fontWeight = FontWeight.Bold)
                        Text("Conservative refresh every few hours", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.backgroundRefreshEnabled, onCheckedChange = vm::setBackgroundRefresh)
                }
            }
        }
    }
}

@Composable
private fun <T> ChoiceRow(label: String, values: List<T>, selected: T, onSelect: (T) -> Unit, text: (T) -> String) {
    Card {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontWeight = FontWeight.Bold)
            values.forEach { value ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == value, onClick = { onSelect(value) })
                    Text(text(value))
                }
            }
        }
    }
}

@Composable
private fun AboutScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("About GabeCast", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Weather data: National Weather Service / NOAA.")
                    Text("Radar imagery: NOAA/NWS public radar image services.")
                    Text("ZIP lookup data: GeoNames postal code data, CC BY 4.0.")
                    Text("Coverage: United States and associated territories.")
                    Text("Free. Ad-free.")
                    Text("No account and no custom server for the MVP.")
                    Text("Weather data can be delayed or unavailable. Follow official emergency instructions during severe weather.")
                    Text("Weather-service contact email is saved locally and required before weather API requests.")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyState(text: String) {
    Card {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private fun visibleTabs(): List<AppTab> = listOf(
    AppTab.Dashboard,
    AppTab.Hourly,
    AppTab.Daily,
    AppTab.Alerts,
    AppTab.Radar,
    AppTab.Locations,
    AppTab.Settings,
    AppTab.About
)

private fun AppTab.icon(): ImageVector = when (this) {
    AppTab.Dashboard -> Icons.Default.Home
    AppTab.Hourly -> Icons.Default.AccessTime
    AppTab.Daily -> Icons.Default.CalendarToday
    AppTab.Alerts -> Icons.Default.Warning
    AppTab.Radar -> Icons.Default.Radar
    AppTab.Locations -> Icons.Default.Place
    AppTab.Settings -> Icons.Default.Settings
    AppTab.About -> Icons.Default.Info
}

private fun formatTemperature(valueF: Double?, settings: UserSettings): String {
    val value = valueF ?: return "--"
    return when (settings.temperatureUnit) {
        TemperatureUnit.Fahrenheit -> "${value.roundToInt()}\u00B0F"
        TemperatureUnit.Celsius -> "${fahrenheitToCelsius(value).roundToInt()}\u00B0C"
    }
}

private fun weatherIconFor(forecast: String?): Int {
    val text = forecast.orEmpty().lowercase()
    return when {
        "thunder" in text || "storm" in text || "lightning" in text -> R.drawable.ic_weather_storm
        "snow" in text || "sleet" in text || "ice" in text || "freezing" in text -> R.drawable.ic_weather_snow
        "rain" in text || "shower" in text || "drizzle" in text -> R.drawable.ic_weather_rain
        "fog" in text || "mist" in text || "haze" in text || "smoke" in text -> R.drawable.ic_weather_fog
        "wind" in text || "breezy" in text || "gust" in text -> R.drawable.ic_weather_wind
        "partly" in text || "mostly sunny" in text || "mostly clear" in text -> R.drawable.ic_weather_partly_cloudy
        "cloud" in text || "overcast" in text -> R.drawable.ic_weather_cloudy
        else -> R.drawable.ic_weather_sunny
    }
}

private fun formatWind(speedMph: Double?, direction: String?, settings: UserSettings): String {
    val speed = speedMph ?: return "--"
    return when (settings.windUnit) {
        WindUnit.Mph -> "${speed.roundToSingleDecimal()} mph ${direction.orEmpty()}".trim()
        WindUnit.Kmh -> "${mphToKmh(speed).roundToSingleDecimal()} km/h ${direction.orEmpty()}".trim()
    }
}

private fun severityColors(severity: String?): Pair<Color, Color> = when (severity?.lowercase()) {
    "extreme" -> Color(0xFF7F1D1D) to Color.White
    "severe" -> Color(0xFFFFDAD6) to Color(0xFF7F1D1D)
    "moderate" -> Color(0xFFFFE8B3) to Color(0xFF4D3900)
    else -> Color(0xFFE8F1EE) to Color(0xFF173F43)
}

private fun Double.round4(): String = "%.4f".format(this)
