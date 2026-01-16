package com.andy.spotifysdktesting.feature.apicontent.data.dtos

import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(val current_weather: WeatherData)
@Serializable
data class WeatherData(val temperature: Double)
@Serializable
data class MeowResponse(val data: List<String>)
@Serializable
data class NewsResponse(val results: List<NewsItem>)
@Serializable
data class NewsItem(val title: String)
@Serializable
data class BoredResponse(val activity: String)