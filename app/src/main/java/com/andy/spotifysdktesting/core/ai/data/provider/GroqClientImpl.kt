package com.andy.spotifysdktesting.core.ai.data.provider

import android.util.Log
import com.andy.spotifysdktesting.core.ai.data.entities.GroqCompletionRequest
import com.andy.spotifysdktesting.core.ai.data.entities.GroqMessage
import com.andy.spotifysdktesting.core.ai.data.entities.GroqResponse
import com.andy.spotifysdktesting.core.ai.data.entities.ResponseFormat
import com.andy.spotifysdktesting.core.ai.domain.AiClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
private const val TAG = "GroqClientImpl"

class GroqClientImpl(
    apiKey: String,
    private val httpClient: HttpClient // 💡 Se inyecta el HttpClient de Koin
) : AiClient {

    private val authHeader = "Bearer $apiKey"

    override suspend fun generateContent(prompt: String): String {
        val requestBody = GroqCompletionRequest(
            model = "llama-3.1-8b-instant", // Modelo rápido y potente
            messages = listOf(
                GroqMessage(role = "user", content = prompt)
            ),
            responseFormat = ResponseFormat(type = "json_object")
        )

        return try {
            val response: GroqResponse = httpClient.post(GROQ_URL) {
                headers {
                    append(HttpHeaders.Authorization, authHeader)
                    // Content-Type: application/json ya lo maneja ContentNegotiation
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody) // Ktor serializa el objeto automáticamente
            }.body()
            response.choices.firstOrNull()?.message?.content ?: ""

        } catch (e: Exception) {
            Log.e(TAG, "Error en llamada a Groq o en deserialización: ${e.message}")
            ""
        }
    }
}