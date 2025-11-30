package com.andy.spotifysdktesting.core.navigation.presentation.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModel
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModelIntent
import com.andy.spotifysdktesting.feature.spotifysdk.ui.screen.SpotifyPlayerBar
import org.koin.compose.viewmodel.koinViewModel
@Composable
fun HomeScreen(vm: HomeViewModel = koinViewModel()) {

    // 🎯 Observa el ÚNICO ESTADO COMPUESTO
    val state by vm.state.collectAsState()

    val context = LocalContext.current

    // Efecto Lateral (Deep Link): Se ejecuta solo si la URL de Auth cambia
    LaunchedEffect(state.authState.authUrl) {
        if (state.authState.authUrl.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.authState.authUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    // ------------------------------------------------

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Contenido Principal de la Pantalla (Loggeado/Desloggeado)
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 60.dp), // Espacio para la BottomBar/SpotifyScreen
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                // Estado 1: Deslogueado (Token no obtenido)
                !state.authState.isLoggedIn -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = { vm.processIntent(HomeViewModelIntent.StartLogin) }) { // Usamos Intent
                            Text("Login con Spotify")
                        }
                    }
                }

                // Estado 2: Logueado, pero el SDK aún no está conectado
                !state.spotifyState.isConnected -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🔌 Conectando a Spotify SDK...")
                        Text("Asegúrate de tener la app de Spotify abierta.")
                    }
                }

                // Estado 3: Logueado y Conectado (Listo para usar)
                else -> {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Status: ✅ Conectado", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Track actual: ${state.spotifyState.currentTrack?.trackName ?: "Ninguno"}")

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = { vm.processIntent(HomeViewModelIntent.AskAiForNextSong) }) {
                            Text("IA: Dame la próxima canción")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Sugerencia IA: ${state.aiState.aiSong}", style = MaterialTheme.typography.bodyMedium)
                        Text("Razón: ${state.aiState.aiReason}", style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { vm.processIntent(HomeViewModelIntent.DjExplainSong) }) {
                            Text("DJ: Explicame la canción")
                        }

                        // NOTA: djCustom necesita un Intent específico si quieres que siga MVI
                        // Button(onClick = { vm.djCustom("Esta lista la armé para vos, Andy.") }) { Text("DJ custom") }
                    }
                }
            }
        }

        // 2. Componente de Spotify (que contendrá el botón y el Dialog)
        // Lo colocamos alineado abajo para que actúe como una BottomBar de control.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // El SpotifyScreen se coloca inmediatamente arriba de donde iría la BottomBar
            SpotifyPlayerBar(vm = vm) // Le pasamos el HomeViewModel y el AuthState

            // Asumiendo que aquí iría una BottomBar real...
            // BottomNavigationBar()
        }
    }
}