package com.andy.spotifysdktesting.feature.spotifysdk.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyAuthViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpotifyAuthScreen(
    vm: SpotifyAuthViewModel = koinViewModel()
) {
    val state by vm.uiState.collectAsState()

    // 🌐 Si genera URL de login, abrir navegador
    val context = LocalContext.current
    LaunchedEffect(state.authUrl) {
        if (state.authUrl.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.authUrl))
            context.startActivity(intent)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Button(onClick = { vm.startLogin() }) {
            Text("Login con Spotify")
        }

        if (state.isLoggedIn) {
            Text("✔ Logueado!")
            Text("Token: ${state.accessToken}")
        }
    }
}
