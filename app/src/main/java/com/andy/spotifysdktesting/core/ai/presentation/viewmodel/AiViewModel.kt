package com.andy.spotifysdktesting.core.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.core.ai.domain.AiMusicBrain
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class AiViewModel(
    private val ai: AiMusicBrain,
    private val spotify: SpotifyManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiState())
    val uiState: StateFlow<AiState> = _uiState

    fun startAi(mood: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true)

        val raw = ai.chooseNextSong(
            mood,
            currentTrack = spotify.currentTrackCache
        )

        runCatching {
            val json = JSONObject(raw)
            val song = json.getString("next_song")
            val reason = json.getString("reason")

            _uiState.value = _uiState.value.copy(
                loading = false,
                aiSong = song,
                aiReason = reason,
                aiRaw = raw
            )
        }.onFailure {
            _uiState.value = _uiState.value.copy(
                loading = false,
                aiRaw = raw
            )
        }
    }

    fun chat(message: String) = viewModelScope.launch {
        val response = ai.chat(message)
        _uiState.value = _uiState.value.copy(chatResponse = response)
    }
}

data class AiState(
    val loading: Boolean = false,
    val aiSong: String = "",
    val aiReason: String = "",
    val aiRaw: String = "",
    val chatResponse: String = ""
)
