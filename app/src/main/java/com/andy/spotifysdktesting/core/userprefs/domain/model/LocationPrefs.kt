package com.andy.spotifysdktesting.core.userprefs.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationPrefs(
    val chosenMethod: LocationMethod = LocationMethod.NONE,
    val displayName: String = "",
    val lat: Double? = null,
    val lon: Double? = null
)

@Serializable
enum class LocationMethod {
    DEVICE, MANUAL_SEARCH, AI_FREEFORM, NONE
}