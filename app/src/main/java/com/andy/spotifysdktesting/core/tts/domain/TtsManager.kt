package com.andy.spotifysdktesting.core.tts.domain

import android.util.Log


class TtsManager(
    private val eleven: TtsEngine,
    private val androidNative: TtsEngine // 💡 Recibe el motor nativo
) {
    suspend fun speak(
        text: String,
        voice: TtsVoice,
        engine: TtsEngineType = TtsEngineType.ANDROID_NATIVE // 💡 Permite seleccionar el motor
    ): TtsResult {

        val ttsEngine = when (engine) {
            TtsEngineType.ELEVEN_LABS -> eleven
            TtsEngineType.ANDROID_NATIVE -> androidNative
        }

        val result = ttsEngine.synthesize(text, voice)

        if (result is TtsResult.Success) {
            return result
        }

        // 💡 Lógica de Fallback (solo si el preferido falló Y no es ya el nativo)
        if (engine != TtsEngineType.ANDROID_NATIVE) {
            Log.w("TtsManager", "Fallo con ElevenLabs, intentando con TTS nativo como fallback.")
            return androidNative.synthesize(text, voice)
        }

        // Si falló el nativo o no se intentó el fallback
        return result
    }
}