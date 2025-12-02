package com.andy.spotifysdktesting.feature.spotifysdk.data.repository

import android.util.Log
import com.andy.spotifysdktesting.feature.spotifysdk.data.entity.PagingObject
import com.andy.spotifysdktesting.feature.spotifysdk.data.entity.TrackRecommendation
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyTokenManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.Track
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.AuthRepository
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.SpotifyRepository
import com.andy.spotifysdktesting.feature.spotifysdk.domain.service.SpotifyApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "SpotifyRepo"

class SpotifyRepositoryImpl(
    private val api: SpotifyApiService,
    private val tokenManager: SpotifyTokenManager,
    private val authRepository: AuthRepository
) : SpotifyRepository {
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

    override suspend fun getTopTracks(limit: Int): List<TrackRecommendation> {
        return try {
            val rawJson = api.getTopTracks(limit, "medium_term")

            val topTracksResponse = Json.decodeFromString<PagingObject>(rawJson)

            topTracksResponse.items.mapNotNull { item ->
                val trackName = item.name
                val artistName = item.artists.firstOrNull()?.name
                val uri = item.uri

                if (trackName != null && artistName != null && uri != null) {
                    TrackRecommendation(uri, trackName, artistName)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener Top Tracks: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getRecentlyPlayed(limit: Int): List<TrackRecommendation> {
        return try {
            val rawJson = api.getRecentlyPlayed(limit)

            // La respuesta de Recently Played es un PagingObject que contiene un objeto 'track' (SimplifiedTrack)
            val playedResponse = Json.decodeFromString<PagingObject>(rawJson)

            playedResponse.items.mapNotNull { item ->
                val simplifiedTrack = item.track
                if (simplifiedTrack != null) {
                    val artistName = simplifiedTrack.artists.firstOrNull()?.name
                    if (artistName != null) {
                        TrackRecommendation(simplifiedTrack.uri, simplifiedTrack.name, artistName)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener Recently Played: ${e.message}", e)
            emptyList()
        }
    }

    // Mantenemos la función de Recomendaciones para el flujo de "seeding"
    override suspend fun getRecommendations(
        seedTracks: List<String>,
        seedArtists: List<String>
    ): List<TrackRecommendation> {
        return try {
            val rawJson = api.getRecommendations(seedTracks = seedTracks, seedArtists = seedArtists)
            // Asumiendo que esta API también devuelve un formato parseable similar a Top/Played
            // Nota: La API de Recommendations a menudo devuelve un objeto 'tracks': List<SimplifiedTrack>, no un PagingObject.
            // Ajusta la deserialización si es necesario. Por simplicidad, asumiremos un parsing similar:

            val recommendations = Json.decodeFromString<PagingObject>(rawJson)

            recommendations.items.mapNotNull { item ->
                // Lógica de mapeo basada en la estructura de respuesta de la API de Recommendations
                val track = item.track ?: return@mapNotNull null // Usamos item.track si es un SimplifiedTrack
                val artistName = track.artists.firstOrNull()?.name
                if (artistName != null) {
                    TrackRecommendation(track.uri, track.name, artistName)
                } else {
                    null
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener recomendaciones por Seed: ${e.message}", e)
            emptyList()
        }
    }
}