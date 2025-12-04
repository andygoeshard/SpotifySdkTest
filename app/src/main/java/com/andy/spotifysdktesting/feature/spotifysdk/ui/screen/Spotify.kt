package com.andy.spotifysdktesting.feature.spotifysdk.ui.screen

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.andy.spotifysdktesting.R
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModel
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModelIntent
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyAuthState
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpotifyPlayerBar(vm: HomeViewModel = koinViewModel()) {

    val state by vm.state.collectAsState()
    val spotifyState = state.spotifyState
    val trackInfo = spotifyState.currentTrack

    if (!spotifyState.isConnected || trackInfo == null) {
        return
    }

    val isPaused = trackInfo.isPaused

    // La barra de control en la parte inferior
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Imagen y Texto
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // 🚨 CAMBIO: Usamos el nuevo Composable del disco giratorio
            VinylDiscImage(
                imageUri = trackInfo.imageUri,
                isPaused = isPaused,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))

            // Columna de Texto
            Column(
                modifier = Modifier.weight(1f) // 🎯 CRÍTICO: La Columna de texto toma el peso restante en su Row
            ) {
                Text(
                    text = trackInfo.trackName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis // 🎯 CRÍTICO: Muestra "..." si no cabe
                )
                Text(
                    text = trackInfo.artistName,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis // 🎯 CRÍTICO: Muestra "..." si no cabe
                )
            }
        }

        // 2. Controles (Play/Pause y Next)
        // Esta Row debe ser ajustada para que tome su espacio intrínseco
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Skip Previous
            // ... (botones se mantienen igual)
            IconButton(onClick = { vm.processIntent(HomeViewModelIntent.OnPreviousSong) }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_skip_previous_24),
                    contentDescription = "Previous",
                    tint = Color.White
                )
            }

            // Botón Play/Pause
            IconButton(onClick = {
                if (isPaused) {
                    vm.processIntent(HomeViewModelIntent.OnPlay)
                } else {
                    vm.processIntent(HomeViewModelIntent.OnPause)
                }
            }) {
                Icon(
                    painter = if (isPaused) painterResource(id = R.drawable.baseline_play_arrow_24) else painterResource(
                        id = R.drawable.baseline_pause_24
                    ),
                    contentDescription = if (isPaused) "Play" else "Pause",
                    tint = Color.White
                )
            }

            // Botón Skip Next
            IconButton(onClick = { vm.processIntent(HomeViewModelIntent.OnNextSong) }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_skip_next_24),
                    contentDescription = "Next",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun VinylDiscImage(imageUri: String?, isPaused: Boolean, modifier: Modifier = Modifier) {
    // Velocidad de rotación: 3 segundos por vuelta (rápido y visible)
    val rotationDuration = 3000

    // Animación de rotación continua
    val rotation = remember { Animatable(0f) }

    // Animación de frenado / inicio
    val brakeFactor = remember { Animatable(1f) } // Factor de velocidad: 1f = velocidad normal

    LaunchedEffect(isPaused) {
        if (!isPaused) {
            // Estado: PLAY
            brakeFactor.stop() // Detener cualquier animación de frenado
            brakeFactor.snapTo(1f) // Asegurar velocidad normal
            rotation.animateTo(
                targetValue = rotation.value + 360f, // Solo un ciclo, pero se repite
                animationSpec = infiniteRepeatable(
                    animation = tween(rotationDuration, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            // Estado: PAUSE
            rotation.stop() // Detener la rotación infinita

            // Animación de desaceleración (Frenado)
            brakeFactor.animateTo(
                targetValue = 0f, // Llevar la velocidad a 0
                animationSpec = tween(
                    durationMillis = 800, // Duración del frenado
                    easing = LinearOutSlowInEasing // Desaceleración suave
                )
            )
        }
    }

    val appliedRotation = rotation.value * brakeFactor.value

    Box(
        modifier = modifier
            .size(40.dp) // Mismo tamaño que antes
            .clip(CircleShape)
            .rotate(appliedRotation)
            .background(Color.Black), // Fondo negro para simular el vinilo
        contentAlignment = Alignment.Center
    ) {
        // Imagen del Album (el disco)
        AsyncImage(
            model = imageUri,
            contentDescription = "Track Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize(0.85f) // Disco un poco más pequeño para dejar el borde negro
                .clip(CircleShape)
        )

        // Círculo Central (el agujero)
        Box(
            modifier = Modifier
                .size(8.dp) // Pequeño círculo central
                .clip(CircleShape)
                .background(Color.White) // Círculo central blanco
        )
    }
}

@Composable
fun ToastError() {
    Toast.makeText(LocalContext.current, "Error connecting to Spotify", Toast.LENGTH_SHORT).show()
}

@Composable
fun AnimTrackImage(imageUri: String?, isPaused: Boolean) {

    var currentRotation by remember { mutableStateOf(0f) }

    val rotation = remember { Animatable(currentRotation) }

    LaunchedEffect(isPaused) {
        if (!isPaused) {
            rotation.animateTo(
                targetValue = currentRotation + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            ) {
                currentRotation = value
            }
        } else {
            if (currentRotation > 0f) {
                rotation.animateTo(
                    targetValue = currentRotation + 50,
                    animationSpec = tween(
                        durationMillis = 1250,
                        easing = LinearOutSlowInEasing
                    )
                ) {
                    currentRotation = value
                }
            }
        }
    }

    AsyncImage(
        model = imageUri,
        contentDescription = "Track Image",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .clip(CircleShape)
            .height(100.dp)
            .width(100.dp)
            .rotate(currentRotation)
    )

}

@Composable
fun TrackText(trackName: String) {
    Text(
        text = trackName,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
}

@Composable
fun ArtistText(artistName: String) {
    Text(
        text = artistName,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp
    )
}
