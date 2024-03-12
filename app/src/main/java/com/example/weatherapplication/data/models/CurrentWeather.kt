package com.example.weatherapplication.data.models

data class CurrentWeather(
    val city: City,
    val cnt: Int,
    val cod: String,
    val list: List<WeatherData>,
    val message: Int,
)