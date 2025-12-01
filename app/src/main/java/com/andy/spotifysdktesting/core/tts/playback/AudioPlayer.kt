package com.andy.spotifysdktesting.core.tts.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import java.io.File
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import java.io.IOException

private const val TAG = "AudioPlayer"

class AudioPlayer(
    context: Context,
    private val exoPlayer: ExoPlayer
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(TAG, "Audio focus changed: $focusChange")
    }
    private var onPlaybackEndedCallback: (() -> Unit)? = null

    init {
        // 🎯 Configuración del listener al inicio para el Singleton
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == ExoPlayer.STATE_ENDED) {
                    Log.d(TAG, "TTS playback ended.")
                    abandonAudioFocus()
                    onPlaybackEndedCallback?.invoke()
                    exoPlayer.clearMediaItems()
                    exoPlayer.seekTo(0)
                }
            }
        })
    }
    @OptIn(UnstableApi::class)
    fun play(audio: ByteArray, onFinish: (() -> Unit)? = null) {
        // 🛑 No llamamos a stop() o release(), solo limpiamos la reproducción anterior.
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        onPlaybackEndedCallback = onFinish

        // 1. Solicitud de Foco de Audio (Ducking)
        val focusResult = requestAudioFocus()

        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "Foco de audio denegado. No se puede reproducir el TTS.")
            onFinish?.invoke()
            return
        }

        // 2. 🎯 Cargar audio directamente desde la memoria (ByteArrayDataSource)
        val source = buildMediaSourceFromBytes(audio)

        exoPlayer.setMediaSource(source)

        // 3. Iniciar reproducción
        exoPlayer.prepare()
        exoPlayer.play()
        Log.d(TAG, "TTS playback started instantly.")
    }
    @OptIn(UnstableApi::class)
    private fun buildMediaSourceFromBytes(audio: ByteArray): androidx.media3.exoplayer.source.MediaSource {

        val dataSource = ByteArrayDataSource(audio)
        val factory = DataSource.Factory { dataSource }
        return ProgressiveMediaSource.Factory(factory)
            .createMediaSource(MediaItem.fromUri(android.net.Uri.EMPTY))
    }

    fun release() {
        exoPlayer.release()
        abandonAudioFocus()
        Log.d(TAG, "ExoPlayer liberado.")
    }

    fun stop() {
        exoPlayer.stop()
        abandonAudioFocus()
    }
    private fun requestAudioFocus(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) // 🎯 LA CLAVE DEL DUCKING
                .setAudioAttributes(audioAttributes)
                .setWillPauseWhenDucked(false) // No pausar a Spotify, solo bajar volumen
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            audioManager.requestAudioFocus(focusRequest)
        } else {
            // Implementación para APIs antiguas (< O)
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    /**
     * Abandona el foco de audio, permitiendo que otras apps (Spotify) recuperen el volumen.
     */
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            audioManager.abandonAudioFocusRequest(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }
}