package com.andy.spotifysdktesting.core.userprefs.domain.model

import kotlinx.serialization.Serializable


@Serializable
data class NewsUserPrefs(
    val preferredSources: List<String> = emptyList(),
    val localNewsEnabled: Boolean = false
)