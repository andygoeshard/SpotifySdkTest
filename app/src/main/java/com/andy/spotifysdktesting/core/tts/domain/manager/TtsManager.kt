package com.andy.spotifysdktesting.core.tts.domain.manager

import android.util.Log
import com.andy.spotifysdktesting.core.tts.domain.engine.TtsEngine
import com.andy.spotifysdktesting.core.tts.domain.engine.TtsEngineType
import com.andy.spotifysdktesting.core.tts.domain.result.TtsResult
import com.andy.spotifysdktesting.core.tts.domain.model.TtsVoice

class TtsManager(
    private val eleven: TtsEngine,
    private val androidNative: TtsEngine
) {
    suspend fun speak(
        text: String,
        voice: TtsVoice,
        engine: TtsEngineType = TtsEngineType.ANDROID_NATIVE
    ): TtsResult {

        val ttsEngine = when (engine) {
            TtsEngineType.ELEVEN_LABS -> eleven
            TtsEngineType.ANDROID_NATIVE -> androidNative
        }

        val result = ttsEngine.synthesize(text, voice)

        if (result is TtsResult.Success) {
            return result
        }

        if (engine != TtsEngineType.ANDROID_NATIVE) {
            Log.w("TtsManager", "Fallo con ElevenLabs, intentando con TTS nativo como fallback.")
            return androidNative.synthesize(text, voice)
        }

        return result
    }
}