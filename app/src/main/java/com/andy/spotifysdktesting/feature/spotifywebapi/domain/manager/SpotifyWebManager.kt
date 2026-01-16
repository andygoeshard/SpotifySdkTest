package com.andy.spotifysdktesting.feature.spotifywebapi.domain.manager

import com.andy.spotifysdktesting.feature.spotifywebapi.data.dto.TrackRecommendation
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.model.Track
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.repository.AuthRepository
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.repository.SpotifyRepository

class SpotifyWebManager(
    private val repo: SpotifyRepository,
    private val authRepo: AuthRepository
) {

    suspend fun searchTracks(query: String): Result<List<Track>> =
        runCatching { repo.searchTracks(query) }
    suspend fun exchangeCode(code: String){
        runCatching { authRepo.exchangeCodeForToken(code) }
    }

    suspend fun getTrackUri(query: String): Result<String?> =
        runCatching { repo.getTrackUriFromSearch(query) }

    suspend fun topTracks(limit: Int): Result<List<TrackRecommendation>> =
        runCatching { repo.getTopTracks(limit) }

    suspend fun recentlyPlayed(limit: Int): Result<List<TrackRecommendation>> =
        runCatching { repo.getRecentlyPlayed(limit) }
}