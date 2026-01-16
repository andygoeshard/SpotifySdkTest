package com.andy.spotifysdktesting.core.ai.domain.state

import com.andy.spotifysdktesting.core.ai.domain.model.AiSuggestion

data class AiState(
    val loading: Boolean = false,
    val suggestion: AiSuggestion? = null,
    val description: String? = null,
    val chatResponse: String? = null,
    val error: String? = null
)
