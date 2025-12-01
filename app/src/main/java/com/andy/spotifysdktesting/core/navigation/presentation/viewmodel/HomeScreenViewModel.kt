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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

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

// 🎯 EVENTOS: Para acciones únicas (ej. navegación)
sealed class HomeEvent {
    data object NavigateToLogin : HomeEvent()
    data class ShowSnackbar(val message: String) : HomeEvent()
}

class HomeViewModel(
    private val spotify: SpotifyViewModel,
    private val ai: AiViewModel,
    private val tts: TtsViewModel,
    private val auth: SpotifyAuthViewModel
) : ViewModel() {

    // 🎯 CANAL DE EVENTOS: Solo para UI (Navegación, Snackbar, etc.)
    private val _event = Channel<HomeEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

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
        try {
            // 1. Llama a la IA: Pide la próxima canción y el mood. Obtiene la razón (texto).
            val reason = ai.startAi("cambiame el mood, rompeme la caja")
            // 2. 🎯 USA EL VALOR RETORNADO: Orquesta la narración.
            triggerDjSequence(reason)
        } catch (e: Exception) {
            handleAuthException(e) // 💡 Manejar el fallo de token/API
        }
    }

    private fun djExplainCurrentSong() = viewModelScope.launch {
        try {
            // 1. Llama a la IA: Pide la explicación de la canción actual. Obtiene la razón (texto).
            val reason = ai.describeActualSong()
            // 2. 🎯 USA EL VALOR RETORNADO: Orquesta la narración.
            triggerDjSequence(reason)
        } catch (e: Exception) {
            handleAuthException(e) // 💡 Manejar el fallo de token/API
        }
    }

    private fun checkDjInterruption() {
        songCounter++
        println("🎶 Contador de canciones: $songCounter / $DJ_CYCLE_LENGTH")

        if (songCounter >= DJ_CYCLE_LENGTH) {
            println("🚨 CICLO CUMPLIDO. Iniciando Interrupción del DJ.")
            songCounter = 0
            viewModelScope.launch {
                try {
                    val reason = ai.describeActualSong()
                    triggerDjSequence(reason)
                } catch (e: Exception) {
                    handleAuthException(e)
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // 🔐 GESTIÓN DE ERRORES DE AUTENTICACIÓN (CLAVE)

    private suspend fun handleAuthException(e: Exception) {
        val errorMessage = e.message ?: ""

        // Asume que SpotifyRepositoryImpl lanza un error con este mensaje si la renovación falla
        if (errorMessage.contains("Re-login necesario", ignoreCase = true)) {
            println("🚨 RENOVACIÓN FALLIDA. Forzando re-login en la UI.")
            // 1. Limpiar el estado local de tokens
            auth.clearTokensAndForceLogin()
            // 2. Notificar a la UI para la navegación
            _event.send(HomeEvent.NavigateToLogin)
        } else {
            // Error de red, TTS, o AI que no requiere re-login. Mostrar un Snackbar.
            _event.send(HomeEvent.ShowSnackbar(errorMessage))
        }
    }

    // ----------------------------------------------------------------------
    // LÓGICA INTERNA Y GESTIÓN DE FLUJOS (Se mantienen)

    // ... (El resto de tus funciones como observeCurrentTrackChanges, etc.)

    private fun observeCurrentTrackChanges() {
        viewModelScope.launch {
            spotify.spotifyState.collect { state ->
                val currentTrack = state.currentTrack
                val currentId = if (currentTrack != null) {
                    "${currentTrack.artistName} | ${currentTrack.trackName}"
                } else {
                    null
                }

                if (currentId.isNullOrBlank() || currentId == lastTrackUri) {
                    return@collect
                }

                println("🎵 Cambio de canción detectado: $currentId (Anterior: $lastTrackUri)")
                lastTrackUri = currentId
                checkDjInterruption()
            }
        }
    }

    private fun triggerDjSequence(reason: String) = viewModelScope.launch {

        if (reason.isBlank() || reason.startsWith("Error:")) {
            println("⚠️ Razón de IA vacía o con error, saltando narración.")
            spotify.resume()
            return@launch
        }
        println("🎤 DJ Narrando: $reason")
        withContext(Dispatchers.IO) {
            tts.onEvent(TtsEvent.SpeakText(reason))
            tts.awaitSpeakCompletion()
        }
        spotify.resume()
    }

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