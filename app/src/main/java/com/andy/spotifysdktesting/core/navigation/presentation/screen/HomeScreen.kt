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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.dj.authUrl) {
        if (state.dj.authUrl.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.dj.authUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    LaunchedEffect(vm.event) {
        vm.event.collectLatest { event ->
            when (event) {
                is HomeEvent.NavigateToLogin -> {
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
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), // Más espacio para la barra de abajo
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when {
                        // Estado 1: Deslogueado
                        !state.dj.isLoggedIn -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Spotify requiere tu atención.", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { vm.processIntent(HomeViewModelIntent.StartLogin) }) {
                                    Text("Login con Spotify")
                                }
                            }
                        }

                        // Estado 2: Conectando SDK
                        !state.dj.isSdkConnected -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔌 Conectando a Spotify SDK...")
                                Text("Asegúrate de tener la app de Spotify abierta.")
                            }
                        }

                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // ----------------------------------------------------
                                // 🚨 CAMBIO MAYOR: LazyColumn para el Historial de Chat
                                ChatHistory(messages = state.dj.messageHistory)
                                Spacer(modifier = Modifier.height(24.dp))
                                // ----------------------------------------------------

                                // El resto de los elementos los ponemos abajo
                                Text("Status: ✅ Conectado", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(16.dp))

                                // ... (El resto de la info del track y botones se mantienen igual) ...

                                // Esto lo movemos para que los botones queden abajo del chat
                                Text("Track: ${state.dj.currentTrack?.trackName ?: "Ninguno"} de ${state.dj.currentTrack?.artistName ?: "Desconocido"}",
                                    style = MaterialTheme.typography.bodyMedium)

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(onClick = { vm.processIntent(HomeViewModelIntent.ExplainCurrentSong) }) {
                                    Text("🎤 DJ: Explicame la canción")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(onClick = { vm.processIntent(HomeViewModelIntent.NextTrackIA) }) {
                                    Text("🎧 IA: Dame la próxima canción")
                                }
                            }
                        }
                    }
                }

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

@Composable
fun ChatHistory(messages: List<String>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .padding(vertical = 8.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom) // Espacio entre mensajes
    ) {
        items(messages.reversed()) { message -> // Iteramos sobre la lista
            DjMessageCard(text = message)
        }
    }
}
@Composable
fun DjMessageCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "AI DJ dice:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}