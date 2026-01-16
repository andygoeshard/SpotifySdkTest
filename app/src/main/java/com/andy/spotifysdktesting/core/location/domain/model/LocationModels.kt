package com.andy.spotifysdktesting.core.location.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationCoordinates(
    val lat: Double,
    val lon: Double
)

@Serializable
data class LocationResult(
    val displayName: String,
    val coordinates: LocationCoordinates
)

@Serializable
private data class NominatimResponse(
    val display_name: String,
    val lat: String,
    val lon: String
)