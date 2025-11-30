package com.andy.spotifysdktesting.feature.spotifysdk.domain.service

import androidx.media3.common.util.Log
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class SpotifyApiService(
    private val client: HttpClient
) {

    // Funciones existentes (ajustadas para Ktor)

    suspend fun searchTracks(query: String): String {
        Log.d("SpotifyApiService", "Buscando tracks con query: $query")
        return client.get("https://api.spotify.com/v1/search") {
            parameter("q", query)
            parameter("type", "track")
            parameter("limit", 20)
        }.body()
    }

    suspend fun getTrack(id: String): String {
        // Asumiendo que esta es la llamada correcta
        return client.get("https://api.spotify.com/v1/tracks/$id").body()
    }

    suspend fun getAudioFeatures(id: String): String {
        return client.get("https://api.spotify.com/v1/audio-features/$id").body()
    }

    suspend fun getRecommendations(
        seedTracks: List<String> = emptyList(),
        seedArtists: List<String> = emptyList(),
        seedGenres: List<String> = emptyList()
    ): String {
        return client.get("https://api.spotify.com/v1/recommendations") {
            if (seedTracks.isNotEmpty())
                parameter("seed_tracks", seedTracks.joinToString(","))

            if (seedArtists.isNotEmpty())
                parameter("seed_artists", seedArtists.joinToString(","))

            if (seedGenres.isNotEmpty())
                parameter("seed_genres", seedGenres.joinToString(","))

            parameter("limit", 20)
        }.body()
    }

    // 🚀 NUEVOS ENDPOINTS PARA EL DJ

    suspend fun getTopTracks(limit: Int, timeRange: String): String {
        return client.get("https://api.spotify.com/v1/me/top/tracks") {
            parameter("limit", limit)
            parameter("time_range", timeRange)
        }.body()
    }

    suspend fun getRecentlyPlayed(limit: Int): String {
        return client.get("https://api.spotify.com/v1/me/player/recently-played") {
            parameter("limit", limit)
        }.body()
    }
}