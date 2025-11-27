package com.andy.spotifysdktesting.core.tts.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.core.tts.domain.TtsManager
import com.andy.spotifysdktesting.core.tts.domain.TtsResult
import com.andy.spotifysdktesting.core.tts.domain.TtsVoice
import com.andy.spotifysdktesting.core.tts.playback.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DjViewModel(
    private val tts: TtsManager,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _state = MutableStateFlow(DjState())
    val state: StateFlow<DjState> = _state

    fun onEvent(event: DjEvent) {
        when (event) {
            is DjEvent.PlayIntro -> speak("Hola Andy, hoy te voy a armar una playlist brutal.")
            is DjEvent.ExplainSong -> speak("Esta canción combina bien con lo que venías escuchando.")
            is DjEvent.CustomText -> speak(event.text)
        }
    }

    private fun speak(text: String) {
        viewModelScope.launch {
            Log.d("DjViewModel", "Iniciando TTS con texto: $text")
            _state.value = _state.value.copy(loading = true)

            val result = tts.speak(
                text = text,
                voice = TtsVoice(id = "JBFqnCBsd6RMkjVDRZzb") // voz default elegante
            )

            _state.value = _state.value.copy(loading = false)

            when (result) {
                is TtsResult.Success -> {
                    Log.d("DjViewModel", "TTS generado correctamente, bytes: ${result.audioBytes.size}")
                    try {
                        audioPlayer.play(result.audioBytes) {
                            Log.d("DjViewModel", "Audio finalizado")
                        }
                    } catch (e: Exception) {
                        Log.e("DjViewModel", "Error reproduciendo audio", e)
                    }
                }
                is TtsResult.Error -> {
                    Log.e("DjViewModel", "Error en TTS: ${result.message}")
                }
            }
        }
    }
}

data class DjState(
    val loading: Boolean = false
)

sealed class DjEvent {
    object PlayIntro : DjEvent()
    object ExplainSong : DjEvent()
    data class CustomText(val text: String) : DjEvent()
}