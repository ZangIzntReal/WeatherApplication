package com.example.weatherapplication.data.countrymodels

data class Country(
    val data: List<Data>,
    val error: Boolean,
    val msg: String
)