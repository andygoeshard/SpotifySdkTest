package com.andy.spotifysdktesting.feature.spotifywebapi.data.dto

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class DeepSeekContent(
    val type: String = "text",
    val text: String
)

@kotlinx.serialization.Serializable
data class DeepSeekMessage(
    val role: String,
    val content: List<DeepSeekContent>
)

@Serializable
data class DeepSeekRequest(
    val model: String,
    val messages: List<DeepSeekMessage>
)
