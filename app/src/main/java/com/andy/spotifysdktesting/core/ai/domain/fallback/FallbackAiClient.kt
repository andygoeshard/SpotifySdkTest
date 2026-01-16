package com.andy.spotifysdktesting.core.ai.domain.fallback

import com.andy.spotifysdktesting.core.ai.domain.repository.AiClient

class FallbackAiClient(
    private val primary: AiClient,
    private val secondary: AiClient
) : AiClient {

    override suspend fun generateContent(prompt: String): String {
        return runCatching {
            primary.generateContent(prompt)
        }.getOrElse {
            secondary.generateContent(prompt)
        }
    }
}
