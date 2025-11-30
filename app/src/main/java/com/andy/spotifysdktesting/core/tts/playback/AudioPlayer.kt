package com.andy.spotifysdktesting.core.tts.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import java.io.File
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.IOException

private const val TAG = "AudioPlayer"

class AudioPlayer(
    private val context: Context
) {

    private var player: ExoPlayer? = null

    // 💡 1. Inicializar el AudioManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // 💡 2. Listener de Foco de Audio (Necesario para las APIs modernas y deprecadas)
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        // No se necesita lógica aquí porque ExoPlayer ya se gestiona con el onPlaybackStateChanged
        // pero este listener es requerido por la API para saber qué app está pidiendo el foco.
        Log.d(TAG, "Audio focus changed: $focusChange")
    }

    /**
     * Prepara el archivo de audio, solicita el foco de audio (con ducking),
     * y comienza la reproducción.
     */
    fun play(audio: ByteArray, onFinish: (() -> Unit)? = null) {
        stop() // Asegura que cualquier reproducción anterior se detenga

        // 1. Solicitud de Foco de Audio (Ducking)
        val focusResult = requestAudioFocus()

        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "Foco de audio denegado. No se puede reproducir el TTS.")
            onFinish?.invoke()
            return // Sale si el foco es denegado (ej. una llamada)
        }

        // 2. Escribir el audio temporalmente
        val file = File(context.cacheDir, "tts_${System.currentTimeMillis()}.tmp")
        try {
            file.writeBytes(audio)
        } catch (e: IOException) {
            Log.e(TAG, "Error escribiendo archivo de audio temporal: ${e.message}")
            abandonAudioFocus()
            onFinish?.invoke()
            return
        }

        // 3. Configurar ExoPlayer
        val exo = ExoPlayer.Builder(context).build()
        player = exo

        val mime = when {
            file.name.endsWith(".wav") -> MimeTypes.AUDIO_WAV
            file.name.endsWith(".ogg") -> MimeTypes.AUDIO_OGG
            file.name.endsWith(".opus") -> MimeTypes.AUDIO_OPUS
            else -> MimeTypes.AUDIO_MPEG
        }

        val mediaItem = MediaItem.Builder()
            .setUri(file.toURI().toString())
            .setMimeType(mime)
            .build()

        exo.setMediaItem(mediaItem)

        // 4. Listener: Liberar foco y limpiar al finalizar
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == ExoPlayer.STATE_ENDED) {
                    Log.d(TAG, "TTS playback ended.")
                    // 🛑 Liberar el foco para que Spotify recupere el volumen
                    abandonAudioFocus()
                    onFinish?.invoke()
                    stop()
                    file.delete() // Limpiar archivo temporal
                }
            }
        })

        exo.prepare()
        exo.play()
        Log.d(TAG, "TTS playback started. Spotify should duck.")
    }

    /**
     * Detiene la reproducción y libera los recursos del reproductor.
     */
    fun stop() {
        player?.run {
            stop()
            release()
        }
        player = null
        // 🛑 Asegurarse de liberar el foco si se llama a stop() manualmente
        abandonAudioFocus()
    }

    // ----------------------------------------------------------------------
    // 💡 Métodos de Foco de Audio (Duck/Un-duck)
    // ----------------------------------------------------------------------

    /**
     * Solicita foco de audio con la bandera AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK.
     * Esto indica a otras apps multimedia (como Spotify) que bajen su volumen.
     */
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
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
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