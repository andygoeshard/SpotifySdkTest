package com.andy.spotifysdktesting.core.tts.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.core.tts.domain.TtsEngineType
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
            is TtsEvent.SpeakText -> speak(event.text)
        }
    }

    suspend fun awaitSpeakCompletion() {
        // Esperamos a que el Deferred se complete (es decir, el audio termine).
        completionDeferred?.await()
    }

    private fun speak(text: String) {
        viewModelScope.launch {
            Log.d("TtsViewModel", "Iniciando TTS con texto: $text")
            _state.value = _state.value.copy(loading = true)

            // 1. Inicializar el Deferred ANTES de empezar el proceso de audio
            completionDeferred = CompletableDeferred()

            val result = tts.speak(
                text = text,
                voice = TtsVoice(id = "JBFqnCBsd6RMkjVDRZzb"),
                engine = TtsEngineType.ANDROID_NATIVE
            )

            _state.value = _state.value.copy(loading = false)

            when (result) {
                is TtsResult.Success -> {
                    // 🎯 CLAVE: Comprobamos si el engine devolvió bytes (para AudioPlayer) o un array vacío (nativo).
                    if (result.audioBytes.isNotEmpty()) {
                        Log.d("TtsViewModel", "TTS generado, reproduciendo con AudioPlayer...")
                        try {
                            audioPlayer.play(result.audioBytes) {
                                Log.d("TtsViewModel", "Audio finalizado por AudioPlayer")
                                completionDeferred?.complete(Unit)
                            }
                        } catch (e: Exception) {
                            Log.e("TtsViewModel", "Error reproduciendo audio con AudioPlayer", e)
                            completionDeferred?.complete(Unit)
                        }
                    } else {
                        // El motor TTS nativo ya manejó la reproducción y el foco de audio.
                        Log.d("TtsViewModel", "TTS finalizado por motor nativo (speak()).")
                        completionDeferred?.complete(Unit) // La corrutina se reanuda inmediatamente
                    }
                }
                is TtsResult.Error -> {
                    Log.e("TtsViewModel", "Error en TTS: ${result.message}")
                    completionDeferred?.complete(Unit)
                }
            }
        }
    }
}

