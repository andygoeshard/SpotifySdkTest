package com.andy.spotifysdktesting.core.ai.presentation.viewmodel

import android.util.Log.v
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Log
import com.andy.spotifysdktesting.core.ai.domain.AiMusicBrain
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyTokenManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.SpotifyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    fun startAi(mood: String) = viewModelScope.launch {
        if (tokenManager.getAccessToken() == null) {
            Log.e(TAG, "No hay token de Spotify. Login requerido.")
            _uiState.value = _uiState.value.copy(loading = false, chatResponse = "Error: Inicia sesión con Spotify.")
            return@launch
        }

        _uiState.value = _uiState.value.copy(loading = true)

        val rawResponse = ai.chooseNextSong(
            mood,
            currentTrack = spotifyManager.currentTrackCache
        )

        // 🎯 FUNCIÓN DE LIMPIEZA
        val cleanedJsonString = rawResponse
            .replace("```json", "")
            .replace("```", "")
            .trim()

        Log.d(TAG, "JSON limpio para parsear: \n$cleanedJsonString")

        runCatching {
            // 1. Parsear la respuesta de Gemini
            val json = JSONObject(cleanedJsonString)
            val rawSongString = json.getString("song")
            val reason = json.getString("reason")

            // 🎯 ESTRATEGIA DE BÚSQUEDA DOBLE
            var trackUri: String? = null

            // Intento 1: Query Optimizada SIN comillas (más flexible para Spotify)
            val parts = rawSongString.split(" - ", limit = 2)
            if (parts.size == 2) {
                val artist = parts[0].trim()
                val track = parts[1].trim()
                // ⚠️ QUITAMOS LAS COMILLAS
                val flexibleQuery = "artist:$artist track:$track"

                Log.d(TAG, "Búsqueda INTENTO 1 (Flexible): $flexibleQuery")
                trackUri = spotifyRepository.getTrackUriFromSearch(flexibleQuery)
            }

            // Intento 2: Si el primer intento falló, usamos la cadena cruda de Gemini.
            if (trackUri == null) {
                Log.d(TAG, "Búsqueda INTENTO 2 (Cruda): $rawSongString")
                trackUri = spotifyRepository.getTrackUriFromSearch(rawSongString)
            }
            if (trackUri == null && parts.size == 2) {
                val invertedArtist = parts[1].trim() // Tema se convierte en Artista
                val invertedTrack = parts[0].trim()  // Artista se convierte en Tema
                val invertedQuery = "artist:$invertedArtist track:$invertedTrack"

                Log.d(TAG, "Búsqueda INTENTO 3 (Invertida): $invertedQuery")
                trackUri = spotifyRepository.getTrackUriFromSearch(invertedQuery)
            }

            // 3. Reproducir la canción (si se encuentra la URI)
            if (trackUri != null) {
                Log.d(TAG, "✅ ÉXITO: Reproduciendo URI sugerida por la IA: $trackUri")
                spotifyManager.playUri(trackUri)
            } else {
                Log.e(TAG, "🔴 ERROR FATAL: No se pudo obtener la URI tras ambos intentos para: $rawSongString")
            }

            // 4. Actualizar el estado de la UI
            _uiState.value = _uiState.value.copy(
                loading = false,
                aiSong = rawSongString,
                aiReason = reason,
                aiRaw = rawResponse,
                chatResponse = ""
            )
        }.onFailure { e ->
            Log.e(TAG, "🔴 ERROR FATAL al procesar la sugerencia de IA o reproducir: ${e.message}", e)
            _uiState.value = _uiState.value.copy(
                loading = false,
                aiRaw = rawResponse,
                chatResponse = "Fallo en la IA o la búsqueda: ${e.message}"
            )
        }
    }

    fun chat(message: String) = viewModelScope.launch {
        val response = ai.chat(message)
        _uiState.value = _uiState.value.copy(chatResponse = response)
    }

    suspend fun describeActualSong(): String {

        // 1. Obtener la canción actual de forma sincronizada usando first()
        // El uso de .first() garantiza que esperamos el valor más reciente del Flow y luego continuamos.
        val currentTrack = spotifyManager.getCurrentlyPlayingTrack().first()

        // 1b. Validaciones de nulidad y estado inicial (IMPORTANTE)
        if (tokenManager.getAccessToken() == null) {
            return "Error: Inicia sesión con Spotify para la descripción del DJ."
        }
        if (currentTrack == null) {
            Log.e(TAG, "No hay canción en reproducción o en caché.")
            return "El DJ necesita que haya una canción en reproducción."
        }

        // 2. Ejecución suspendida con manejo de errores (runCatching)
        _uiState.value = _uiState.value.copy(loading = true)

        val result = runCatching {
            // Llama a la capa de dominio (debe ser suspendida y retornar la respuesta cruda)
            val rawResponse = ai.describeActualSong(currentTrack)

            // 💡 Depuración: Loggeamos la respuesta cruda para ver qué devuelve Gemini
            Log.d(TAG, "Respuesta CRUDA de Gemini: \n$rawResponse")

            // Asumiendo el parseo JSON
            val cleanedJsonString = rawResponse
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // 💡 Depuración: Loggeamos la respuesta LIMPIA antes de parsear
            Log.d(TAG, "Respuesta LIMPIA para JSON: \n$cleanedJsonString")

            // Esto fallará si la respuesta no es un JSON válido o si falta la clave 'reason'
            val json = JSONObject(cleanedJsonString)
            val reason = json.getString("reason")

            // 3. Actualizar el estado para la UI/Chat
            _uiState.value = _uiState.value.copy(
                loading = false,
                chatResponse = reason // Puedes usar aiReason o chatResponse, elije uno
            )

            return reason // ✅ Retorna la razón
        }

        // 4. Manejo de Errores (Si falla la red, la API o el parseo JSON)
        return result.getOrElse { e ->
            Log.e(TAG, "🔴 ERROR al describir la canción con IA: ${e.message}", e)

            // Actualizamos el estado con el error
            _uiState.value = _uiState.value.copy(
                loading = false,
                chatResponse = "El DJ falló: ${e.message}"
            )
            // Retornamos un mensaje de error que será reproducido por el TTS
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