package com.example.weatherapplication.data

import com.example.weatherapplication.data.models.CurrentWeather
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Response


interface ApiInterface {
    @GET("weather?")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("units") units: String,
        @Query("lang") lang: String,
        @Query("appid") apiKey: String,
    ): Response<CurrentWeather>
}