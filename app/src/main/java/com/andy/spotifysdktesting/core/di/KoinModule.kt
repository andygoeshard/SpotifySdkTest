package com.andy.spotifysdktesting.core.di

import com.andy.spotifysdktesting.BuildConfig
import com.andy.spotifysdktesting.core.tts.data.CoquiEngine
import com.andy.spotifysdktesting.core.tts.data.ElevenLabsEngine
import com.andy.spotifysdktesting.core.tts.domain.TtsEngine
import com.andy.spotifysdktesting.core.tts.domain.TtsManager
import com.andy.spotifysdktesting.core.tts.playback.AudioPlayer
import com.andy.spotifysdktesting.core.tts.presentation.viewmodel.DjViewModel
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named


val koinModule = module {

    single {
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                        explicitNulls = false
                    }
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 25_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 25_000
            }
        }
    }


    // TtsEngines
    single<TtsEngine>(named("coqui")) {
        CoquiEngine(
            apiKey = BuildConfig.COQUI_API_KEY,
            client = get()
        )
    }

    single<TtsEngine>(named("eleven")) {
        ElevenLabsEngine(
            apiKey = BuildConfig.ELEVEN_API_KEY,
            client = get()
        )
    }

    // TtsManager
    single {
        TtsManager(
            eleven = get(named("eleven"))
        )
    }

    // AudioPlayer (si tiene dependencias, las agregás)
    single { AudioPlayer(androidContext()) }

    viewModel { SpotifyViewModel(get()) }
    viewModel { DjViewModel(
        get(),
        audioPlayer = get()
    ) }
}