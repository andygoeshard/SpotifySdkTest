package com.andy.spotifysdktesting.core.tts.di

import com.andy.spotifysdktesting.BuildConfig
import com.andy.spotifysdktesting.core.tts.data.ElevenLabsEngine
import com.andy.spotifysdktesting.core.tts.domain.TtsEngine
import com.andy.spotifysdktesting.core.tts.domain.TtsManager
import com.andy.spotifysdktesting.core.tts.playback.AudioPlayer
import com.andy.spotifysdktesting.core.tts.presentation.viewmodel.TtsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module


val ttsModule = module {

    single<TtsEngine>(named("eleven")) {
        ElevenLabsEngine(
            apiKey = BuildConfig.ELEVEN_API_KEY,
            client = get(named("AuthClient"))
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
    viewModel { TtsViewModel(
        get(),
        audioPlayer = get()
    ) }

}