package com.andy.spotifysdktesting.core.location.di

import com.andy.spotifysdktesting.core.location.data.repository.LocationRepositoryImpl
import com.andy.spotifysdktesting.core.location.domain.manager.AILocationDataSource
import com.andy.spotifysdktesting.core.location.domain.manager.DeviceLocationDataSource
import com.andy.spotifysdktesting.core.location.domain.manager.NominatimLocationDataSource
import com.andy.spotifysdktesting.core.location.domain.repository.LocationRepository
import org.koin.dsl.module

val locationModule = module {

    single { DeviceLocationDataSource(get()) }

    single { NominatimLocationDataSource(get()) }

    single { AILocationDataSource(get()) }

    single<LocationRepository> {
        LocationRepositoryImpl(
            deviceSource = get(),
            nominatimSource = get(),
            aiSource = get()
        )
    }
}