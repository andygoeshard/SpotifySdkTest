package com.andy.spotifysdktesting.core.tts.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.andy.spotifysdktesting.core.tts.domain.TtsEngine
import com.andy.spotifysdktesting.core.tts.domain.TtsResult
import com.andy.spotifysdktesting.core.tts.domain.TtsVoice
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "AndroidTtsEngine"

class AndroidTtsEngine(
    context: Context
) : TtsEngine {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private val tempAudioFile: File = File(appContext.cacheDir, "temp_tts_audio.wav")

    init {
        // La inicialización del TTS nativo es asíncrona
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Configuración inicial
                tts?.language = Locale.getDefault()
                Log.d(TAG, "Android TTS inicializado con éxito.")
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

        if (tts == null) {
            continuation.resume(TtsResult.Error("Motor TTS nativo no inicializado o fallido."))
            return@suspendCancellableCoroutine
        }

        // 💡 1. Configurar la voz (si se quiere una específica)
        // Implementar lógica para buscar y seleccionar la voz por TtsVoice.id
        // Por simplicidad, aquí usamos la configuración por defecto.

        val utteranceId = text.hashCode().toString()

        // 💡 2. Usar synthesizeToFile (más robusto que speak() para devolver bytes)
        val result = tts!!.synthesizeToFile(
            text,
            null, // Usar parámetros por defecto, o Voice.getParams()
            tempAudioFile,
            utteranceId
        )

        if (result == TextToSpeech.ERROR) {
            continuation.resume(TtsResult.Error("Fallo al iniciar la síntesis del archivo."))
            return@suspendCancellableCoroutine
        }

        // 💡 3. Escuchar cuándo termina la síntesis
        tts!!.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onStop(utteranceId: String?, interrupted: Boolean) {}

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    try {
                        val audioBytes = FileInputStream(tempAudioFile).use { it.readBytes() }
                        tempAudioFile.delete() // Limpiar el archivo temporal
                        continuation.resume(TtsResult.Success(audioBytes))
                    } catch (e: Exception) {
                        continuation.resume(TtsResult.Error("Error leyendo o limpiando el archivo TTS.", e))
                    }
                }
            }
            // Manejo de errores
            override fun onError(id: String?) {
                continuation.resume(TtsResult.Error("Error durante la síntesis TTS nativa."))
                tempAudioFile.delete()
            }
            override fun onError(id: String?, errorCode: Int) {
                onError(id) // Usar el método obsoleto como fallback
            }
        })

        continuation.invokeOnCancellation {
            tts?.stop()
            tempAudioFile.delete()
        }
    }

    // Función de limpieza para el ciclo de vida del Service
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        Log.d(TAG, "Android TTS shutdown.")
    }
}