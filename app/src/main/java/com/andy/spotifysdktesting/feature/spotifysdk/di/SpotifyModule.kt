package com.andy.spotifysdktesting.feature.spotifysdk.di

import com.andy.spotifysdktesting.BuildConfig
import com.andy.spotifysdktesting.feature.spotifysdk.data.repository.AuthRepositoryImpl
import com.andy.spotifysdktesting.feature.spotifysdk.data.repository.SpotifyRepositoryImpl
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.PKCEManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.SpotifyRepository
import com.andy.spotifysdktesting.feature.spotifysdk.domain.service.SpotifyApiService
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyTokenManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.AuthRepository
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyAuthViewModel
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val spotifyModule = module {
    single { SpotifyManager(get()) }
    single { SpotifyTokenManager(get()) }
    viewModel { SpotifyViewModel(get(), get()) }
    viewModel { SpotifyAuthViewModel(get()) }

    single { PKCEManager() }

    // 🎯 CLIENTE API CON AUTENTICACIÓN (ApiClient)
    // Definimos el cliente completo aquí para evitar problemas de "Unresolved reference"
    single(named("ApiClient")) {
        val tokenManager: SpotifyTokenManager = get()
        val authRepo: AuthRepository = get()

        HttpClient {
            // 1. Configuración Base (Igual que en koinModule)
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

    // 🎯 INYECCIONES
    single {
        SpotifyApiService(
            client = get(named("ApiClient")), // Usa el cliente con Token
        )
    }

    single<SpotifyRepository> { SpotifyRepositoryImpl(get()) }

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