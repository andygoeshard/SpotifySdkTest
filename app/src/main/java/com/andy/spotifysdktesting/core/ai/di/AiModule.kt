package com.andy.spotifysdktesting.core.ai.di

import com.andy.spotifysdktesting.BuildConfig
import com.andy.spotifysdktesting.core.ai.data.provider.GeminiClientProvider
import com.andy.spotifysdktesting.core.ai.domain.AiMusicBrain
import com.andy.spotifysdktesting.core.ai.presentation.viewmodel.AiViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val aiModule = module {
    single { GeminiClientProvider(apiKey = BuildConfig.GEMINI_API_KEY) }
    single { AiMusicBrain(get(), get()) }
    viewModel { AiViewModel(get(),get()) }
}