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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeEvent
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModel
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModelIntent
import com.andy.spotifysdktesting.feature.spotifysdk.ui.screen.SpotifyPlayerBar
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
@Composable
fun HomeScreen(vm: HomeViewModel = koinViewModel()) {

    // 🎯 Observa el ÚNICO ESTADO COMPUESTO
    val state by vm.state.collectAsStateWithLifecycle() // Mejor que collectAsState
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Efecto Lateral (Deep Link): Se ejecuta solo si la URL de Auth cambia
    LaunchedEffect(state.authState.authUrl) {
        if (state.authState.authUrl.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.authState.authUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    // 🎯 NUEVO: Observación del flujo de eventos (Manejo de re-login y Snackbar)
    LaunchedEffect(vm.event) {
        vm.event.collectLatest { event ->
            when (event) {
                is HomeEvent.NavigateToLogin -> {
                    // Limpiamos el URL para que se reactive el botón de Login
                    // La limpieza de tokens se hace en el HomeViewModel
                    snackbarHostState.showSnackbar("Sesión expirada. Por favor, inicia sesión de nuevo.")
                }
                is HomeEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // ------------------------------------------------

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

                // 1. Contenido Principal de la Pantalla (Loggeado/Desloggeado)
                Column(
                    modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
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
                                Text("Spotify requiere tu atención.", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { vm.processIntent(HomeViewModelIntent.StartLogin) }) {
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
                            }
                        }
                    }
                }

                // 2. Componente de Spotify (BottomBar)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    SpotifyPlayerBar(vm = vm)
                }
            }
        }
    )
}