package com.andy.spotifysdktesting.feature.spotifywebapi.domain.state

data class SpotifyAuthState(
    val authUrl: String = "",
    val accessToken: String? = null,
    val isLoggedIn: Boolean = false
)