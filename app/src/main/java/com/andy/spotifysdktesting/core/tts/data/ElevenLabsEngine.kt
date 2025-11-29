package com.andy.spotifysdktesting.core.tts.data

import com.andy.spotifysdktesting.core.tts.domain.TtsEngine
import com.andy.spotifysdktesting.core.tts.domain.TtsResult
import com.andy.spotifysdktesting.core.tts.domain.TtsVoice
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.readBytes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.system.measureTimeMillis

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
        val url =
            "https://api.elevenlabs.io/v1/text-to-speech/${voice.id}/stream?output_format=opus_48000_128"

        val requestBody = ElevenRequest(text)

        return try {
            var audioBytes: ByteArray? = null

            val duration = measureTimeMillis {
                val response: HttpResponse = client.post(url) {
                    header("xi-api-key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

                if (!response.status.isSuccess()) {
                    return TtsResult.Error("HTTP ${response.status}: ${response.bodyAsText()}")
                }

                val channel = response.bodyAsChannel()
                audioBytes = channel.readRemaining().readBytes()
            }

            val bytes = audioBytes ?: return TtsResult.Error("No audio generated")

            if (bytes.isEmpty()) {
                return TtsResult.Error("Empty audio")
            }

            TtsResult.Success(bytes)

        } catch (e: Exception) {
            TtsResult.Error("Ktor ElevenLabs error", e)
        }
    }
}
