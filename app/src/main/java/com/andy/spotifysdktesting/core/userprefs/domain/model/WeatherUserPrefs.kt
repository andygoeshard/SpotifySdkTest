package com.andy.spotifysdktesting.core.userprefs.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherUserPrefs(
    val defaultCity: String = "",
    val preferredUnit: String = "celsius"
)