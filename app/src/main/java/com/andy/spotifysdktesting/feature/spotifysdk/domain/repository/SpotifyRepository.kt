package com.andy.spotifysdktesting.feature.spotifysdk.domain.repository

import com.andy.spotifysdktesting.feature.spotifysdk.data.entity.TrackRecommendation
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.Track

interface SpotifyRepository {
    suspend fun searchTracks(query: String): List<Track>
    suspend fun getTrack(id: String): Track
    suspend fun getTrackUriFromSearch(query: String): String?
    suspend fun getRecommendations(
        seedTracks: List<String>,
        seedArtists: List<String>
    ): List<TrackRecommendation>
    suspend fun getTopTracks(limit: Int = 20): List<TrackRecommendation>
    suspend fun getRecentlyPlayed(limit: Int = 5): List<TrackRecommendation>
}
