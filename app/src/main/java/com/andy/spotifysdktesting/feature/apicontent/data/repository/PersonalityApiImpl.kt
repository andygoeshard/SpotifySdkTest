package com.andy.spotifysdktesting.feature.apicontent.data.repository

import com.andy.spotifysdktesting.feature.apicontent.data.dtos.BoredResponse
import com.andy.spotifysdktesting.feature.apicontent.data.dtos.MeowResponse
import com.andy.spotifysdktesting.feature.apicontent.data.dtos.NewsResponse
import com.andy.spotifysdktesting.feature.apicontent.data.dtos.WeatherResponse
import com.andy.spotifysdktesting.feature.apicontent.domain.repository.PersonalityApi
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class PersonalityApiImpl(
    private val client: HttpClient,
    private val latitude: Double,
    private val longitude: Double,
    private val newsApiKey: String
) : PersonalityApi {

    override suspend fun getWeatherTip(): String? = runCatching {
        val response: WeatherResponse = client.get(
            "https://api.open-meteo.com/v1/forecast"
        ) {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current_weather", true)
        }.body()

        val temp = response.current_weather.temperature
        "En tu zona hay ${temp}°. Ideal para escuchar algo tranqui."

    }.getOrNull()

    override suspend fun getCatFact(): String? = runCatching {
        val resp: MeowResponse = client.get("https://meowfacts.herokuapp.com/").body()
        resp.data.firstOrNull()
    }.getOrNull()

    override suspend fun getNewsHeadline(): String? = runCatching {
        val news: NewsResponse = client.get("https://newsdata.io/api/1/news") {
            parameter("apikey", newsApiKey)
            parameter("country", "ar")
            parameter("language", "es")
        }.body()

        news.results.firstOrNull()?.title
    }.getOrNull()

    override suspend fun getRandomFact(): String? = runCatching {
        val bored = client.get("https://www.boredapi.com/api/activity").body<BoredResponse>()
        bored.activity
    }.getOrNull()
}
