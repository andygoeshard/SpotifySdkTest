package com.andy.spotifysdktesting.feature.spotifysdk.data.repository

import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.Track
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.SpotifyRepository
import com.andy.spotifysdktesting.feature.spotifysdk.domain.service.SpotifyApiService
import org.json.JSONObject

class SpotifyRepositoryImpl(
    private val api: SpotifyApiService
) : SpotifyRepository {

    private fun parseTracks(json: String): List<Track> {
        val root = JSONObject(json)
        val items = root.getJSONObject("tracks").getJSONArray("items")

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

    override suspend fun searchTracks(query: String): List<Track> {
        return parseTracks(api.searchTracks(query))
    }

    override suspend fun getTrack(id: String): Track {
        val json = JSONObject(api.getTrack(id))

        return Track(
            id = json.getString("id"),
            name = json.getString("name"),
            artist = json.getJSONArray("artists").getJSONObject(0).getString("name"),
            image = json.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url"),
            uri = json.getString("uri")
        )
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
