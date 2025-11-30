package com.andy.spotifysdktesting.core.tts.presentation.intent

sealed class TtsEvent {
    data class SpeakText(val text: String) : TtsEvent()
}