package com.example.weather.repository

import com.example.weather.data.remote.Weather
import com.example.weather.data.remote.WeatherAPI
import com.example.weather.utils.Constants.API_KEY
import com.example.weather.utils.Resource
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class WeatherRepository @Inject constructor(
    private val api: WeatherAPI
) {
    suspend fun getWeatherInfo(
        lat: String,
        lon: String
    ): Resource<Weather> {
        val response = try {
            api.getWeather(lat, lon, API_KEY)
        } catch (e: Exception) {
            return Resource.Error("Unknown Error occurred.")
        }
        return Resource.Success(response)
    }
}