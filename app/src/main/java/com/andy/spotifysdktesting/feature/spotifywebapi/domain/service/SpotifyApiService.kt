package com.andy.spotifysdktesting.feature.spotifywebapi.domain.service

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText

class SpotifyApiService(
    private val client: HttpClient
) {
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