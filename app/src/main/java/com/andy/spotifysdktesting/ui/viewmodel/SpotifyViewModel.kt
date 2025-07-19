package com.andy.spotifysdktesting.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.data.CurrentTrack
import com.andy.spotifysdktesting.service.SpotifyManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpotifyViewModel(application: Application) : AndroidViewModel(application) {

    val spotifyManager = SpotifyManager(application.applicationContext)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _currentTrack = MutableStateFlow<CurrentTrack?>(null)
    val currentTrack: StateFlow<CurrentTrack?> = _currentTrack

    private val clientId: String = "YOUR_CLIENT_ID"
    private val redirectUri: String = "YOUR_REDIRECT_URI"

    fun connectToSpotify() {
        viewModelScope.launch {
                spotifyManager.connect(clientId, redirectUri).collect { connected ->
                    _isConnected.value = connected
            }
        }
    }

    fun observeCurrentTrack() {
        viewModelScope.launch {
            spotifyManager.getCurrentlyPlayingTrack().collect { trackInfo ->
                _currentTrack.value = trackInfo.copy(
                    imageUri = spotifyManager.imageUrl(trackInfo.imageUri)
                )
            }
        }
    }
    fun disconnectFromSpotify() {
        spotifyManager.disconnect()
        _isConnected.value = false
        _currentTrack.value = null
    }
}
