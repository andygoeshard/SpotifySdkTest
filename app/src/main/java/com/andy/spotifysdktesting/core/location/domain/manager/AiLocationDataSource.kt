package com.andy.spotifysdktesting.core.location.domain.manager

import coil.util.CoilUtils.result
import com.andy.spotifysdktesting.core.ai.domain.manager.AiManager
import com.andy.spotifysdktesting.core.location.domain.model.LocationCoordinates
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AILocationDataSource(
    private val aiClient: AiManager
) {

    @Serializable
    private data class AILocationWrapper(
        val location: LocationCoordinates?
    )

    suspend fun parseLocation(text: String): LocationCoordinates? {
        val prompt = """
            Extraé la ubicación del siguiente texto humano:
            "$text"

            Respondé SOLO con este JSON:
            {
                "location": { "lat": number, "lon": number }
            }
        """.trimIndent()

        return try {

            return LocationCoordinates( 0.0,0.0)
        } catch (e: Exception) {
            null
        }
    }
}
