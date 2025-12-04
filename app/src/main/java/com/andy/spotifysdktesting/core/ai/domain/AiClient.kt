package com.andy.spotifysdktesting.core.ai.domain

interface AiClient {
    suspend fun generateContent(
        prompt: String): String
}