package com.andy.spotifysdktesting.core.tts.domain.manager

import com.andy.spotifysdktesting.core.tts.domain.engine.TtsEngineType
import com.andy.spotifysdktesting.core.tts.domain.model.TtsVoice
import com.andy.spotifysdktesting.core.tts.domain.result.TtsResult
import com.andy.spotifysdktesting.core.tts.playback.AudioPlayer
import com.andy.spotifysdktesting.core.tts.domain.state.TtsState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TtsPlaybackManager(
    private val tts: TtsManager,
    private val audioPlayer: AudioPlayer,
    private val scope: CoroutineScope
) {

    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state

    private var completionDeferred: CompletableDeferred<Unit>? = null

    fun speak(text: String) {
        scope.launch {
            _state.value = _state.value.copy(loading = true)

            completionDeferred = CompletableDeferred()

            val result = tts.speak(
                text = text,
                voice = TtsVoice(id = "JBFqnCBsd6RMkjVDRZzb"),
                engine = TtsEngineType.ANDROID_NATIVE
            )

            _state.value = _state.value.copy(loading = false)

            when (result) {
                is TtsResult.Success -> handleSuccess(result)
                is TtsResult.Error -> {
                    completionDeferred?.complete(Unit)
                }
            }
        }
    }

    suspend fun awaitSpeakCompletion() {
        completionDeferred?.await()
    }

    private fun handleSuccess(result: TtsResult.Success) {
        if (result.audioBytes.isNotEmpty()) {
            audioPlayer.play(result.audioBytes) {
                completionDeferred?.complete(Unit)
            }
        } else {
            // TTS nativo ya habló
            completionDeferred?.complete(Unit)
        }
    }

    fun stop() {
        audioPlayer.stop()
        completionDeferred?.complete(Unit)
    }
}
