package com.andy.spotifysdktesting.feature.spotifysdk.domain.repository

import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.Track

interface SpotifyRepository {
    suspend fun searchTracks(query: String): List<Track>
    suspend fun getTrack(id: String): Track
    suspend fun getTrackUriFromSearch(query: String): String?
    suspend fun getRecommendations(
        seedTracks: List<String>,
        seedArtists: List<String>,
        seedGenres: List<String>
    ): List<Track>
    suspend fun getTopTracks(limit: Int = 20, timeRange: String = "medium_term"): List<Track>
    suspend fun getRecentlyPlayed(limit: Int = 5): List<Track>
}
