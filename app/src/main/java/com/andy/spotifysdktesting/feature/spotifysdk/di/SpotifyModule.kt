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
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val spotifyModule = module{
    single { SpotifyManager(get()) }
    single { SpotifyTokenManager(get()) }
    viewModel { SpotifyViewModel(get()) }
    viewModel { SpotifyAuthViewModel(get()) }
    single { SpotifyApiService(get(), get()) }
    single<SpotifyRepository> { SpotifyRepositoryImpl(get()) }

    single { PKCEManager() }

    single<AuthRepository> {
        AuthRepositoryImpl(
            context = get(),
            pkce = get(),
            clientId = BuildConfig.SPOTIFY_CLIENT_ID,
            redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI,
            client = get(),
            get()
        )
    }

}