package com.gabecast.weather.data.remote

import android.content.Context
import com.gabecast.weather.BuildConfig
import com.gabecast.weather.settings.UserSettingsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://api.weather.gov/"

fun createNwsApi(context: Context): NwsApi {
    val cacheDirectory = File(context.cacheDir, "http_weather")
    val cache = Cache(cacheDirectory, 40L * 1024L * 1024L)
    val contactPrefs = context.getSharedPreferences("request_contact", Context.MODE_PRIVATE)
    val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    val client = OkHttpClient.Builder()
        .cache(cache)
        .addInterceptor { chain ->
            val confirmed = contactPrefs.getBoolean(UserSettingsRepository.CONTACT_CONFIRMED_PREF_KEY, false)
            val contactEmail = contactPrefs
                .getString(UserSettingsRepository.CONTACT_PREF_KEY, "")
                ?.takeIf { it.isNotBlank() }
            if (!confirmed || contactEmail == null) {
                throw IOException("Enter a contact email before requesting weather data.")
            }
            val request = chain.request().newBuilder()
                .header("User-Agent", "GabeCast/1.0 ($contactEmail)")
                .header("Accept", "application/geo+json")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(NwsApi::class.java)
}
