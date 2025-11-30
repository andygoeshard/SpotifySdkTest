package com.andy.spotifysdktesting.core.navigation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.core.ai.presentation.viewmodel.AiState
import com.andy.spotifysdktesting.core.ai.presentation.viewmodel.AiViewModel
import com.andy.spotifysdktesting.core.tts.presentation.intent.TtsEvent
import com.andy.spotifysdktesting.core.tts.presentation.state.TtsState
import com.andy.spotifysdktesting.core.tts.presentation.viewmodel.TtsViewModel
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyAuthState
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyAuthViewModel
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyState
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

data class HomeViewState(
    val spotifyState: SpotifyState,
    val aiState: AiState,
    val djState: TtsState,
    val authState: SpotifyAuthState,
)

// 2. LAS INTENCIONES (Intent)
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

// ----------------------------------------------------------------------
// 3. EL VIEWMODEL (El Motor MVI)
class HomeViewModel(
    private val spotify: SpotifyViewModel,
    private val ai: AiViewModel,
    private val tts: TtsViewModel,
    private val auth: SpotifyAuthViewModel
) : ViewModel() {

    // 🎯 NUEVO: CONTADOR Y CICLO DEL DJ
    private var songCounter: Int = 0
    private val DJ_CYCLE_LENGTH = 3
    private var lastTrackUri: String? = null

    init {
        println("CREADO hashito HomeViewModel >>> ${System.identityHashCode(this)}")
        observeLoginStatus()
        observeCurrentTrackChanges() // 💡 Motor del ciclo DJ
    }

    // 🎯 ÚNICO PUNTO DE VERDAD
    val state: StateFlow<HomeViewState> = combine(
        spotify.spotifyState,
        ai.uiState,
        tts.state,
        auth.uiState
    ) { spotify, ai, tts, auth ->
        HomeViewState(spotify, ai, tts, auth)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeViewState(
            spotify.spotifyState.value,
            ai.uiState.value,
            tts.state.value,
            auth.uiState.value
        )
    )

    // ----------------------------------------------------------------------
    // PROCESAMIENTO DE INTENT
    fun processIntent(intent: HomeViewModelIntent) {
        when (intent) {
            is HomeViewModelIntent.StartLogin -> auth.startLogin()
            is HomeViewModelIntent.OnSpotifyCodeReceived -> handleCodeReceived(intent.code)

            is HomeViewModelIntent.AskAiForNextSong -> askAiForNextSong()
            is HomeViewModelIntent.DjExplainSong -> djExplainCurrentSong()
            is HomeViewModelIntent.SendAiChat -> ai.chat(intent.message)

            is HomeViewModelIntent.OnNextSong -> spotify.skipNext()
            is HomeViewModelIntent.OnPreviousSong -> spotify.skipPrevious()
            is HomeViewModelIntent.OnPlay -> spotify.resume()
            is HomeViewModelIntent.OnPause -> spotify.pause()
        }
    }

    // ----------------------------------------------------------------------
    // LÓGICA DEL DJ AUTOMÁTICO

    private fun askAiForNextSong()= viewModelScope.launch {
        // 1. Pedir a la IA la canción (solo sugerencia)
        ai.startAi("cambiame el mood, rompeme la caja")
        // 2. Opcional: Si quieres que hable INMEDIATAMENTE después de la sugerencia:
        triggerDjSequence(isImmediateSuggestion = true)
    }

    private fun observeCurrentTrackChanges() {
        viewModelScope.launch {
            spotify.spotifyState.collect { state ->

                // 🛑 CORRECCIÓN FINAL: Usamos los campos exactos de CurrentTrack.
                val currentTrack = state.currentTrack

                val currentId = if (currentTrack != null) {
                    // Combinamos Artista y Título para obtener un identificador único
                    // Ej: "Duki | Goteo"
                    "${currentTrack.artistName} | ${currentTrack.trackName}"
                } else {
                    null
                }

                // 1. Si el ID es nulo, vacío, o no hay cambio, terminamos.
                if (currentId.isNullOrBlank() || currentId == lastTrackUri) {
                    return@collect
                }

                // 2. CAMBIO DE CANCIÓN DETECTADO
                println("🎵 Cambio de canción detectado: $currentId (Anterior: $lastTrackUri)")
                lastTrackUri = currentId

                // 3. Incrementar el contador y chequear la interrupción
                checkDjInterruption()
            }
        }
    }

    private fun checkDjInterruption() {
        songCounter++

        println("🎶 Contador de canciones: $songCounter / $DJ_CYCLE_LENGTH")

        if (songCounter >= DJ_CYCLE_LENGTH) {
            println("🚨 CICLO CUMPLIDO. Iniciando Interrupción del DJ.")
            songCounter = 0
            triggerDjSequence()
        }
    }

    private fun triggerDjSequence(isImmediateSuggestion: Boolean = false) = viewModelScope.launch {

        val reason: String = if (!isImmediateSuggestion) {
            ai.describeActualSong()
        } else {
            ai.uiState.value.aiReason
        }

        if (reason.isBlank()) {
            println("⚠️ Razón de IA vacía, saltando narración y continuando.")
            spotify.resume()
            return@launch
        }

        // A. Pausar la música
        spotify.pause()

        // B. Narrar la razón
        println("🎤 DJ Narrando: $reason")

        tts.onEvent(TtsEvent.SpeakText(reason))
        tts.awaitSpeakCompletion()

        // C. Reanudar la reproducción
        spotify.resume()
    }

    private fun djExplainCurrentSong() {
        triggerDjSequence()
    }

    // ----------------------------------------------------------------------
    // LÓGICA INTERNA Y GESTIÓN DE FLUJOS (Sin cambios)

    private fun handleCodeReceived(code: String) {
        println("🔔 HOMEVIEWMODEL RECIBIÓ CODE: $code")
        auth.onCodeReceived(code)
    }
    private fun observeLoginStatus() {
        viewModelScope.launch {
            auth.uiState.collect { authState ->
                if (authState.isLoggedIn && !spotify.spotifyState.value.isConnected) {
                    println("🔌 CONECTANDO A SPOTIFY SDK…")
                    this@HomeViewModel.spotify.connectToSpotify()
                }
            }
        }
    }
}