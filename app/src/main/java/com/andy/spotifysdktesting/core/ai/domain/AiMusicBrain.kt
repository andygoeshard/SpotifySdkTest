package com.andy.spotifysdktesting.core.ai.domain

import com.andy.spotifysdktesting.core.ai.data.provider.GeminiClientProvider
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager

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

        val resp = gemini.client.generateContent(prompt)
        return resp.text ?: ""
    }

    suspend fun chat(message: String): String {
        val resp = gemini.client.generateContent(message)
        return resp.text ?: ""
    }

}
