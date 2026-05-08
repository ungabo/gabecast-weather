package com.gabecast.weather.data.remote

import com.gabecast.weather.data.remote.dto.NwsAlertsResponse
import com.gabecast.weather.data.remote.dto.NwsForecastResponse
import com.gabecast.weather.data.remote.dto.NwsObservationResponse
import com.gabecast.weather.data.remote.dto.NwsPointResponse
import com.gabecast.weather.data.remote.dto.NwsStationsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface NwsApi {
    @GET("points/{lat},{lon}")
    suspend fun getPoint(
        @Path("lat") latitude: String,
        @Path("lon") longitude: String
    ): Response<NwsPointResponse>

    @GET
    suspend fun getForecast(@Url url: String): Response<NwsForecastResponse>

    @GET
    suspend fun getHourlyForecast(@Url url: String): Response<NwsForecastResponse>

    @GET
    suspend fun getObservationStations(@Url url: String): Response<NwsStationsResponse>

    @GET("stations/{stationId}/observations/latest")
    suspend fun getLatestObservation(@Path("stationId") stationId: String): Response<NwsObservationResponse>

    @GET("alerts/active")
    suspend fun getActiveAlertsByPoint(
        @Query("point") point: String
    ): Response<NwsAlertsResponse>
}
