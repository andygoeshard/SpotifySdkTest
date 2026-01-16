package com.andy.spotifysdktesting.core.ai.domain.helper

import android.util.Log
import com.andy.spotifysdktesting.core.ai.domain.model.AiSuggestion
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "AiManager"
fun extractReasonFromJson(rawJson: String): String {
    return try {
        val jsonElement = Json.parseToJsonElement(rawJson).jsonObject
        jsonElement["reason"]?.jsonPrimitive?.content
            ?: "Error de parseo de IA: No se encontró la razón."
    } catch (e: Exception) {
        Log.e(TAG, "FALLO AL PARSEAR JSON DE IA: $rawJson", e)
        "Error interno: Fallo al procesar la respuesta de la IA."
    }
}

fun cleanAiResponse(rawResponse: String): String {
    return rawResponse
        .trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
}

fun parseSuggestion(rawJson: String): AiSuggestion {
    return try {
        val json = Json.parseToJsonElement(rawJson).jsonObject
        AiSuggestion(
            songName = json["songName"]!!.jsonPrimitive.content,
            reason = json["reason"]!!.jsonPrimitive.content
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error parseando sugerencia IA", e)
        AiSuggestion(
            songName = "",
            reason = "La IA tiró fruta, disculpá."
        )
    }
}

fun buildPrompt(
    mood: String,
    track: CurrentTrack?
): String = """
Sos una IA DJ. Elegí la próxima canción según el tema actual y el mood. 
reason que se pueda leer en 5 segundos o menos
Mood: $mood
Tema actual: ${track?.trackName} - ${track?.artistName}
Respondé SOLO en este JSON (obligatorio):
{
  "songName": "artista - tema",
  "reason": "Comentario estilo radio del este tema nuevo."
}
""".trimIndent()

fun buildDescribePrompt(
    currentTrack: CurrentTrack?
): String = """
Sos una IA DJ argentina, buena onda y compañera. Describí la canción que suena ahora. Texto que se pueda leer en 5 segundos o menos
Canción actual: ${currentTrack?.trackName} - ${currentTrack?.artistName}
Respuesta OBLIGATORIA: solo este JSON, sin texto afuera:
{
  "reason": "descripción breve (hasta 10 segundos), con tono argentino natural"
}
""".trimIndent()
