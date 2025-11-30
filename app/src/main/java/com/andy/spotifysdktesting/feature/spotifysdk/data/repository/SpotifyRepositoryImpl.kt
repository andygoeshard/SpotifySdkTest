package com.andy.spotifysdktesting.feature.spotifysdk.data.repository

import android.util.Log
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.Track
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.SpotifyRepository
import com.andy.spotifysdktesting.feature.spotifysdk.domain.service.SpotifyApiService
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "SpotifyRepo"

class SpotifyRepositoryImpl(
    private val api: SpotifyApiService
) : SpotifyRepository {

    // ----------------------------------------------------------------------
    // PARSERS REUTILIZABLES
    // ----------------------------------------------------------------------

    /**
     * Parsea un solo objeto JSON de canción de Spotify.
     */
    private fun parseSingleTrack(trackJson: JSONObject): Track {
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

    /**
     * Parsea una lista de canciones donde el array de items está en un campo específico (ej. "tracks").
     */
    private fun parseItemsArray(json: String, rootKey: String? = null): List<Track> {
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
    // IMPLEMENTACIÓN DE ENDPOINTS
    // ----------------------------------------------------------------------

    override suspend fun searchTracks(query: String): List<Track> {
        return try {
            val response = api.searchTracks(query)
            parseItemsArray(response, rootKey = "tracks")
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando tracks: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getTrack(id: String): Track {
        val json = JSONObject(api.getTrack(id))
        return parseSingleTrack(json)
    }

    override suspend fun getTrackUriFromSearch(query: String): String? {
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
    ): List<Track> {
        return try {
            val response = api.getRecommendations(seedTracks, seedArtists, seedGenres)
            val json = JSONObject(response)
            val items = json.getJSONArray("tracks")

            // El endpoint de recommendations devuelve un array simple de tracks
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

    // 🚀 IMPLEMENTACIÓN DE LOS NUEVOS ENDPOINTS PARA EL DJ

    override suspend fun getTopTracks(limit: Int, timeRange: String): List<Track> {
        return try {
            val response = api.getTopTracks(limit, timeRange)
            // Top Tracks devuelve un array simple en la clave "items" del root
            parseItemsArray(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo Top Tracks: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getRecentlyPlayed(limit: Int): List<Track> {
        return try {
            val response = api.getRecentlyPlayed(limit)
            val root = JSONObject(response)
            val items = root.getJSONArray("items")

            // Recently Played tiene la canción dentro del campo "track" de cada item
            return (0 until items.length()).mapNotNull { i ->
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