package com.andy.spotifysdktesting.core.navigation.di

import com.andy.spotifysdktesting.core.navigation.domain.DjStateManager
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val navigationModule = module {
    single { HomeViewModel(
        get(),
        ai = get(),
        tts = get(),
        auth = get(),
        context = androidContext(),
        get()
    ) }

    single { DjStateManager() }
}