package com.example.weather.data.models

data class HourlyWeatherList(
    val temp: String,
    val imgUrl: String,
    val time: String,
    val highTemp: String,
    val lowTemp: String
)
