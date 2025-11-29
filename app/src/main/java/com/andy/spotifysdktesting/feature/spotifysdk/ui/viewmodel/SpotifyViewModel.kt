package com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.BuildConfig
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpotifyViewModel(application: Application) : AndroidViewModel(application) {
    val spotifyManager = SpotifyManager(application.applicationContext)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _currentTrack = MutableStateFlow<CurrentTrack?>(null)
    val currentTrack: StateFlow<CurrentTrack?> = _currentTrack

    private var trackJob: Job? = null
    private var connectionJob: Job? = null
    private val clientId: String = BuildConfig.SPOTIFY_CLIENT_ID
    private val redirectUri: String = BuildConfig.SPOTIFY_REDIRECT_URI

    fun connectToSpotify() {
        connectionJob?.cancel()

        connectionJob = viewModelScope.launch {
            // El Flow se mantiene activo y emitirá el estado de la conexión.
            spotifyManager.connect(clientId, redirectUri).collect { connected ->
                if (connected) {
                    observeCurrentTrack()
                    println("🔌 CONECTADO A SPOTIFY SDK :D")
                    _isConnected.value = true
                } else {
                    // Si falla, cancelamos la observación de track.
                    trackJob?.cancel()
                    _isConnected.value = false
                    _currentTrack.value = null
                    println("🔌 NO CONECTADO A SPOTIFY SDK… retry")

                }
            }
        }
    }

    fun observeCurrentTrack() {
        trackJob?.cancel()
        trackJob = viewModelScope.launch {
            spotifyManager
                .getCurrentlyPlayingTrack()
                .collect { trackInfo ->
                    _currentTrack.value = trackInfo.copy(
                        imageUri = spotifyManager.imageUrl(trackInfo.imageUri)
                    )
                }
        }
    }

    fun disconnectFromSpotify() {
        trackJob?.cancel()
        trackJob = null

        spotifyManager.disconnect()

        _isConnected.value = false
        _currentTrack.value = null
    }

    override fun onCleared() {
        super.onCleared()
        connectionJob?.cancel() // Asegurar la limpieza del Job de conexión
        disconnectFromSpotify()
    }
}
