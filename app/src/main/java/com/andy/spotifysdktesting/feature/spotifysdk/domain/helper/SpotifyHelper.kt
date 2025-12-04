package com.andy.spotifysdktesting.feature.spotifysdk.domain.helper

fun extractIdFromUri(uri: String): String? {
    if (uri.isBlank()) return null
    return uri.split(':').lastOrNull()
}