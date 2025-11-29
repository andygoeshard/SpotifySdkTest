package com.andy.spotifysdktesting.feature.spotifysdk.domain.repository

import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.Track

interface SpotifyRepository {
    suspend fun searchTracks(query: String): List<Track>
    suspend fun getTrack(id: String): Track
    suspend fun getRecommendations(
        seedTracks: List<String>,
        seedArtists: List<String>,
        seedGenres: List<String>
    ): List<Track>
}
