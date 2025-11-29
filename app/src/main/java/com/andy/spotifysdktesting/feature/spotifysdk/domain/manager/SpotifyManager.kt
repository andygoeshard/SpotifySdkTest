package com.andy.spotifysdktesting.feature.spotifysdk.domain.manager

import android.content.Context
import android.util.Log
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SpotifyManager(
    private val context: Context
) {

    private var spotifyAppRemote: SpotifyAppRemote? = null

    /** Token de la Web API (PKCE). Lo setea tu autenticación */
    var accessToken: String? = null

    /** Última canción escuchada (cacheado para IA) */
    var currentTrackCache: CurrentTrack? = null

    // -----------------------------------------------------------
    //  CONNECT
    // -----------------------------------------------------------
    fun connect(clientId: String, redirectUri: String): Flow<Boolean> = callbackFlow {

        val params = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(
            context,
            params,
            object : Connector.ConnectionListener {

                override fun onConnected(remote: SpotifyAppRemote) {
                    spotifyAppRemote = remote
                    Log.d("SpotifyManager", "Connected to Spotify App Remote")
                    trySend(true)
                }

                override fun onFailure(error: Throwable) {
                    Log.e("SpotifyManager", "Connection failed: ${error.localizedMessage}")
                    trySend(false)
                }
            }
        )

        awaitClose {
            disconnect()
        }
    }

    fun disconnect() {
        spotifyAppRemote?.let {
            Log.d("SpotifyManager", "Disconnecting Spotify App Remote")
            SpotifyAppRemote.disconnect(it)
        }
        spotifyAppRemote = null
    }

    // -----------------------------------------------------------
    //  CURRENT TRACK STREAM
    // -----------------------------------------------------------
    fun getCurrentlyPlayingTrack(): Flow<CurrentTrack> = callbackFlow {

        val remote = spotifyAppRemote
        if (remote == null) {
            close(IllegalStateException("Spotify not connected"))
            return@callbackFlow
        }

        remote.playerApi
            .subscribeToPlayerState()
            .setEventCallback { playerState ->

                val track: Track? = playerState.track ?: return@setEventCallback

                val ct = CurrentTrack(
                    trackName = track?.name ?: "",
                    artistName = track?.artist?.name ?: "",
                    imageUri = track?.imageUri?.raw,
                    isPaused = playerState.isPaused
                )

                currentTrackCache = ct
                trySend(ct)
            }
            .setErrorCallback {
                Log.e("SpotifyManager", "Player state error: $it")
            }

        awaitClose {
            Log.d("SpotifyManager", "Stopped listening to player state")
        }
    }

    // -----------------------------------------------------------
    //  PLAYER CONTROL
    // -----------------------------------------------------------
    fun play() {
        spotifyAppRemote?.playerApi?.resume()
    }

    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    fun next() {
        spotifyAppRemote?.playerApi?.skipNext()
    }

    fun previous() {
        spotifyAppRemote?.playerApi?.skipPrevious()
    }

    /** 🔥 NUEVO: Reproduce directamente una canción usando una URI */
    fun playUri(uri: String) {
        spotifyAppRemote?.playerApi?.play(uri)
            ?: Log.e("SpotifyManager", "Not connected. Cannot play URI.")
    }

    // -----------------------------------------------------------
    //  IMAGE HELPER
    // -----------------------------------------------------------
    fun imageUrl(imgUri: String?): String? {
        if (imgUri == null) return null
        return try {
            val key = imgUri.substringAfter("spotify:image:")
            "https://i.scdn.co/image/$key"
        } catch (e: Exception) {
            null
        }
    }
}