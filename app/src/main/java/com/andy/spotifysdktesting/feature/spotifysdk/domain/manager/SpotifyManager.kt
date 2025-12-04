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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

class SpotifyManager(
    private val context: Context
) {

    private var spotifyAppRemote: SpotifyAppRemote? = null
    var currentTrackCache: CurrentTrack? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

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
                    _isConnected.value = true
                    trySend(true)
                }

                override fun onFailure(error: Throwable) {
                    Log.e("SpotifyManager", "Connection failed: ${error.localizedMessage}")
                    _isConnected.value = false // 🎯 ACTUALIZAR ESTADO
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
        _isConnected.value = false // 🎯 ACTUALIZAR ESTADO
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
                    isPaused = playerState.isPaused,
                    id = track?.uri
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

    fun playUri(uri: String) {
        spotifyAppRemote?.playerApi?.play(uri)
            ?: Log.e("SpotifyManager", "Not connected. Cannot play URI.")
    }

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