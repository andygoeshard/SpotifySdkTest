package com.andy.spotifysdktesting.core.ai.domain

import android.util.Log
import com.andy.spotifysdktesting.core.ai.domain.model.AiNextSong
import com.andy.spotifysdktesting.feature.spotifysdk.data.entity.TrackRecommendation
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.collections.mapIndexed

private const val TAG = "AiMusicBrain"

private fun extractReasonFromJson(rawJson: String): String {
    return try {
        val jsonElement = Json.parseToJsonElement(rawJson).jsonObject
        jsonElement["reason"]?.jsonPrimitive?.content ?: "Error de parseo de IA: No se encontró la razón."
    } catch (e: Exception) {
        Log.e(TAG, "FALLO AL PARSEAR JSON DE IA: $rawJson", e)
        "Error interno: Fallo al procesar la respuesta de la IA."
    }
}

fun parseAiNextSongResponse(rawJson: String): AiNextSong {
    return try {
        // La IA debe devolver un JSON válido. Lo parseamos en el objeto esperado.
        Json.decodeFromString(AiNextSong.serializer(), rawJson.trim())
    } catch (e: Exception) {
        Log.e(TAG, "FALLO AL PARSEAR JSON DE nextSong: $rawJson", e)
        // Devolvemos un objeto de error para manejo en DjService
        AiNextSong(song = "", reason = "Error de formato de la IA.")
    }
}

class AiMusicBrain(
    private val aiClient: AiClient,
) {

    suspend fun chooseNextSong(
        currentMood: String,
        currentTrack: CurrentTrack?,

    ): String {

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

    suspend fun chooseFromRecommendations(
        currentMood: String,
        currentTrack: CurrentTrack?,
        recommendations: List<TrackRecommendation>
    ): AiNextSong {

        // 1. Formatear las opciones como texto legible para la IA
        val optionsText = recommendations.mapIndexed { index, track ->
            "${index + 1}. ${track.artist} - ${track.name}"
        }.joinToString("\n")

        val prompt = """
            Sos un DJ AI con la onda más copada de Argentina. Tu trabajo es seleccionar la MEJOR canción de la lista que te pasé.
         
            **CONTEXTO MUSICAL:**
            Mood Deseado: $currentMood (Asegurate de que la selección encaje con este mood.)
            TEMA ACTUAL SONANDO: ${currentTrack?.artistName ?: "N/A"} - ${currentTrack?.trackName ?: "N/A"}
            
            **OPCIONES REALES DE SPOTIFY (Elegí SOLO UNA):**
            $optionsText

            **RESTRICCIONES ESTRICTAS:**
            1. **SELECCIÓN:** Elegí la opción que mejor siga la onda y el *mood*.
            2. **FORMATO:** El valor de "song" DEBE ser una cadena limpia y exacta: `Artista - Nombre del Tema`. DEBE ser uno de los temas de la lista.
            
            **JSON DE SALIDA (ESTRICTO):**
            {
                "song": "Artista - Nombre del Tema", 
                "reason": "Una justificación breve, divertida y bien argenta (máx. 15 palabras) de por qué elegiste este tema de la lista."
            }
        """.trimIndent()

        Log.d(TAG, "PROMPT enviado (Recomendaciones): \n$prompt")

        val rawResponse = aiClient.generateContent(prompt)

        if (rawResponse.isNotEmpty()) {
            Log.d(TAG, "RESPUESTA JSON de IA: \n$rawResponse")
            // Devolvemos el objeto parseado
            return parseAiNextSongResponse(rawResponse)
        } else {
            Log.w(TAG, "RESPUESTA de IA fue nula o vacía.")
            return AiNextSong(song = "", reason = "IA no respondió.")
        }
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
            return extractReasonFromJson(rawResponse)
        } else {
            Log.w(TAG, "RESPUESTA de IA fue nula o vacía.")
            return "No pude obtener una descripción en este momento."
        }
    }

    suspend fun chat(message: String): String {
        val rawResponse = aiClient.generateContent(message)

        if (rawResponse.isNotEmpty()) {
            Log.d(TAG, "RESPUESTA Chat de IA: \n$rawResponse")
        }

        return rawResponse
    }

}
