package com.andy.spotifysdktesting.feature.spotifysdk.data.repository

import android.util.Log
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyTokenManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.Track
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.AuthRepository
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.SpotifyRepository
import com.andy.spotifysdktesting.feature.spotifysdk.domain.service.SpotifyApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "SpotifyRepo"

class SpotifyRepositoryImpl(
    private val api: SpotifyApiService,
    private val tokenManager: SpotifyTokenManager,
    private val authRepository: AuthRepository
) : SpotifyRepository {

    // ----------------------------------------------------------------------
    // 🔐 GESTIÓN DE TOKENS Y LLAMADAS SEGURAS (NUEVO)
    // ----------------------------------------------------------------------

    /**
     * Envuelve una llamada a la API de Spotify. Asegura que el token de acceso
     * sea válido, renovándolo automáticamente si es necesario.
     * * @param block El bloque de código suspendido que ejecuta la llamada a la API.
     * @return El resultado del bloque.
     */
    private suspend fun <T> safeApiCall(block: suspend () -> T): T = withContext(Dispatchers.IO) {

        // 1. Verificar y Renovación (Se ejecuta en AuthRepository.refreshToken)
        if (tokenManager.isAccessTokenExpired()) {
            Log.d(TAG, "🚨 Token expirado o a punto de expirar. Intentando renovación...")
            val success = authRepository.refreshToken()

            if (!success) {
                // Si la renovación falla, lanzamos una excepción para forzar el re-login en la UI
                Log.e(TAG, "🔴 Fallo en la renovación del token.")
                throw Exception("Fallo en la renovación del token de Spotify. Re-login necesario.")
            }
            Log.d(TAG, "✅ Renovación de token exitosa.")
        }

        // 2. Ejecutar la llamada original
        return@withContext block()
    }


    // ----------------------------------------------------------------------
    // PARSERS REUTILIZABLES (Sin cambios)
    // ----------------------------------------------------------------------

    private fun parseSingleTrack(trackJson: JSONObject): Track {
        // ... (Tu implementación original) ...
        val album = trackJson.getJSONObject("album")
        val images = album.getJSONArray("images")
        val imageUrl = if (images.length() > 0) images.getJSONObject(0).getString("url") else ""

        return Track(
            id = trackJson.getString("id"),
            name = trackJson.getString("name"),
            artist = trackJson.getJSONArray("artists").getJSONObject(0).getString("name"),
            image = imageUrl,
            uri = trackJson.getString("uri")
        )
    }

    private fun parseItemsArray(json: String, rootKey: String? = null): List<Track> {
        // ... (Tu implementación original) ...
        val root = JSONObject(json)
        val itemsArray: JSONArray = when {
            rootKey != null && root.has(rootKey) -> root.getJSONObject(rootKey).getJSONArray("items")
            root.has("items") -> root.getJSONArray("items") // Usado por Top Tracks
            else -> return emptyList()
        }

        return (0 until itemsArray.length()).mapNotNull { i ->
            try {
                parseSingleTrack(itemsArray.getJSONObject(i))
            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear item de lista: ${e.message}")
                null
            }
        }
    }

    // ----------------------------------------------------------------------
    // IMPLEMENTACIÓN DE ENDPOINTS (Envueltos en safeApiCall)
    // ----------------------------------------------------------------------

    override suspend fun searchTracks(query: String): List<Track> {
        return safeApiCall { // 💡 Envuelto en safeApiCall
            try {
                val response = api.searchTracks(query)
                parseItemsArray(response, rootKey = "tracks")
            } catch (e: Exception) {
                Log.e(TAG, "Error buscando tracks: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun getTrack(id: String): Track = safeApiCall { // 💡 Envuelto en safeApiCall
        val json = JSONObject(api.getTrack(id))
        return@safeApiCall parseSingleTrack(json)
    }

    override suspend fun getTrackUriFromSearch(query: String): String? {
        // Esta función llama a searchTracks, que ya está envuelta, no necesita envolverse de nuevo.
        val tracks = searchTracks(query)
        val uri = tracks.firstOrNull()?.uri

        if (uri != null) {
            Log.d(TAG, "URI de la canción '$query' encontrada: $uri")
        } else {
            Log.w(TAG, "No se encontró URI para la canción: $query")
        }
        return uri
    }

    override suspend fun getRecommendations(
        seedTracks: List<String>,
        seedArtists: List<String>,
        seedGenres: List<String>
    ): List<Track> = safeApiCall { // 💡 Envuelto en safeApiCall
        try {
            val response = api.getRecommendations(seedTracks, seedArtists, seedGenres)
            val json = JSONObject(response)
            val items = json.getJSONArray("tracks")

            (0 until items.length()).mapNotNull { i ->
                try {
                    parseSingleTrack(items.getJSONObject(i))
                } catch (e: Exception) {
                    Log.e(TAG, "Error al parsear item de recommendations: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo recomendaciones: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getTopTracks(limit: Int, timeRange: String): List<Track> = safeApiCall { // 💡 Envuelto en safeApiCall
        try {
            val response = api.getTopTracks(limit, timeRange)
            parseItemsArray(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo Top Tracks: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getRecentlyPlayed(limit: Int): List<Track> = safeApiCall { // 💡 Envuelto en safeApiCall
        try {
            val response = api.getRecentlyPlayed(limit)
            val root = JSONObject(response)
            val items = root.getJSONArray("items")

            // Recently Played tiene la canción dentro del campo "track" de cada item
            return@safeApiCall (0 until items.length()).mapNotNull { i ->
                try {
                    val item = items.getJSONObject(i)
                    val trackJson = item.getJSONObject("track")
                    parseSingleTrack(trackJson)
                } catch (e: Exception) {
                    Log.e(TAG, "Error al parsear item de Recently Played: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo Recently Played: ${e.message}")
            emptyList()
        }
    }
}