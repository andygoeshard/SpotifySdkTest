package com.andy.spotifysdktesting.feature.spotifysdk.di

import com.andy.spotifysdktesting.BuildConfig
import com.andy.spotifysdktesting.feature.spotifywebapi.data.repository.AuthRepositoryImpl
import com.andy.spotifysdktesting.feature.spotifywebapi.data.repository.SpotifyRepositoryImpl
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.manager.PKCEManager
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.repository.SpotifyRepository
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.service.SpotifyApiService
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifySdkManager
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.manager.SpotifyAuthManager
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.manager.SpotifyTokenManager
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.manager.SpotifyWebManager
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val spotifyModule = module {
    single { SpotifySdkManager(get()) }
    single { SpotifyWebManager(get(), get()) }
    single { SpotifyAuthManager(get(), get()) }

    single { SpotifyTokenManager(get()) }

    single { PKCEManager() }
    single(named("ApiClient")) {
        val tokenManager: SpotifyTokenManager = get()
        val authRepo: AuthRepository = get()

        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 25_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 25_000
            }
            expectSuccess = false

            // 2. Plugin AUTH (Solo para este cliente)
            install(Auth) {
                bearer {
                    loadTokens {
                        val accessToken = tokenManager.getAccessToken()
                        val refreshToken = tokenManager.getRefreshToken()
                        if (accessToken != null && refreshToken != null) {
                            BearerTokens(accessToken, refreshToken)
                        } else {
                            null
                        }
                    }

                    refreshTokens {
                        val success = authRepo.refreshToken()
                        if (success) {
                            val newAccess = tokenManager.getAccessToken()
                            val newRefresh = tokenManager.getRefreshToken()
                            if (newAccess != null && newRefresh != null) {
                                BearerTokens(newAccess, newRefresh)
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    single {
        SpotifyApiService(
            client = get(named("ApiClient")), // Usa el cliente con Token
        )
    }

    single<SpotifyRepository> { SpotifyRepositoryImpl(get(), get(),get()) }

    single<AuthRepository> {
        AuthRepositoryImpl(
            context = get(),
            pkce = get(),
            clientId = BuildConfig.SPOTIFY_CLIENT_ID,
            redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI,
            client = get(named("AuthClient")), // Usa el cliente SIN Token (del koinModule)
            tokenManager = get()
        )
    }
}