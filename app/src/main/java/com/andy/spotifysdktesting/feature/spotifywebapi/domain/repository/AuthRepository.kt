package com.andy.spotifysdktesting.feature.spotifywebapi.domain.repository

interface AuthRepository {
    suspend fun startLogin(): String
    suspend fun exchangeCodeForToken(code: String): Boolean
    suspend fun refreshToken(): Boolean
    suspend fun getCurrentAccessToken(): String?
    suspend fun clearTokens()
}