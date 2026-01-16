package com.andy.spotifysdktesting.core.ai.domain.repository

interface AiClient {
    suspend fun generateContent(
        prompt: String): String
}

