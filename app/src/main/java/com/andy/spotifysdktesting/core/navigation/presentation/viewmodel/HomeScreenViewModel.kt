package com.andy.spotifysdktesting.core.navigation.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.core.ai.presentation.viewmodel.AiState
import com.andy.spotifysdktesting.core.ai.presentation.viewmodel.AiViewModel
import com.andy.spotifysdktesting.core.navigation.domain.DjStateManager
import com.andy.spotifysdktesting.core.service.DjService
import com.andy.spotifysdktesting.core.tts.presentation.state.TtsState
import com.andy.spotifysdktesting.core.tts.presentation.viewmodel.TtsViewModel
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyAuthState
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyAuthViewModel
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyState
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow

data class HomeViewState(
    val spotifyState: SpotifyState,
    val aiState: AiState,
    val djState: TtsState,
    val authState: SpotifyAuthState,
    val djText: String,
)

sealed class HomeViewModelIntent {
    data object StartLogin : HomeViewModelIntent()
    data class OnSpotifyCodeReceived(val code: String) : HomeViewModelIntent()

    data object AskAiForNextSong : HomeViewModelIntent()
    data object DjExplainSong : HomeViewModelIntent()
    data class SendAiChat(val message: String) : HomeViewModelIntent()

    data object OnNextSong : HomeViewModelIntent()
    data object OnPreviousSong : HomeViewModelIntent()
    data object OnPlay : HomeViewModelIntent()
    data object OnPause : HomeViewModelIntent()
}

sealed class HomeEvent {
    data object NavigateToLogin : HomeEvent()
    data class ShowSnackbar(val message: String) : HomeEvent()
}

class HomeViewModel(
    private val spotify: SpotifyViewModel,
    private val ai: AiViewModel,
    private val tts: TtsViewModel,
    private val auth: SpotifyAuthViewModel,
    private val context: Context,
    private val djStateManager: DjStateManager // 🎯 INYECCIÓN
) : ViewModel() {

    private val _event = Channel<HomeEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    init {
        println("CREADO hashito HomeViewModel >>> ${System.identityHashCode(this)}")
        observeLoginStatus()
    }

    val state: StateFlow<HomeViewState> = combine(
        spotify.spotifyState,
        ai.uiState,
        tts.state,
        auth.uiState,
        djStateManager.currentDjText // 🎯 COMBINAR EL ESTADO DEL DJ
    ) { spotify, ai, tts, auth, djText ->
        HomeViewState(spotify, ai, tts, auth, djText)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeViewState(
            spotify.spotifyState.value,
            ai.uiState.value,
            tts.state.value,
            auth.uiState.value,
            djStateManager.currentDjText.value // 🎯 VALOR INICIAL
        )
    )

    fun processIntent(intent: HomeViewModelIntent) {
        when (intent) {
            is HomeViewModelIntent.StartLogin -> auth.startLogin()
            is HomeViewModelIntent.OnSpotifyCodeReceived -> handleCodeReceived(intent.code)

            is HomeViewModelIntent.AskAiForNextSong -> sendServiceCommand(DjService.ACTION_NEXT_TRACK_IA)
            is HomeViewModelIntent.DjExplainSong -> sendServiceCommand(DjService.ACTION_EXPLAIN_TRACK)
            is HomeViewModelIntent.SendAiChat -> ai.chat(intent.message)

            is HomeViewModelIntent.OnNextSong -> spotify.skipNext()
            is HomeViewModelIntent.OnPreviousSong -> spotify.skipPrevious()
            is HomeViewModelIntent.OnPlay -> spotify.resume()
            is HomeViewModelIntent.OnPause -> spotify.pause()
        }
    }

    private fun startDjService() {
        println("🚀 Iniciando DjService (Cerebro en Background)...")
        val intent = Intent(context, DjService::class.java).apply {
            action = DjService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun sendServiceCommand(action: String) {
        val intent = Intent(context, DjService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }

    private fun handleCodeReceived(code: String) {
        println("🔔 HOMEVIEWMODEL RECIBIÓ CODE: $code")
        auth.onCodeReceived(code)
    }

    private fun observeLoginStatus() {
        viewModelScope.launch {
            auth.uiState.collect { authState ->
                if (authState.isLoggedIn) {
                    if (!spotify.spotifyState.value.isConnected) {
                        println("🔌 CONECTANDO A SPOTIFY SDK…")
                        this@HomeViewModel.spotify.connectToSpotify()
                    }
                    startDjService()
                }
            }
        }
    }
}