package com.andy.spotifysdktesting.core.ai.data.provider

import com.google.ai.client.generativeai.GenerativeModel

class GeminiClientProvider(apiKey: String) {
    val client = GenerativeModel(
        modelName = "gemini-2.0-flash-lite",
        apiKey = apiKey
    )
}
