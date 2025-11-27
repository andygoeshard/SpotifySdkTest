package com.andy.spotifysdktesting.core.tts.domain

interface TtsEngine {
    suspend fun synthesize(
        text: String,
        voice: TtsVoice
    ): TtsResult
}

