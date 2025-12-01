package com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpotifyAuthViewModel(
    private val repo: AuthRepository
) : ViewModel() {

    val uiState = MutableStateFlow(SpotifyAuthState())

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() = viewModelScope.launch {
        // Llama al repositorio para ver si existe un token en el DataStore
        val token = repo.getCurrentAccessToken()

        if (!token.isNullOrEmpty()) {
            println("✅ [AUTH] Token encontrado. Actualizando estado a LOGGED_IN.")
            // 💡 Esto es lo que cambiará el estado antes de que la pantalla se cargue.
            uiState.update { it.copy(isLoggedIn = true) }
        } else {
            println("❌ [AUTH] No hay token guardado. Estado LOGGED_OUT.")
            // Mantiene el estado por defecto, pero asegura que el chequeo se hizo.
            uiState.update { it.copy(isLoggedIn = false) }
        }
    }
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

    fun clearTokensAndForceLogin() = viewModelScope.launch {
        repo.clearTokens() // 👈 Asume que tienes este método en AuthRepositoryImpl

        // 2. Resetear el estado de UI
        uiState.update { it.copy(
            isLoggedIn = false,
            authUrl = "" // Asegura que el botón de Login aparezca en la UI
        ) }
    }
}

data class SpotifyAuthState(
    val authUrl: String = "",
    val accessToken: String? = null,
    val isLoggedIn: Boolean = false
)