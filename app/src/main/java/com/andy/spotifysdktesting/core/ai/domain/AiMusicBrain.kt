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

fun cleanAiResponse(rawResponse: String): String {
    return rawResponse
        .trim()
        .removePrefix("```json") // Quita ```json
        .removePrefix("```")      // Quita ``` (si no especificó json)
        .removeSuffix("```")      // Quita el cierre ```
        .trim()                   // Limpia cualquier espacio o salto de línea residual
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

            Respondé en JSON obligatorio - nada más:
            {
                "songName": "artista - tema",
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

        return cleanAiResponse(rawResponse)
    }

    suspend fun chooseFromRecommendations(
        currentMood: String,
        currentTrack: CurrentTrack?,
        recommendations: List<TrackRecommendation>
    ): AiNextSong {

        val optionsText = recommendations.mapIndexed { index, track ->
            "${index + 1}. ${track.artist} - ${track.name}"
        }.joinToString("\n")

        val systemIdentity = "Eres un DJ argentino, canchero, rápido y directo. y el nombre de la radio es radio pingote"

        val prompt = """
    $systemIdentity

    Responde **SÓLO** con un JSON {"song": "Artista - Tema", "reason": "Justificación argenta (máx. 12 palabras)"}. **NO** incluyas texto extra, explicaciones o código wrapping (```json).

    MOOD: $currentMood
    ACTUAL: ${currentTrack?.artistName ?: "N/A"} - ${currentTrack?.trackName ?: "N/A"}

    CANDIDATOS (Elige solo uno de la lista):
    $optionsText
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

            // 🚨 PASO CRÍTICO: Limpiar la respuesta antes de parsear
            val cleanedResponse = cleanAiResponse(rawResponse)
            Log.d(TAG, "RESPUESTA LIMPIA para JSON: \n$cleanedResponse")

            // Ahora el parser solo ve el objeto JSON limpio: {"reason": "..."}
            return extractReasonFromJson(cleanedResponse)
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
