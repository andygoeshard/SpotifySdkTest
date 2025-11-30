package com.andy.spotifysdktesting.core.ai.domain

import androidx.media3.common.util.Log
import com.andy.spotifysdktesting.core.ai.data.provider.GeminiClientProvider
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager

private const val TAG = "AiMusicBrain"
class AiMusicBrain(
    private val gemini: GeminiClientProvider,
    private val spotify: SpotifyManager
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

        Log.d(TAG, "PROMPT enviado a Gemini: \n$prompt")

        val resp = gemini.client.generateContent(prompt)

        val rawResponse = resp.text
        if (rawResponse != null) {
            Log.d(TAG, "RESPUESTA JSON de Gemini: \n$rawResponse")
        } else {
            Log.w(TAG, "RESPUESTA de Gemini fue nula.")
        }

        return rawResponse ?: ""
    }

    suspend fun describeActualSong(currentTrack: CurrentTrack?): String {
        // 🛑 CORRECCIÓN CLAVE: PIDE UN JSON ESTRICTO con la clave 'reason'
        val prompt = """
        Sos una IA DJ. Tu objetivo es describir el tema actual como si hubiesen pasado un par de canciones antes. 
        
        **Tu respuesta debe ser estricta y DEBE ser un objeto JSON** con la clave "reason" que contenga tu descripción de DJ. La descripción debe durar 10 segundos o menos. NO incluyas ninguna prosa, explicación, ni bloques de código (```json) fuera del objeto JSON.

        Tema actual:
        Nombre: ${currentTrack?.trackName}
        Artista: ${currentTrack?.artistName}

        Ejemplo de respuesta: {"reason": "¡Ay, ya llegó el ritmo! Después de un par de temazos, prepárense para sentir la... ¡\"Vaina Loca\" de Ozuna! ¡Dale, a bailar!"}
        hay que respetar el json. no hace falta que sea igual, es mas, no deberia ser igual ejemplo. tiene que sonar mas argentino.
    """.trimIndent()

        Log.d(TAG, "PROMPT enviado a Gemini: \n$prompt")

        val resp = gemini.client.generateContent(prompt)
        val rawResponse = resp.text

        if (rawResponse != null) {
            Log.d(TAG, "RESPUESTA CRUDA de Gemini: \n$rawResponse")
        } else {
            Log.w(TAG, "RESPUESTA de Gemini fue nula.")
        }

        // 💡 Retornamos la respuesta cruda. El ViewModel se encargará de parsear.
        return rawResponse ?: ""
    }

    suspend fun chat(message: String): String {
        // En el chat también puede ser útil loguear la respuesta si hay errores
        val resp = gemini.client.generateContent(message)
        val rawResponse = resp.text

        if (rawResponse != null) {
            Log.d(TAG, "RESPUESTA Chat de Gemini: \n$rawResponse")
        }

        return rawResponse ?: ""
    }

}
