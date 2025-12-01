package com.andy.spotifysdktesting.core.ai.data.provider

import com.andy.spotifysdktesting.core.ai.domain.AiClient
import com.google.ai.client.generativeai.GenerativeModel

class GeminiClientImpl(apiKey: String) : AiClient {
    private val client = GenerativeModel(
        modelName = "gemini-2.5-flash", // Mejor modelo para JSON y velocidad
        apiKey = apiKey
    )

    override suspend fun generateContent(prompt: String): String {
        val response = client.generateContent(prompt)
        return response.text ?: ""
    }
}