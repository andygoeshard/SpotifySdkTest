package com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SpotifyAuthViewModel(
    private val repo: AuthRepository
) : ViewModel() {

    val uiState = MutableStateFlow(SpotifyAuthState())

    // Eliminado: private var currentVerifier = "" -> YA NO ES NECESARIO

    fun startLogin() = viewModelScope.launch {
        val url = repo.startLogin()
        uiState.value = uiState.value.copy(authUrl = url)
    }

    fun onCodeReceived(code: String) = viewModelScope.launch {
        val result = repo.exchangeCodeForToken(code)
        if (result) {
            uiState.value = uiState.value.copy(
                isLoggedIn = true,
                authUrl = "",
                accessToken = repo.getCurrentAccessToken()
            )
            println("LOGUEADO CON SPOTIFY")
        } else {
            println("ERROR AL LOGUEAR - Fallo intercambio de token")
        }
    }
}

data class SpotifyAuthState(
    val authUrl: String = "",
    val accessToken: String? = null,
    val isLoggedIn: Boolean = false
)