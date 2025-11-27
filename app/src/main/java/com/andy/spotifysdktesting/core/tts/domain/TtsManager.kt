package com.andy.spotifysdktesting.core.tts.domain


class TtsManager(
    private val eleven: TtsEngine
) {
    suspend fun speak(text: String, voice: TtsVoice): TtsResult {
        return when (val res = eleven.synthesize(text, voice)) {
            is TtsResult.Success -> res
            else -> eleven.synthesize(text, voice)
        }
    }
}