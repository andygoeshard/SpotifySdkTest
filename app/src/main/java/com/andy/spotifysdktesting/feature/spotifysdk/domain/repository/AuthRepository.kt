package com.andy.spotifysdktesting.feature.spotifysdk.domain.repository


interface AuthRepository {
    suspend fun startLogin(): String // Antes Pair<String, String>
    suspend fun exchangeCodeForToken(code: String): Boolean // Antes (code, verifier)
    suspend fun refreshToken(): Boolean
    suspend fun getCurrentAccessToken(): String?
}