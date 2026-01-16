package com.andy.spotifysdktesting.core.ai.di

import com.andy.spotifysdktesting.BuildConfig
import com.andy.spotifysdktesting.core.ai.data.repository.DeepSeekClientImpl
import com.andy.spotifysdktesting.core.ai.data.repository.GeminiClientImpl
import com.andy.spotifysdktesting.core.ai.data.repository.GroqClientImpl
import com.andy.spotifysdktesting.core.ai.domain.repository.AiClient
import com.andy.spotifysdktesting.core.ai.domain.manager.AiManager
import org.koin.core.qualifier.named
import org.koin.dsl.module

val aiModule = module {
    single(named("gemini")) {
        GeminiClientImpl(BuildConfig.GEMINI_API_KEY)
    }

    single(named("groq")) {
        GroqClientImpl(
            apiKey = BuildConfig.GROQ_API_KEY,
            httpClient = get(named("AuthClient"))
        )
    }

    single(named("deepseek")) {
        DeepSeekClientImpl(
            apiKey = BuildConfig.DEEPSEEK_API_KEY,
            httpClient = get(named("AuthClient"))
        )
    }

    single<AiClient> { get(named("gemini")) }
    single { AiManager(aiClient = get()) }
}