package com.andy.spotifysdktesting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.andy.spotifysdktesting.ui.screen.SpotifyScreen
import com.andy.spotifysdktesting.ui.theme.SpotifySdkTestingTheme
import com.andy.spotifysdktesting.ui.viewmodel.SpotifyViewModel
import com.spotify.android.appremote.api.SpotifyAppRemote


class MainActivity : ComponentActivity() {

    private val viewModel: SpotifyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SpotifySdkTestingTheme {
                SpotifyScreen(viewModel)
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

