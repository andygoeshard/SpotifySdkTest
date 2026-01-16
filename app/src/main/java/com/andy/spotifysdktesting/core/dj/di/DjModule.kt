package com.andy.spotifysdktesting.core.dj.di

import com.andy.spotifysdktesting.core.dj.domain.manager.DjManager
import com.andy.spotifysdktesting.core.service.DjService
import org.koin.dsl.module

val djModule = module{
    single {
        DjManager(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
}