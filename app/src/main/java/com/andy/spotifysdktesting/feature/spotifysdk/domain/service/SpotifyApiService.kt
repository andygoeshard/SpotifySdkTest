package com.andy.spotifysdktesting.feature.spotifysdk.domain.service

import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class SpotifyApiService(
    private val client: HttpClient,
    private val spotifyManager: SpotifyManager
) {
    private suspend fun authHeader(builder: HttpRequestBuilder) {
        val token = spotifyManager.accessToken ?: error("Spotify token null")
        builder.header(HttpHeaders.Authorization, "Bearer $token")
    }

    suspend fun searchTracks(query: String): String {
        return client.get("https://api.spotify.com/v1/search") {
            authHeader(this)
            parameter("q", query)
            parameter("type", "track")
            parameter("limit", 20)
        }.body()
    }

    suspend fun getTrack(id: String): String {
        return client.get("https://api.spotify.com/v1/tracks/$id") {
            authHeader(this)
        }.body()
    }

    suspend fun getAudioFeatures(id: String): String {
        return client.get("https://api.spotify.com/v1/audio-features/$id") {
            authHeader(this)
        }.body()
    }

    suspend fun getRecommendations(
        seedTracks: List<String> = emptyList(),
        seedArtists: List<String> = emptyList(),
        seedGenres: List<String> = emptyList()
    ): String {
        return client.get("https://api.spotify.com/v1/recommendations") {
            authHeader(this)

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
