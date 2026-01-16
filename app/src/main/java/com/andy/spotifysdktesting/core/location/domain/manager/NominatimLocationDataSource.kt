package com.andy.spotifysdktesting.core.location.domain.manager

import com.andy.spotifysdktesting.core.location.domain.model.LocationCoordinates
import com.andy.spotifysdktesting.core.location.domain.model.LocationResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

class NominatimLocationDataSource(
    private val http: HttpClient
) {
    @Serializable
    private data class NominatimResponse(
        val display_name: String,
        val lat: String,
        val lon: String
    )

    suspend fun search(query: String): List<LocationResult> {
        return try {
            val response: List<NominatimResponse> =
                http.get("https://nominatim.openstreetmap.org/search") {
                    parameter("q", query)
                    parameter("format", "json")
                    parameter("limit", 5)
                }.body()

            response.map {
                LocationResult(
                    displayName = it.display_name,
                    coordinates = LocationCoordinates(
                        lat = it.lat.toDouble(),
                        lon = it.lon.toDouble()
                    )
                )
            }

        } catch (e: Exception) {
            emptyList()
        }
    }
}
