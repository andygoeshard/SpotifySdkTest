package com.andy.spotifysdktesting.feature.spotifysdk.domain.manager

import android.content.Context
import android.util.Log
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack
import com.andy.spotifysdktesting.feature.spotifysdk.domain.state.SpotifySdkState
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "Spotify SDK Manager"
class SpotifySdkManager(
    private val context: Context
) {
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private val _state = MutableStateFlow(SpotifySdkState())
    val state: StateFlow<SpotifySdkState> = _state.asStateFlow()
    fun connect(clientId: String, redirectUri: String) {

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
                    _state.update { it.copy(isConnected = true) }
                    Log.d(TAG, "Spotify SDK conectado")
                    subscribeToPlayerState()
                }

                override fun onFailure(error: Throwable) {
                    Log.e(TAG, "Error al conectar Spotify SDK", error)
                    _state.value = SpotifySdkState()
                }
            }
        )
    }

    fun disconnect() {
        spotifyAppRemote?.let { SpotifyAppRemote.disconnect(it) }
        spotifyAppRemote = null
        _state.value = SpotifySdkState()
    }

    private fun subscribeToPlayerState() {
        spotifyAppRemote
            ?.playerApi
            ?.subscribeToPlayerState()
            ?.setEventCallback { playerState ->

                val track = playerState.track

                val current = track?.let {
                    CurrentTrack(
                        trackName = it.name,
                        artistName = it.artist.name,
                        imageUri = imageUrl(it.imageUri?.raw),
                        isPaused = playerState.isPaused,
                        id = it.uri
                    )
                }

                _state.update {
                    it.copy(
                        isPaused = playerState.isPaused,
                        currentTrack = current
                    )
                }
            }
            ?.setErrorCallback {
                Log.e("SpotifySdkManager", "Player error", it)
            }
    }

    fun play() = spotifyAppRemote?.playerApi?.resume()
    fun pause() = spotifyAppRemote?.playerApi?.pause()
    fun next() = spotifyAppRemote?.playerApi?.skipNext()
    fun previous() = spotifyAppRemote?.playerApi?.skipPrevious()
    fun playUri(uri: String) = spotifyAppRemote?.playerApi?.play(uri)

    private fun imageUrl(imgUri: String?): String? =
        imgUri?.substringAfter("spotify:image:")?.let {
            "https://i.scdn.co/image/$it"
        }
}