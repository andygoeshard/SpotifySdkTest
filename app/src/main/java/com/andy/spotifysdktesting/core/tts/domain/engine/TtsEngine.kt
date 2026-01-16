package com.andy.spotifysdktesting.core.tts.domain.engine

import com.andy.spotifysdktesting.core.tts.domain.result.TtsResult
import com.andy.spotifysdktesting.core.tts.domain.model.TtsVoice

interface TtsEngine {
    suspend fun synthesize(
        text: String,
        voice: TtsVoice
    ): TtsResult
}