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

data class SpotifyState(
    val isConnected: Boolean,
    val isPause: Boolean,
    val currentTrack: CurrentTrack?
)

class SpotifyViewModel(
    application: Application,
    private val spotifyManager: SpotifyManager,)
    : AndroidViewModel(application) {
    private val _spotifyState = MutableStateFlow(SpotifyState(
        isConnected = false,
        isPause = true,
        currentTrack = null
    ))
    val spotifyState: StateFlow<SpotifyState> = _spotifyState
    private var trackJob: Job? = null
    private var connectionJob: Job? = null
    private val clientId: String = BuildConfig.SPOTIFY_CLIENT_ID
    private val redirectUri: String = BuildConfig.SPOTIFY_REDIRECT_URI

    fun connectToSpotify() {
        connectionJob?.cancel()

        connectionJob = viewModelScope.launch {
            spotifyManager.connect(clientId, redirectUri).collect { connected ->
                if (connected) {
                    observeCurrentTrack()
                    println("🔌 CONECTADO A SPOTIFY SDK :D")
                } else {
                    trackJob?.cancel()
                    _spotifyState.value = SpotifyState(false, isPause = true, currentTrack = null)
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
                    _spotifyState.value = SpotifyState(true, trackInfo.isPaused,trackInfo.copy(
                        imageUri = spotifyManager.imageUrl(trackInfo.imageUri)
                    ))
                }
        }
    }

    fun disconnectFromSpotify() {
        trackJob?.cancel()
        trackJob = null
        spotifyManager.disconnect()
        _spotifyState.value = SpotifyState(false, true,null)
    }

    override fun onCleared() {
        super.onCleared()
        connectionJob?.cancel()
        disconnectFromSpotify()
    }

    fun resume() {
        spotifyManager.play()
    }

    fun pause() {
        spotifyManager.pause()
    }

    fun skipNext() {
        spotifyManager.next()
    }

    fun skipPrevious() {
        spotifyManager.previous()
    }

    fun playUri(uri: String) {
        spotifyManager.playUri(uri)
    }
}
