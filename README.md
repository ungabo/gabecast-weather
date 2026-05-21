# GabeCast

Free. Ad-free.

GabeCast is a compact Android weather app for U.S. locations. It uses official NOAA/National Weather Service data directly from the device, with local caching for forecasts, current conditions, alerts, radar, and widgets.

## Contact Email Requirement

NOAA/NWS asks client applications to identify a contact in API requests. GabeCast does not ship with a default, dummy, or developer email address.

On first launch, the user must enter their own contact email before the app can make weather or radar API calls. The email is:

- saved only on the device;
- used only in the HTTP `User-Agent` header for NOAA/NWS requests;
- required before location search, current-location setup, forecast refresh, radar refresh, or background widget refresh can request data.

If the email is blank or invalid, GabeCast keeps API calls disabled and shows the setup screen.

## Features

- Current conditions
- Daily forecast
- Hourly forecast
- Active weather alerts
- Static radar image near the selected location
- Saved locations
- Multiple compact Android widgets
- First-seen active alert notifications, when notification permission is granted
- Fahrenheit/Celsius and mph/km/h settings
- Light, dark, and system theme modes
- Local cache and stale-data fallback
- U.S.-only coverage notice

## Data Sources

Weather and alert data are provided by the National Weather Service API at `https://api.weather.gov`.

Radar imagery is provided by NOAA/National Weather Service public radar image services. When available, GabeCast composites the transparent radar image over a static Esri World Topographic Map basemap for local context without using a full map SDK.

GabeCast is not endorsed by NOAA or the National Weather Service. Weather data and radar imagery may be delayed or unavailable, and users should follow official emergency instructions during hazardous weather.

## Building

This project is a Kotlin Android app using Jetpack Compose, Room, DataStore, OkHttp, Retrofit, WorkManager, and Play Services Location.

From the project root:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat testDebugUnitTest
```

The release APK can be aligned and signed with a local Android keystore for device testing. Do not commit keystores, passwords, APKs, or local SDK/toolchain folders.

## Web Visual Version

A small static web version/design check lives in `web/`. It uses the same GabeCast artwork as the Android app and can be opened directly in a browser:

```text
web/index.html
```

This is useful for fast desktop/mobile screenshot checks of the dashboard, radar, widgets, and artwork composition.

Current screenshot outputs are kept in `web/screenshots/`.

## Local Radar Debugging

Radar/map alignment can be tested locally without installing the Android app:

```powershell
python tools\radar_debug.py --lat 42.2411 --lon -83.6130 --radius 75 --size 1024 --out artifacts\radar-debug\ypsilanti
```

The script fetches the NOAA radar layer and Esri basemap separately, composites them, draws the selected center and radius guide, and writes comparison screenshots plus a JSON report under `artifacts/radar-debug/`. The app uses an explicit latest NOAA radar timestamp and an approximately 75-mile-wide local view.

## Repository Notes

Generated APKs, local toolchains, Gradle caches, `local.properties`, and keystore files are intentionally ignored by git.
