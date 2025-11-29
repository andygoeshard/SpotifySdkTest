package com.andy.spotifysdktesting.core.navigation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.core.ai.presentation.viewmodel.AiState
import com.andy.spotifysdktesting.core.ai.presentation.viewmodel.AiViewModel
import com.andy.spotifysdktesting.core.tts.presentation.viewmodel.DjEvent
import com.andy.spotifysdktesting.core.tts.presentation.viewmodel.DjState
import com.andy.spotifysdktesting.core.tts.presentation.viewmodel.DjViewModel
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyAuthViewModel
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val spotify: SpotifyViewModel,
    private val ai: AiViewModel,
    private val dj: DjViewModel,
    private val auth: SpotifyAuthViewModel
) : ViewModel() {

    init {
        println("CREADO hashito HomeViewModel >>> ${System.identityHashCode(this)}")
    }

    val isSdkConnected: StateFlow<Boolean> = spotify.isConnected
    val isLoggedIn: StateFlow<Boolean> =
        auth.uiState.map { it.isLoggedIn }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val authUrl: StateFlow<String> =
        auth.uiState.map { it.authUrl }
            .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val currentTrack = spotify.currentTrack
    val aiState = ai.uiState
    val djState = dj.state

    fun startLogin() {
        auth.startLogin()
    }

    fun onSpotifyCodeReceived(code: String) {
        println("🔔 HOMEVIEWMODEL RECIBIÓ CODE: $code")

        auth.onCodeReceived(code)

        viewModelScope.launch {
            auth.uiState.collect { state ->
                println("STATE CAMBIÓ: $state")
                if (auth.uiState.value.isLoggedIn) {
                    println("🔌 CONECTANDO A SPOTIFY SDK…")
                    spotify.connectToSpotify()
                    spotify.connectToSpotify()
                }
            }
        }
    }

    fun askAiForNextSong(mood: String) {
        ai.startAi(mood)
    }

    fun djExplainSong() {
        dj.onEvent(DjEvent.ExplainSong)
    }

    fun djCustom(text: String) {
        dj.onEvent(DjEvent.CustomText(text))
    }
}
