package com.andy.spotifysdktesting.core.tts.data

import com.andy.spotifysdktesting.core.tts.domain.TtsEngine
import com.andy.spotifysdktesting.core.tts.domain.TtsResult
import com.andy.spotifysdktesting.core.tts.domain.TtsVoice
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CoquiEngine(
    private val apiKey: String,
    private val client: OkHttpClient
) : TtsEngine {

    @Serializable
    data class CoquiRequest(val text: String, val voice: String)

    override suspend fun synthesize(text: String, voice: TtsVoice): TtsResult {
        return try {
            val url = "https://api.coqui.ai/v1/text-to-speech"

            val json = Json.encodeToString(
                CoquiRequest(text, voice.id)
            )

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "audio/wav")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return TtsResult.Error("HTTP ${response.code}")
            }

            val audioBytes = response.body?.bytes()
                ?: return TtsResult.Error("Empty audio response")

            TtsResult.Success(audioBytes)

        } catch (e: Exception) {
            TtsResult.Error("Coqui error", e)
        }
    }
}