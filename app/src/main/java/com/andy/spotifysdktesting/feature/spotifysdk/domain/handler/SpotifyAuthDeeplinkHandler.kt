package com.andy.spotifysdktesting.feature.spotifysdk.domain.handler

import android.net.Uri


object SpotifyAuthDeeplinkHandler {

    fun extractAuthCode(uri: Uri): String? {
        println(">>> DeepLink recibido: ${uri}")
        return uri.getQueryParameter("code")
    }
}