package com.example.weatherapplication.data.models

data class Sys(
    val country: String,
    val id: Int,
    val sunrise: Long,
    val sunset: Long,
    val type: Int
)