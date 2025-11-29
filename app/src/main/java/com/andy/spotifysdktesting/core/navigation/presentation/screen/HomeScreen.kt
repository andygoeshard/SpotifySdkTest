package com.andy.spotifysdktesting.core.navigation.presentation.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(vm: HomeViewModel = koinViewModel()) {

    val isLogged by vm.isLoggedIn.collectAsState()
    val isConnected by vm.isSdkConnected.collectAsState()
    val authUrl by vm.authUrl.collectAsState()
    val track by vm.currentTrack.collectAsState()
    val ai by vm.aiState.collectAsState()
    val dj by vm.djState.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(authUrl) {

        if (authUrl.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    // ------------------------------------------------

    // Manejamos los tres estados de la aplicación
    when {
        // Estado 1: Deslogueado (Token no obtenido)
        !isLogged -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = { vm.startLogin() }) {
                    Text("Login con Spotify")
                }
            }
        }

        // Estado 2: Logueado, pero el SDK aún no está conectado
        !isConnected -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Muestra un mensaje de espera mientras el HomeViewModel espera la conexión.
                Text("🔌 Conectando a Spotify SDK...")
                Text("Asegúrate de tener la app de Spotify abierta.")
            }
        }

        // Estado 3: Logueado y Conectado (Listo para usar)
        else -> {
            Column {
                Text("Status: ✅ Conectado") // Feedback de que el SDK está OK
                Text("Track actual: ${track?.trackName ?: "Ninguno"}")

                Button(onClick = { vm.askAiForNextSong("energético") }) {
                    Text("IA: Dame la próxima canción")
                }

                Text("Sugerencia IA: ${ai.aiSong}")
                Text("Razón: ${ai.aiReason}")

                Button(onClick = { vm.djExplainSong() }) {
                    Text("DJ: Explicame la canción")
                }

                Button(onClick = { vm.djCustom("Esta lista la armé para vos, Andy.") }) {
                    Text("DJ custom")
                }
            }
        }
    }
}