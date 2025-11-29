package com.andy.spotifysdktesting.feature.spotifysdk.data.repository

import android.content.Context
import com.andy.spotifysdktesting.feature.spotifysdk.data.entity.TokenResponse
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.PKCEManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyTokenManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val context: Context,
    private val pkce: PKCEManager,
    private val clientId: String,
    private val redirectUri: String,
    private val client: HttpClient,
    private val tokenManager: SpotifyTokenManager
) : AuthRepository {

    // CAMBIO: Ahora retorna solo String (la URL)
    override suspend fun startLogin(): String {
        val verifier = pkce.generateCodeVerifier()
        val challenge = pkce.generateCodeChallenge(verifier)

        // IMPORTANTE: Guardamos el verifier en disco (DataStore)
        tokenManager.saveVerifier(verifier)

        val authUrl = "https://accounts.spotify.com/authorize?" +
                "client_id=$clientId" +
                "&response_type=code" +
                "&redirect_uri=$redirectUri" +
                "&code_challenge=$challenge" +
                "&code_challenge_method=S256" +
                "&scope=user-read-private%20user-read-email%20streaming%20user-read-playback-state%20user-modify-playback-state"

        return authUrl
    }

    // CAMBIO: Ya no recibe verifier como parámetro
    override suspend fun exchangeCodeForToken(
        code: String
    ): Boolean = withContext(Dispatchers.IO) {

        // RECUPERAR: Leemos el verifier de DataStore
        val verifier = tokenManager.getVerifier()

        if (verifier.isNullOrEmpty()) {
            println("ERROR: Verifier no encontrado. Posible reinicio de proceso.")
            return@withContext false
        }

        val form = Parameters.build {
            append("client_id", clientId)
            append("grant_type", "authorization_code")
            append("code", code)
            append("redirect_uri", redirectUri)
            append("code_verifier", verifier)
        }

        val response: HttpResponse = client.post("https://accounts.spotify.com/api/token") {
            setBody(FormDataContent(form))
            header("Content-Type", "application/x-www-form-urlencoded")
        }

        if (!response.status.isSuccess()) return@withContext false

        val body: TokenResponse = response.body()

        tokenManager.saveTokens(
            accessToken = body.accessToken,
            refreshToken = body.refreshToken,
            expiresInSeconds = body.expiresIn
        )

        // Limpiamos el verifier usado
        tokenManager.clearVerifier()

        return@withContext true
    }

    override suspend fun refreshToken(): Boolean = withContext(Dispatchers.IO) {
        val refresh = tokenManager.getRefreshToken() ?: return@withContext false

        val response = client.post("https://accounts.spotify.com/api/token") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                Parameters.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refresh)
                    append("client_id", clientId)
                }
            )
        }

        if (!response.status.isSuccess()) return@withContext false

        val body: TokenResponse = response.body()

        tokenManager.saveTokens(
            accessToken = body.accessToken,
            refreshToken = body.refreshToken ?: refresh,
            expiresInSeconds = body.expiresIn
        )

        return@withContext true
    }

    override suspend fun getCurrentAccessToken(): String? {
        return tokenManager.getAccessToken()
    }

}