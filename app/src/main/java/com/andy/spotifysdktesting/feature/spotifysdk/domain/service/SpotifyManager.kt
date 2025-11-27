package com.andy.spotifysdktesting.feature.spotifysdk.domain.service

import android.content.Context
import android.util.Log
import com.andy.spotifysdktesting.feature.spotifysdk.data.CurrentTrack
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SpotifyManager(private val context:Context) {

    private var spotifyAppRemote: SpotifyAppRemote? = null

    fun connect(clientId: String, redirectUri: String): Flow<Boolean> = callbackFlow {
        SpotifyAppRemote.connect(
            context,
            ConnectionParams.Builder(clientId)
                .setRedirectUri(redirectUri)
                .showAuthView(true)
                .build(),
            object : Connector.ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    this@SpotifyManager.spotifyAppRemote = spotifyAppRemote
                    trySend(true)
                }
                override fun onFailure(throwable: Throwable) {
                    Log.e("SpotifyManager", "Connect failed")
                    trySend(false)
                }
            }
        )
        awaitClose {
            spotifyAppRemote?.let {
                SpotifyAppRemote.disconnect(it)
            }
        }

    }

    fun disconnect() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
            spotifyAppRemote = null
        }
    }

    fun getCurrentlyPlayingTrack(): Flow<CurrentTrack> = callbackFlow {
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
            val track: Track? = playerState.track
            if (track != null) {
                trySend(CurrentTrack(track.name, track.artist.name, track.imageUri.raw, playerState.isPaused))
            }
        }
        awaitClose { Log.e("SpotifyManager", "No se pudo obtener la data") }
    }

    fun play(){
        spotifyAppRemote?.playerApi?.resume()
    }

    fun pause(){
        spotifyAppRemote?.playerApi?.pause()
    }

    fun next(){
        spotifyAppRemote?.playerApi?.skipNext()
    }

    fun previous(){
        spotifyAppRemote?.playerApi?.skipPrevious()
    }

    fun imageUrl(imgUrl: String?): String {

        val count: Int = imgUrl?.length!! - 1
        val imgUrl3: String = "https://i.scdn.co/image/" + imgUrl.slice(14..count)
        return imgUrl3

    }

}