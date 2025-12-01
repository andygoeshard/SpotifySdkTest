package com.andy.spotifysdktesting.core.tts.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import com.andy.spotifysdktesting.core.tts.domain.TtsEngine
import com.andy.spotifysdktesting.core.tts.domain.TtsResult
import com.andy.spotifysdktesting.core.tts.domain.TtsVoice
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "AndroidTtsEngine"

class AndroidTtsEngine(
    context: Context
) : TtsEngine {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)

                // === AUDIOFOCUS CON DUCKING REAL ===
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFocusRequest = AudioFocusRequest.Builder(
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                    )
                        .setAudioAttributes(audioAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener { change ->
                            Log.d(TAG, "Audio focus changed: $change")
                        }
                        .build()
                }

                tts?.language = Locale.getDefault()

                Log.d(TAG, "Android TTS inicializado con DUCKING.")
            } else {
                Log.e(TAG, "Fallo al inicializar Android TTS: $status")
                tts = null
            }
        }
    }

    override suspend fun synthesize(
        text: String,
        voice: TtsVoice
    ): TtsResult = suspendCancellableCoroutine { continuation ->

        val engine = tts
        if (engine == null) {
            continuation.resume(TtsResult.Error("Motor TTS nativo no inicializado."))
            return@suspendCancellableCoroutine
        }

        // === SOLICITAR AUDIO FOCUS CON DUCKING ===
        val focusGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }

        if (focusGranted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            continuation.resume(TtsResult.Error("No se obtuvo audio focus."))
            return@suspendCancellableCoroutine
        }

        val utteranceId = text.hashCode().toString()

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val result = engine.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            params,
            utteranceId
        )

        if (result == TextToSpeech.ERROR) {
            continuation.resume(TtsResult.Error("Error al iniciar TTS."))
            return@suspendCancellableCoroutine
        }

        engine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {

            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS START $utteranceId")
            }

            override fun onDone(id: String?) {
                if (id == utteranceId) {

                    // === LIBERAR FOCUS ===
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                    } else {
                        audioManager.abandonAudioFocus(null)
                    }

                    continuation.resume(TtsResult.Success(ByteArray(0)))
                }
            }

            override fun onError(id: String?) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                } else {
                    audioManager.abandonAudioFocus(null)
                }

                continuation.resume(TtsResult.Error("Error durante TTS."))
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {}
        })

        continuation.invokeOnCancellation {
            engine.stop()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                audioManager.abandonAudioFocus(null)
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        Log.d(TAG, "Android TTS shutdown.")
    }
}
