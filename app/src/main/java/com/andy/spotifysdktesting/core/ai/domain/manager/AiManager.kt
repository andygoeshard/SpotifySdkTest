package com.andy.spotifysdktesting.core.ai.domain.manager

import android.util.Log
import com.andy.spotifysdktesting.core.ai.domain.helper.buildDescribePrompt
import com.andy.spotifysdktesting.core.ai.domain.helper.buildPrompt
import com.andy.spotifysdktesting.core.ai.domain.helper.cleanAiResponse
import com.andy.spotifysdktesting.core.ai.domain.helper.extractReasonFromJson
import com.andy.spotifysdktesting.core.ai.domain.helper.parseSuggestion
import com.andy.spotifysdktesting.core.ai.domain.repository.AiClient
import com.andy.spotifysdktesting.core.ai.domain.state.AiState
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
private const val TAG = "AiManager"
class AiManager(
    private val aiClient: AiClient,
) {
    private val _state = MutableStateFlow(AiState())
    val state: StateFlow<AiState> = _state

    suspend fun chooseNextSong(
        mood: String,
        currentTrack: CurrentTrack?
    ) {
        _state.update { it.copy(loading = true, error = null) }

        runCatching {
            val raw = aiClient.generateContent(buildPrompt(mood, currentTrack))
            val cleaned = cleanAiResponse(raw)
            parseSuggestion(cleaned)
        }.onSuccess { suggestion ->
            _state.update {
                it.copy(
                    loading = false,
                    suggestion = suggestion
                )
            }
        }.onFailure { e ->
            Log.e(TAG, "IA failed", e)
            _state.update {
                it.copy(
                    loading = false,
                    error = "La IA se fue a tomar mate"
                )
            }
        }
    }


    suspend fun describeActualSong(currentTrack: CurrentTrack?) {
        _state.update { it.copy(loading = true, error = null) }

        runCatching {
            val raw = aiClient.generateContent(buildDescribePrompt(currentTrack))
            extractReasonFromJson(cleanAiResponse(raw))
        }.onSuccess { reason ->
            _state.update {
                it.copy(
                    loading = false,
                    description = reason
                )
            }
        }.onFailure {
            _state.update {
                it.copy(
                    loading = false,
                    error = "No pude describir el tema"
                )
            }
        }
    }

    suspend fun chat(message: String) {
        runCatching {
            aiClient.generateContent(message)
        }.onSuccess { response ->
            _state.update {
                it.copy(chatResponse = response)
            }
        }
    }
}