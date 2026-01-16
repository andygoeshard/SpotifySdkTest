package com.andy.spotifysdktesting.feature.spotifywebapi.domain.repository

import com.andy.spotifysdktesting.feature.spotifywebapi.data.dto.TrackRecommendation
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.model.Track

interface SpotifyRepository {
    suspend fun searchTracks(query: String): List<Track>
    suspend fun getTrack(id: String): Track
    suspend fun getTrackUriFromSearch(query: String): String?
    suspend fun getTopTracks(limit: Int = 20): List<TrackRecommendation>
    suspend fun getRecentlyPlayed(limit: Int = 5): List<TrackRecommendation>
}