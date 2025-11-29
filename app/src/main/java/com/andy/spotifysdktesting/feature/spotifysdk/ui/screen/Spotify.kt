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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.andy.spotifysdktesting.R
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpotifyScreen(viewModel: SpotifyViewModel =  koinViewModel()) {

    var showDialog by remember { mutableStateOf(false) }
    val isConnected by viewModel.isConnected.collectAsState()
    val trackInfo by viewModel.currentTrack.collectAsState()

    IconButton(onClick = {
        viewModel.connectToSpotify()
    }) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_music_note_24),
            contentDescription = "Music Icon",
            tint = Color.White,
            modifier = Modifier
                .background(Color.Gray, shape = CircleShape)
                .padding(10.dp)
        )
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            showDialog = true
            viewModel.observeCurrentTrack()
        }
    }

    if (showDialog) {
        if (trackInfo != null) {
            MusicControlDialog(
                onDismissRequest = {
                    showDialog = false
                    viewModel.disconnectFromSpotify()
                },
                currentTrack = trackInfo!!,
                viewModel
            )
        } else {
            ToastError()
            showDialog = false
            viewModel.disconnectFromSpotify()
        }
    }

}

@Composable
fun MusicControlDialog(
    onDismissRequest: () -> Unit,
    currentTrack: CurrentTrack,
    viewModel: SpotifyViewModel
) {
    Dialog(onDismissRequest = onDismissRequest) {

        var isPaused by remember { mutableStateOf(currentTrack.isPaused) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxHeight()
                .fillMaxWidth()
                .clickable { onDismissRequest() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.35f)
                    .align(Alignment.Center)
                    .background(Color.Black, shape = MaterialTheme.shapes.medium)
                    .clickable(false) {}
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier
                                .clickable { onDismissRequest() }
                                .padding(0.dp, 0.dp, 16.dp, 0.dp),
                            tint = Color.White
                        )
                    }

                    AnimTrackImage(currentTrack.imageUri, currentTrack.isPaused)

                    Spacer(modifier = Modifier.height(16.dp))

                    TrackText(trackName = currentTrack.trackName)
                    ArtistText(artistName = currentTrack.artistName)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = {
                            viewModel.spotifyManager.previous()
                            isPaused = false
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_skip_previous_24),
                                contentDescription = "Previous",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = {
                            isPaused = !isPaused
                            if (isPaused) viewModel.spotifyManager.pause() else viewModel.spotifyManager.play()
                        }) {
                            Icon(
                                painter = if (!isPaused) painterResource(id = R.drawable.baseline_pause_24) else painterResource(
                                    id = R.drawable.baseline_play_arrow_24
                                ),
                                contentDescription = if (isPaused) "Pause" else "Play",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = {
                            viewModel.spotifyManager.next()
                            isPaused = false
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_skip_next_24),
                                contentDescription = "Next",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

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
fun ToastError() {

    Toast.makeText(LocalContext.current, "Error connecting to Spotify", Toast.LENGTH_SHORT).show()

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
