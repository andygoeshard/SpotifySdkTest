package com.andy.spotifysdktesting.core.userprefs.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DjFeaturesPrefs(
    val showWeather: Boolean = false,
    val showFunTips: Boolean = false,
    val showRandomFact: Boolean = false,
    val showShortNews: Boolean = false
)
