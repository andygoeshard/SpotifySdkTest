package com.andy.spotifysdktesting.feature.apicontent.domain.repository

interface PersonalityApi {
    suspend fun getWeatherTip(): String?
    suspend fun getCatFact(): String?
    suspend fun getNewsHeadline(): String?
    suspend fun getRandomFact(): String?
}