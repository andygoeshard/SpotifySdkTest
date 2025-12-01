package com.andy.spotifysdktesting.core.ai.data.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- MODELOS DE SOLICITUD (REQUEST) ---

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqCompletionRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerialName("response_format")
    val responseFormat: ResponseFormat? = null // Opcional para JSON estricto
)

@Serializable
data class ResponseFormat(
    val type: String = "json_object"
)


// --- MODELOS DE RESPUESTA (RESPONSE) ---

@Serializable
data class GroqResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val index: Int,
    val message: GroqMessage
)