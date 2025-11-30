package com.andy.spotifysdktesting.feature.spotifysdk.data.repository

import androidx.media3.common.util.Log
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.Track
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.SpotifyRepository
import com.andy.spotifysdktesting.feature.spotifysdk.domain.service.SpotifyApiService
import org.json.JSONObject

private const val TAG = "SpotifyRepo"

class SpotifyRepositoryImpl(
    private val api: SpotifyApiService
) : SpotifyRepository {

    private fun parseTracks(json: String): List<Track> {
        val root = JSONObject(json)
        // Verificar que existan tracks antes de intentar parsear
        if (!root.has("tracks")) return emptyList()

        val items = root.getJSONObject("tracks").getJSONArray("items")

        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            val album = item.getJSONObject("album")
            val images = album.getJSONArray("images")
            val imageUrl = if (images.length() > 0) images.getJSONObject(0).getString("url") else ""

            Track(
                id = item.getString("id"),
                name = item.getString("name"),
                artist = item.getJSONArray("artists").getJSONObject(0).getString("name"),
                image = imageUrl,
                uri = item.getString("uri")
            )
        }
    }

    override suspend fun searchTracks(query: String): List<Track> {
        return try {
            val response = api.searchTracks(query)
            parseTracks(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando tracks: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getTrack(id: String): Track {
        val json = JSONObject(api.getTrack(id))
        val album = json.getJSONObject("album")
        val images = album.getJSONArray("images")
        val imageUrl = if (images.length() > 0) images.getJSONObject(0).getString("url") else ""

        return Track(
            id = json.getString("id"),
            name = json.getString("name"),
            artist = json.getJSONArray("artists").getJSONObject(0).getString("name"),
            image = imageUrl,
            uri = json.getString("uri")
        )
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
        val json = JSONObject(api.getRecommendations(seedTracks, seedArtists, seedGenres))
        val items = json.getJSONArray("tracks")

        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            Track(
                id = item.getString("id"),
                name = item.getString("name"),
                artist = item.getJSONArray("artists")
                    .getJSONObject(0)
                    .getString("name"),
                image = item.getJSONObject("album")
                    .getJSONArray("images")
                    .getJSONObject(0)
                    .getString("url"),
                uri = item.getString("uri")
            )
        }
    }
}
