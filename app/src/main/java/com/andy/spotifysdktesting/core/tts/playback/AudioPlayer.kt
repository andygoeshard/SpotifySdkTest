package com.andy.spotifysdktesting.core.tts.playback

import android.content.Context
import java.io.File
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class AudioPlayer(
    private val context: Context
) {

    private var player: ExoPlayer? = null

    fun play(audio: ByteArray, onFinish: (() -> Unit)? = null) {
        stop()

        // Archivo temporal (sirve para mp3, wav, opus, etc.)
        val file = File(context.cacheDir, "tts_${System.currentTimeMillis()}.tmp")
        file.writeBytes(audio)

        // Crea nuevo player
        val exo = ExoPlayer.Builder(context).build()
        player = exo

        // Detecta tipo MIME según extensión (si querés lo puedo mejorar)
        val mime = when {
            file.name.endsWith(".wav") -> MimeTypes.AUDIO_WAV
            file.name.endsWith(".ogg") -> MimeTypes.AUDIO_OGG
            file.name.endsWith(".opus") -> MimeTypes.AUDIO_OPUS
            else -> MimeTypes.AUDIO_MPEG // MP3 default
        }

        val mediaItem = MediaItem.Builder()
            .setUri(file.toURI().toString())
            .setMimeType(mime)
            .build()

        exo.setMediaItem(mediaItem)

        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == ExoPlayer.STATE_ENDED) {
                    onFinish?.invoke()
                    stop()
                }
            }
        })

        exo.prepare()
        exo.play()
    }

    fun stop() {
        player?.run {
            stop()
            release()
        }
        player = null
    }
}