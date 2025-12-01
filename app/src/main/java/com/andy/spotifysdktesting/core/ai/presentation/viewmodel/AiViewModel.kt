package com.andy.spotifysdktesting.core.ai.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.core.ai.domain.AiMusicBrain
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyTokenManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.SpotifyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "AiViewModel"

class AiViewModel(
    private val ai: AiMusicBrain,
    private val spotifyManager: SpotifyManager,
    private val spotifyRepository: SpotifyRepository,
    private val tokenManager: SpotifyTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiState())
    val uiState: StateFlow<AiState> = _uiState

    // 💡 MODIFICADO: Ahora es suspend y retorna la razón (String) para evitar la carrera.
    suspend fun startAi(mood: String): String {
        if (tokenManager.getAccessToken() == null) {
            Log.e(TAG, "No hay token de Spotify. Login requerido.")
            _uiState.value = _uiState.value.copy(
                loading = false,
                chatResponse = "Error: Inicia sesión con Spotify."
            )
            return "Error: Inicia sesión con Spotify." // 👈 Retorno anticipado (es válido)
        }

        _uiState.value = _uiState.value.copy(loading = true)

        val rawResponse = ai.chooseNextSong(
            mood,
            currentTrack = spotifyManager.currentTrackCache
        )

        // 1. Ejecución con runCatching
        val result = runCatching {
            val cleanedJsonString = rawResponse
                .replace("```json", "")
                .replace("```", "")
                .trim()

            Log.d(TAG, "JSON limpio para parsear: \n$cleanedJsonString")

            // 1a. Parsear la respuesta de IA
            val json = JSONObject(cleanedJsonString)
            val rawSongString = json.getString("song")
            val reason = json.getString("reason") // 👈 RAZÓN NUEVA

            // 1b. Estrategia de Búsqueda y Reproducción (NO debe retornar aquí)
            var trackUri: String? = null
            // ... (Toda la lógica de búsqueda de URI se mantiene igual) ...

            // 🚨 IMPORTANTE: Mantenemos el código de búsqueda y reproducción dentro de este bloque,
            // pero eliminamos el 'return' explícito dentro de runCatching.

            val parts = rawSongString.split(" - ", limit = 2)
            if (parts.size == 2) {
                val artist = parts[0].trim()
                val track = parts[1].trim()
                val flexibleQuery = "artist:$artist track:$track"
                Log.d(TAG, "Búsqueda INTENTO 1 (Flexible): $flexibleQuery")
                trackUri = spotifyRepository.getTrackUriFromSearch(flexibleQuery)
            }
            if (trackUri == null) {
                Log.d(TAG, "Búsqueda INTENTO 2 (Cruda): $rawSongString")
                trackUri = spotifyRepository.getTrackUriFromSearch(rawSongString)
            }
            if (trackUri == null && parts.size == 2) {
                val invertedArtist = parts[1].trim()
                val invertedTrack = parts[0].trim()
                val invertedQuery = "artist:$invertedArtist track:$invertedTrack"
                Log.d(TAG, "Búsqueda INTENTO 3 (Invertida): $invertedQuery")
                trackUri = spotifyRepository.getTrackUriFromSearch(invertedQuery)
            }

            // 3. Reproducir la canción (si se encuentra la URI)
            if (trackUri != null) {
                Log.d(TAG, "✅ ÉXITO: Reproduciendo URI sugerida por la IA: $trackUri")
                spotifyManager.playUri(trackUri)
            } else {
                Log.e(
                    TAG,
                    "🔴 ERROR FATAL: No se pudo obtener la URI tras ambos intentos para: $rawSongString"
                )
            }

            // 4. Actualizar el estado de la UI
            _uiState.value = _uiState.value.copy(
                loading = false,
                aiSong = rawSongString,
                aiReason = reason,
                aiRaw = rawResponse,
                chatResponse = ""
            )

            reason // 👈 Esto es el último valor del bloque 'runCatching', lo que retorna si es exitoso.
        }
        return result.getOrElse { e ->
            Log.e(TAG, "🔴 ERROR FATAL al procesar la sugerencia de IA o reproducir: ${e.message}", e)
            _uiState.value = _uiState.value.copy(
                loading = false,
                aiRaw = rawResponse,
                chatResponse = "Fallo en la IA o la búsqueda: ${e.message}"
            )
            "Fallo en la IA: ${e.message}" // Retorna error para TTS
        }
    }


    fun chat(message: String) = viewModelScope.launch {
        val response = ai.chat(message)
        _uiState.value = _uiState.value.copy(chatResponse = response)
    }

    // ... (la función describeActualSong se mantiene tal cual, ya retorna la razón) ...
    suspend fun describeActualSong(): String {
        val currentTrack = spotifyManager.getCurrentlyPlayingTrack().first()
        _uiState.value = _uiState.value.copy(loading = true)
        val result = runCatching {
            val rawResponse = ai.describeActualSong(currentTrack)
            val json = JSONObject(rawResponse.replace("```json", "").replace("```", "").trim())
            val reason = json.getString("reason")
            _uiState.value = _uiState.value.copy(loading = false, aiReason = reason)
            return reason
        }
        return result.getOrElse { e ->
            _uiState.value = _uiState.value.copy(loading = false)
            "El DJ tuvo un error y no pudo describir la canción."
        }
    }

}

data class AiState(
    val loading: Boolean = false,
    val aiSong: String = "",
    val aiReason: String = "",
    val aiRaw: String = "",
    val chatResponse: String = ""
)