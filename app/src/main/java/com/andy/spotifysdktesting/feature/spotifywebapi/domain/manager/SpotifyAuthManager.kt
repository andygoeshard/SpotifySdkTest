package com.andy.spotifysdktesting.feature.spotifywebapi.domain.manager

import com.andy.spotifysdktesting.feature.spotifywebapi.domain.repository.AuthRepository
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.state.SpotifyAuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpotifyAuthManager(
    private val repo: AuthRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SpotifyAuthState())
    val state: StateFlow<SpotifyAuthState> = _state

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        scope.launch(Dispatchers.IO) {
            val token = repo.getCurrentAccessToken()
            _state.value = SpotifyAuthState(
                isLoggedIn = !token.isNullOrEmpty(),
                accessToken = token
            )
        }
    }

    fun startLogin() {
        scope.launch {
            val url = repo.startLogin()
            _state.value = _state.value.copy(authUrl = url)
        }
    }

    fun onCodeReceived(code: String) {
        scope.launch {
            val success = repo.exchangeCodeForToken(code)
            if (success) {
                val token = repo.getCurrentAccessToken()
                _state.value = SpotifyAuthState(
                    isLoggedIn = true,
                    accessToken = token
                )
            }
        }
    }

    fun logout() {
        scope.launch {
            repo.clearTokens()
            _state.value = SpotifyAuthState()
        }
    }


}