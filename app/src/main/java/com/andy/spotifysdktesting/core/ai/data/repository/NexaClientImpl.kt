package com.andy.spotifysdktesting.core.ai.data.repository

import com.andy.spotifysdktesting.core.ai.domain.repository.AiClient

class NexaClient(
    private val nexaEngine: NexaEngine
) : AiClient {

    override suspend fun generateContent(prompt: String): String {
        return nexaEngine.runInference(prompt)
    }
}