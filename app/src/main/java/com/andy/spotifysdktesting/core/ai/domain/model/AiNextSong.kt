package com.andy.spotifysdktesting.core.ai.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AiNextSong(
    val song: String,
    val reason: String
)