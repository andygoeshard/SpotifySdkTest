package com.andy.spotifysdktesting.core.location.data.repository

import com.andy.spotifysdktesting.core.location.domain.manager.AILocationDataSource
import com.andy.spotifysdktesting.core.location.domain.manager.DeviceLocationDataSource
import com.andy.spotifysdktesting.core.location.domain.manager.NominatimLocationDataSource
import com.andy.spotifysdktesting.core.location.domain.model.LocationCoordinates
import com.andy.spotifysdktesting.core.location.domain.repository.LocationRepository
import com.andy.spotifysdktesting.core.location.domain.model.LocationResult

class LocationRepositoryImpl(
    private val deviceSource: DeviceLocationDataSource,
    private val nominatimSource: NominatimLocationDataSource,
    private val aiSource: AILocationDataSource
) : LocationRepository {

    override suspend fun getDeviceLocation(): LocationCoordinates? {
        return deviceSource.getLocation()
    }

    override suspend fun searchLocation(query: String): List<LocationResult> {
        return nominatimSource.search(query)
    }

    override suspend fun resolveLocationFromAI(text: String): LocationCoordinates? {
        return aiSource.parseLocation(text)
    }
}
