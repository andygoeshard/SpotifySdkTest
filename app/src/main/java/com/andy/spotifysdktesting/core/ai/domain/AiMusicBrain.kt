package com.andy.spotifysdktesting.core.ai.domain

import android.util.Log
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack

private const val TAG = "AiMusicBrain"
class AiMusicBrain(
    private val aiClient: AiClient,
) {

    suspend fun chooseNextSong(currentMood: String, currentTrack: CurrentTrack?): String {

        val prompt = """
            Sos una IA DJ. Basate en el tema actual para elegir la siguiente canción.
         
            Mood: $currentMood
         
            Tema actual:
            Nombre: ${currentTrack?.trackName}
            Artista: ${currentTrack?.artistName}

            Respondé en JSON:
            {
                "song": "artista - tema",
                "reason": "por qué la elegiste"
            }
        """.trimIndent()

        Log.d(TAG, "PROMPT enviado: \n$prompt")

        val rawResponse = aiClient.generateContent(prompt)

        if (rawResponse.isNotEmpty()) {
            Log.d(TAG, "RESPUESTA JSON de IA: \n$rawResponse")
        } else {
            Log.w(TAG, "RESPUESTA de IA fue nula o vacía.")
        }

        return rawResponse
    }

    suspend fun describeActualSong(currentTrack: CurrentTrack?): String {
        val prompt = """
        Sos una IA DJ Carismatica. Tu objetivo es describir la cancion que esta sonando ahora mismo.
        canción actual:
        Nombre: ${currentTrack?.trackName}
        Artista: ${currentTrack?.artistName}
        tiene que sonar mas argentino, pero no cringe. buena onda y compañera.
    
        **Tu respuesta debe ser estricta y DEBE ser un objeto JSON** con la clave "reason" que contenga tu descripción de DJ. La descripción debe durar 10 segundos o menos. NO incluyas ninguna prosa, explicación, ni bloques de código (```json) fuera del objeto JSON.
        
        hay que respetar el json.
    """.trimIndent()

        Log.d(TAG, "PROMPT enviado: \n$prompt")

        // 💡 LLAMADA AL CLIENTE GENÉRICO
        val rawResponse = aiClient.generateContent(prompt)

        if (rawResponse.isNotEmpty()) {
            Log.d(TAG, "RESPUESTA CRUDA de IA: \n$rawResponse")
        } else {
            Log.w(TAG, "RESPUESTA de IA fue nula o vacía.")
        }

        return rawResponse
    }

    suspend fun chat(message: String): String {
        val rawResponse = aiClient.generateContent(message)

        if (rawResponse.isNotEmpty()) {
            Log.d(TAG, "RESPUESTA Chat de IA: \n$rawResponse")
        }

        return rawResponse
    }

}
