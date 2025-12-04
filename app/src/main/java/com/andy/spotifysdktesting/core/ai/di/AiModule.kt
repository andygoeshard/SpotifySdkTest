package com.andy.spotifysdktesting.core.ai.di

import com.andy.spotifysdktesting.BuildConfig
import com.andy.spotifysdktesting.core.ai.data.provider.GeminiClientImpl
import com.andy.spotifysdktesting.core.ai.data.provider.GroqClientImpl
import com.andy.spotifysdktesting.core.ai.domain.AiClient
import com.andy.spotifysdktesting.core.ai.domain.AiMusicBrain
import com.andy.spotifysdktesting.core.ai.presentation.viewmodel.AiViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val aiModule = module {
    single {
        GroqClientImpl(
            apiKey = BuildConfig.GROQ_API_KEY,
            httpClient = get(named("AuthClient"))
        )
    }
    single { GeminiClientImpl(BuildConfig.GEMINI_API_KEY) }
    single<AiClient> { get<GeminiClientImpl>() }
    single { AiMusicBrain(aiClient = get()) }
    viewModel { AiViewModel(get(),get(),get(),get()) }
}