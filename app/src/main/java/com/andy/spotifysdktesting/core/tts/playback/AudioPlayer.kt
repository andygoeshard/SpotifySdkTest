package com.andy.spotifysdktesting.core.tts.playback

import android.content.Context
import android.media.MediaPlayer
import java.io.File

class AudioPlayer(
    private val context: Context
) {

    private var player: MediaPlayer? = null

    fun play(audio: ByteArray, onFinish: (() -> Unit)? = null) {
        stop()

        // Crear archivo temporal
        val file = File(context.cacheDir, "tts_temp_${System.currentTimeMillis()}.mp3")
        file.writeBytes(audio)

        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            setOnCompletionListener {
                onFinish?.invoke()
                stop()
            }
            start()
        }
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
    }
}