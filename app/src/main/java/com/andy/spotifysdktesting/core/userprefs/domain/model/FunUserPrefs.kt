package com.andy.spotifysdktesting.core.userprefs.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FunUserPrefs(
    val catFactsEnabled: Boolean = true,
)