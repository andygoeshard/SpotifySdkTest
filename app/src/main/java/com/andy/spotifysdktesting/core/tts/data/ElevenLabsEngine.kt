package com.andy.spotifysdktesting.core.tts.data

import android.util.Log
import com.andy.spotifysdktesting.core.tts.domain.TtsEngine
import com.andy.spotifysdktesting.core.tts.domain.TtsResult
import com.andy.spotifysdktesting.core.tts.domain.TtsVoice
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ElevenLabsEngine(
    private val client: HttpClient,
    private val apiKey: String
) : TtsEngine {

    @Serializable
    data class ElevenRequest(
        val text: String,
        val model_id: String = "eleven_multilingual_v2",
        @SerialName("voice_settings")
        val voiceSettings: VoiceSettings = VoiceSettings()
    )

    @Serializable
    data class VoiceSettings(
        val stability: Float = 0.5f,
        val similarity_boost: Float = 0.8f
    )

    override suspend fun synthesize(text: String, voice: TtsVoice): TtsResult {
        return try {

            val url =
                "https://api.elevenlabs.io/v1/text-to-speech/${voice.id}?output_format=mp3_44100_128"

            val requestBody = ElevenRequest(text)

            val response: HttpResponse = client.post(url) {
                header("xi-api-key", apiKey)
                contentType(ContentType.Application.Json)
                accept(ContentType.Audio.MPEG)
                setBody(requestBody)
            }

            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                Log.e("ElevenLabsEngine", "Error: ${response.status} - $errorText")
                return TtsResult.Error("HTTP ${response.status}: $errorText")
            }

            val bytes: ByteArray = response.body()

            if (bytes.isEmpty()) {
                return TtsResult.Error("Empty audio")
            }

            TtsResult.Success(bytes)

        } catch (e: Exception) {
            Log.e("ElevenLabsEngine", "Exception", e)
            TtsResult.Error("Ktor ElevenLabs error", e)
        }
    }
}