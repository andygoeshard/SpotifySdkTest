package com.andy.spotifysdktesting.feature.spotifysdk.domain.service

import androidx.media3.common.util.Log
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

// ⚠️ SOLO RECIBE EL CLIENTE HTTP
class SpotifyApiService(
    private val client: HttpClient
) {
    // ELIMINAMOS authHeader, ya que Ktor Auth Plugin lo hace automáticamente

    suspend fun searchTracks(query: String): String {
        Log.d("SpotifyApiService", "Buscando tracks con query: $query")
        return client.get("https://api.spotify.com/v1/search") {
            // ELIMINAMOS authHeader(this)
            parameter("q", query)
            parameter("type", "track")
            parameter("limit", 20)
        }.body()
    }

    suspend fun getTrack(id: String): String {
        return client.get("https://api.spotify.com/v1/tracks/$id") {
            // ELIMINAMOS authHeader(this)
        }.body()
    }

    suspend fun getAudioFeatures(id: String): String {
        return client.get("https://api.spotify.com/v1/audio-features/$id") {
            // ELIMINAMOS authHeader(this)
        }.body()
    }

    suspend fun getRecommendations(
        seedTracks: List<String> = emptyList(),
        seedArtists: List<String> = emptyList(),
        seedGenres: List<String> = emptyList()
    ): String {
        return client.get("https://api.spotify.com/v1/recommendations") {
            // ELIMINAMOS authHeader(this)

            if (seedTracks.isNotEmpty())
                parameter("seed_tracks", seedTracks.joinToString(","))

            if (seedArtists.isNotEmpty())
                parameter("seed_artists", seedArtists.joinToString(","))

            if (seedGenres.isNotEmpty())
                parameter("seed_genres", seedGenres.joinToString(","))

            parameter("limit", 20)
        }.body()
    }
}