package com.andy.spotifysdktesting.core.location.domain.repository

import com.andy.spotifysdktesting.core.location.domain.model.LocationCoordinates
import com.andy.spotifysdktesting.core.location.domain.model.LocationResult

interface LocationRepository {
    suspend fun getDeviceLocation(): LocationCoordinates?
    suspend fun searchLocation(query: String): List<LocationResult>
    suspend fun resolveLocationFromAI(text: String): LocationCoordinates?
}