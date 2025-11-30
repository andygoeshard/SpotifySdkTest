package com.andy.spotifysdktesting.core.tts.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.core.tts.domain.TtsManager
import com.andy.spotifysdktesting.core.tts.domain.TtsResult
import com.andy.spotifysdktesting.core.tts.domain.TtsVoice
import com.andy.spotifysdktesting.core.tts.playback.AudioPlayer
import com.andy.spotifysdktesting.core.tts.presentation.intent.TtsEvent
import com.andy.spotifysdktesting.core.tts.presentation.state.TtsState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class TtsViewModel(
    private val tts: TtsManager,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state
    private var completionDeferred: CompletableDeferred<Unit>? = null

    fun onEvent(event: TtsEvent) {
        when (event) {
            // Mantenemos la llamada a 'speak', pero ahora esta función inicia la espera.
            is TtsEvent.SpeakText -> speak(event.text)
        }
    }

    // 🎯 NUEVA FUNCIÓN PÚBLICA PARA QUE HOMEVIEWMODEL ESPERE.
    // HomeViewModel usará esta función inmediatamente después de llamar a onEvent.
    suspend fun awaitSpeakCompletion() {
        // Esperamos a que el Deferred se complete (es decir, el audio termine).
        completionDeferred?.await()
    }

    // Mantenemos speak como privado o interno, pero con la lógica de Deferred.
    private fun speak(text: String) {
        viewModelScope.launch {
            Log.d("TtsViewModel", "Iniciando TTS con texto: $text")
            _state.value = _state.value.copy(loading = true)

            // 1. Inicializar el Deferred ANTES de empezar el proceso de audio
            completionDeferred = CompletableDeferred()

            val result = tts.speak(
                text = text,
                voice = TtsVoice(id = "JBFqnCBsd6RMkjVDRZzb")
            )

            _state.value = _state.value.copy(loading = false)

            when (result) {
                is TtsResult.Success -> {
                    Log.d("TtsViewModel", "TTS generado, reproduciendo...")
                    try {
                        audioPlayer.play(result.audioBytes) {
                            Log.d("TtsViewModel", "Audio finalizado")
                            // 2. Completamos el Deferred cuando el audio termina.
                            completionDeferred?.complete(Unit)
                        }
                    } catch (e: Exception) {
                        Log.e("TtsViewModel", "Error reproduciendo audio", e)
                        completionDeferred?.complete(Unit) // Completamos si hay error de reproducción
                    }
                }
                is TtsResult.Error -> {
                    Log.e("TtsViewModel", "Error en TTS: ${result.message}")
                    completionDeferred?.complete(Unit) // Completamos si falla la generación de TTS
                }
            }
        }
    }
}


