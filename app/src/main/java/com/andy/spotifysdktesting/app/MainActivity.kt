package com.andy.spotifysdktesting.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.andy.spotifysdktesting.feature.spotifysdk.ui.screen.SpotifyScreen
import com.andy.spotifysdktesting.app.ui.theme.SpotifySdkTestingTheme
import com.andy.spotifysdktesting.core.tts.presentation.screen.DjScreen
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyViewModel

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SpotifySdkTestingTheme {
                DjScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

}