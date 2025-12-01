package com.andy.spotifysdktesting.core.tts.di

import androidx.media3.exoplayer.ExoPlayer
import com.andy.spotifysdktesting.BuildConfig
import com.andy.spotifysdktesting.core.tts.data.AndroidTtsEngine
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

    single {
        ExoPlayer.Builder(androidContext()).build()
    }

    single<TtsEngine>(named("eleven")) {
        ElevenLabsEngine(
            apiKey = BuildConfig.ELEVEN_API_KEY,
            client = get(named("AuthClient"))
        )
    }
    single<TtsEngine>(named("android")) {
        AndroidTtsEngine(androidContext())
    }

    single {
        TtsManager(
            eleven = get(named("eleven")),
            androidNative = get(named("android"))
        )
    }

    single { AudioPlayer(
        androidContext(),
        exoPlayer = get()
    ) }
    viewModel { TtsViewModel(
        get(),
        audioPlayer = get()
    ) }

}