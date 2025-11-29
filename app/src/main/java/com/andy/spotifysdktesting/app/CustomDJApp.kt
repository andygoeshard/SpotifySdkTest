package com.andy.spotifysdktesting.app

import android.app.Application
import com.andy.spotifysdktesting.core.ai.di.aiModule
import com.andy.spotifysdktesting.core.di.koinModule
import com.andy.spotifysdktesting.core.navigation.di.navigationModule
import com.andy.spotifysdktesting.core.tts.di.ttsModule
import com.andy.spotifysdktesting.feature.spotifysdk.di.spotifyModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.fileProperties

class CustomDJApp: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@CustomDJApp)
            androidLogger()
            androidFileProperties()
            modules(
                koinModule,
                ttsModule,
                aiModule,
                spotifyModule,
                navigationModule
            )
        }
    }

}