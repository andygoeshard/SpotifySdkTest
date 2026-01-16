package com.andy.spotifysdktesting.core.dj.domain.manager

import android.util.Log
import com.andy.spotifysdktesting.BuildConfig
import com.andy.spotifysdktesting.core.ai.domain.manager.AiManager
import com.andy.spotifysdktesting.core.dj.domain.intent.DjIntent
import com.andy.spotifysdktesting.core.dj.domain.model.NextTrackCache
import com.andy.spotifysdktesting.core.dj.domain.state.DjState
import com.andy.spotifysdktesting.core.tts.domain.manager.TtsPlaybackManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifySdkManager
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.manager.SpotifyAuthManager
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.manager.SpotifyWebManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DjManager"

class DjManager(
    private val spotifySdk: SpotifySdkManager,
    private val spotifyWeb: SpotifyWebManager,
    private val authManager: SpotifyAuthManager,
    private val ai: AiManager,
    private val tts: TtsPlaybackManager,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(DjState())
    val state: StateFlow<DjState> = _state

    private var lastTrackId: String? = null

    private var nextTrackCache: NextTrackCache? = null
    private var songCounter = 0

    private val DJ_CYCLE_LENGTH = 3
    private val MAX_HISTORY_SIZE = 15

    /* ─────────────────────────── INTENTS ─────────────────────────── */

    fun onIntent(intent: DjIntent) {
        when (intent) {
            DjIntent.Start -> enableDj()
            DjIntent.Stop -> disableDj()
            is DjIntent.OnSpotifyCodeReceived -> onSpotifyCodeReceived(intent.code)
            DjIntent.ExplainCurrentSong -> explainCurrentSong()
            DjIntent.NextTrackIA -> playNextTrackFromIA()
            DjIntent.SpotifyTrackChanged -> onSpotifyTrackChanged()
            DjIntent.OnPause -> spotifySdk.pause()
            DjIntent.OnPlay -> spotifySdk.play()
            DjIntent.OnNextTrack -> spotifySdk.next()
            DjIntent.OnPreviousTrack -> spotifySdk.previous()
        }
    }

    /* ─────────────────────────── AUTH ─────────────────────────── */

    init {
        Log.d(TAG, "Dj Manager Inicializado.")
        observeAuth()
        enableDj()
        observeSpotifySdk()
        scope.launch {
            preselectNextTrack()
        }
        Log.d("KOIN_CHECK", "📦 DjManager CREADO. Hash: ${System.identityHashCode(this)}")
    }

    private fun observeAuth() {
        scope.launch {
            authManager.state.collect { authState ->
                _state.update { current ->
                    current.copy(
                        isLoggedIn = authState.isLoggedIn,
                        accessToken = authState.accessToken,
                        authUrl = authState.authUrl
                    )
                }
                if (authState.isLoggedIn && !spotifySdk.state.value.isConnected) {
                    connectSdk()
                }
            }
        }
    }

    private fun connectSdk(){
        spotifySdk.connect(
            clientId = BuildConfig.SPOTIFY_CLIENT_ID,
            redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI
        )
    }

    private fun observeSpotifySdk() {
        scope.launch {
            spotifySdk.state.collect { sdkState ->
                _state.update { current ->
                    current.copy(
                        isSdkConnected = sdkState.isConnected,
                        currentTrack = sdkState.currentTrack
                    )
                }
            }
        }
    }

    fun startLogin() {
        authManager.startLogin()
    }

    fun onSpotifyCodeReceived(code: String) {
        authManager.onCodeReceived(code)
    }

    fun logout() {
        authManager.logout()
    }

    /* ─────────────────────────── CORE FLOW ─────────────────────────── */


    private fun enableDj() {
        _state.update { it.copy(enabled = true) }
        updateDjText("🎧 DJ activado")
    }

    private fun disableDj() {
        _state.value = DjState()
        nextTrackCache = null
        songCounter = 0
        Log.d(TAG, "DJ desactivado")
    }

    private fun onSpotifyTrackChanged() {
        val current = _state.value.currentTrack ?: return

        if (current.id == lastTrackId) return
        lastTrackId = current.id

        if (!_state.value.enabled) return

        songCounter++
        Log.d(TAG, "contador de canciones: $songCounter")

        if (songCounter >= DJ_CYCLE_LENGTH) {
            songCounter = 0
            scope.launch {
                explainCurrentSong()
                delay(2000)
                Log.d(TAG, "Reiniciando ciclo: $songCounter")
            }
        }
    }

    /* ─────────────────────────── DJ SPEAKS ─────────────────────────── */

    private fun explainCurrentSong() {
        scope.launch {
            val current = spotifySdk.state.value.currentTrack ?: return@launch

            setThinking("🎤 DJ preparando explicación…")

            ai.describeActualSong(current)
            val text = ai.state.value.description ?: return@launch

            speak(text)
        }
    }

    private fun playNextTrackFromIA() {
        scope.launch {
            if (nextTrackCache == null) {
                preselectNextTrack()
            }

            val cached = nextTrackCache ?: return@launch
            nextTrackCache = null

            spotifySdk.playUri(cached.uri)
            speak("🎶 ${cached.reason}")
            songCounter = 1
            preselectNextTrack()
        }
    }

    private suspend fun preselectNextTrack() {
        val current = spotifySdk.state.value.currentTrack ?: return

        setThinking("🤖 DJ pensando próxima canción…")

        ai.chooseNextSong(
            mood = "mismo mood y genero",
            currentTrack = current
        )

        val suggestion = ai.state.value.suggestion ?: return
        val uri = spotifyWeb
            .getTrackUri(suggestion.songName)
            .getOrNull()
            ?: return

        nextTrackCache = NextTrackCache(
            uri = uri,
            songName = suggestion.songName,
            reason = suggestion.reason
        )

        _state.update {
            it.copy(
                thinking = false,
                statusText = "⏭ Preparado: ${suggestion.songName}",
                nextTrack = nextTrackCache
            )
        }
    }

    /* ─────────────────────────── TTS ─────────────────────────── */

    private suspend fun speak(text: String) {
        _state.update {
            it.copy(
                thinking = false,
                speaking = true,
                statusText = text
            )
        }

        updateDjText(text)

        tts.speak(text)
        tts.awaitSpeakCompletion()

        _state.update { it.copy(speaking = false) }
    }

    /* ─────────────────────────── STATE HELPERS ─────────────────────────── */

    private fun setThinking(text: String) {
        _state.update {
            it.copy(
                thinking = true,
                statusText = text
            )
        }
        updateDjText(text)
    }

    private fun updateDjText(text: String) {
        _state.update { state ->
            val history = (state.messageHistory + text)
                .takeLast(MAX_HISTORY_SIZE)

            state.copy(
                currentText = text,
                messageHistory = history
            )
        }

        Log.d(TAG, "DJ: $text")
    }
}
