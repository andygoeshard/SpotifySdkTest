package com.andy.spotifysdktesting.feature.spotifysdk.domain.service

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
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
        return client.get("https://api.spotify.com/v1/tracks/$id").body()
    }

    suspend fun getAudioFeatures(id: String): String {
        return client.get("https://api.spotify.com/v1/audio-features/$id").body()
    }

    suspend fun getRecommendations(
        seedTracks: List<String>,
        seedArtists: List<String>,
        seedGenres: List<String>
    ): String {
        val response = client.get("https://api.spotify.com/v1/recommendations") {

            if (seedTracks.isNotEmpty()) {
                parameter("seed_tracks", seedTracks.joinToString(","))
            }

            if (seedArtists.isNotEmpty()) {
                parameter("seed_artists", seedArtists.joinToString(","))
            }

            if (seedGenres.isNotEmpty()) {
                parameter("seed_genres", seedGenres.joinToString(","))
            }

            parameter("limit", 10)
        }

        val body = response.bodyAsText()

        Log.e("KtorDebug", "👉 STATUS: ${response.status.value}")
        Log.e("KtorDebug", "👉 BODY RAW: $body")

        return body
    }

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