package com.andy.spotifysdktesting.core.tts.domain.result

sealed class TtsResult {
    data class Success(val audioBytes: ByteArray) : TtsResult()
    data class Error(val message: String, val cause: Throwable? = null) : TtsResult()
}