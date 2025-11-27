package com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.feature.spotifysdk.data.CurrentTrack
import com.andy.spotifysdktesting.feature.spotifysdk.domain.service.SpotifyManager
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


    private val clientId: String = "tu_client_id_real"
    private val redirectUri: String = "myapp://callback"

    fun connectToSpotify() {
        viewModelScope.launch {
                spotifyManager.connect(clientId, redirectUri).collect { connected ->
                    _isConnected.value = connected
                    return@collect
            }
            spotifyManager.connect(clientId, redirectUri).collect { connected ->
                _isConnected.value = connected
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
}
