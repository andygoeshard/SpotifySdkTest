package com.andy.spotifysdktesting.core.ai.data.repository

import android.util.Log
import com.andy.spotifysdktesting.core.ai.data.entities.DeepSeekContent
import com.andy.spotifysdktesting.core.ai.data.entities.DeepSeekMessage
import com.andy.spotifysdktesting.core.ai.data.entities.DeepSeekRequest
import com.andy.spotifysdktesting.core.ai.domain.repository.AiClient
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import org.slf4j.MDC.put

private const val TAG = "DeepSeekClient"

class DeepSeekClientImpl(
    private val apiKey: String,
    private val httpClient: HttpClient
) : AiClient {

    override suspend fun generateContent(prompt: String): String {

        val request = DeepSeekRequest(
            model = "deepseek-chat",
            messages = listOf(
                DeepSeekMessage(
                    role = "user",
                    content = listOf(
                        DeepSeekContent(text = prompt)
                    )
                )
            )
        )

        val response = httpClient.post("https://api.deepseek.com/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val error = response.bodyAsText()
            Log.e(TAG, "DeepSeek falló: ${response.status} $error")
            throw Exception("DeepSeek error ${response.status}")
        }

        return response.bodyAsText()
    }

}
