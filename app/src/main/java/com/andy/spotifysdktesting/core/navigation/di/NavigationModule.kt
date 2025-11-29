package com.andy.spotifysdktesting.core.navigation.di

import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModel
import org.koin.dsl.module

val navigationModule = module {
    single { HomeViewModel(
        get(),
        ai = get(),
        dj = get(),
        auth = get()
    ) }
}