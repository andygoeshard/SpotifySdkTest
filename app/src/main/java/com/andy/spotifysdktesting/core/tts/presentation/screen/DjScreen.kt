package com.andy.spotifysdktesting.core.tts.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andy.spotifysdktesting.core.tts.presentation.viewmodel.DjEvent
import com.andy.spotifysdktesting.core.tts.presentation.viewmodel.DjViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DjScreen(viewModel: DjViewModel = koinViewModel()) {

    val state = viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "IA DJ",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            onClick = { viewModel.onEvent(DjEvent.PlayIntro) },
            enabled = !state.value.loading
        ) { Text("Presentación") }

        Button(
            onClick = { viewModel.onEvent(DjEvent.ExplainSong) },
            enabled = !state.value.loading
        ) { Text("Explicar canción") }

        var custom by remember { mutableStateOf("") }

        OutlinedTextField(
            value = custom,
            onValueChange = { custom = it },
            label = { "Decir texto" }
        )

        Button(
            onClick = { viewModel.onEvent(DjEvent.CustomText(custom)) },
            enabled = !state.value.loading && custom.isNotBlank()
        ) { Text("Hablar") }

        if (state.value.loading) {
            Text("Generando voz...", color = Color.Gray)
        }
    }
}