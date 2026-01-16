package com.andy.spotifysdktesting.feature.spotifywebapi.domain.handler

import android.net.Uri

object SpotifyAuthDeeplinkHandler {

    fun extractAuthCode(uri: Uri): String? {
        println(">>> DeepLink recibido: ${uri}")
        return uri.getQueryParameter("code")
    }
}