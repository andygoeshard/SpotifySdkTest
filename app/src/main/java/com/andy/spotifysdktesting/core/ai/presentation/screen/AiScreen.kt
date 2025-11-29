package com.andy.spotifysdktesting.core.ai.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andy.spotifysdktesting.core.ai.presentation.viewmodel.AiViewModel
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun AiScreen(viewModel: AiViewModel = koinViewModel()) {

    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ---- BOTÓN PRINCIPAL ----
        Button(
            onClick = { viewModel.startAi("relax") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎧 Empezar IA musical")
        }

        Spacer(Modifier.height(16.dp))

        // ---- LOADING ----
        if (state.loading) {
            Text("⏳ Pensando la próxima canción...")
            Spacer(Modifier.height(16.dp))
        }

        // ---- RESULTADOS DE LA IA ----
        if (state.aiSong.isNotBlank()) {
            Text("🎶 Próxima canción sugerida:")
            Text(state.aiSong, modifier = Modifier.padding(bottom = 12.dp))
        }

        if (state.aiReason.isNotBlank()) {
            Text("🤖 Razón:")
            Text(state.aiReason, modifier = Modifier.padding(bottom = 12.dp))
        }

        if (state.aiRaw.isNotBlank()) {
            Text("🧪 RAW:")
            Text(state.aiRaw, modifier = Modifier.padding(bottom = 16.dp))
        }

        Spacer(Modifier.height(24.dp))


        // ---- CHAT ----
        var message by remember { mutableStateOf("") }

        Text("💬 Chat con la IA")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    if (message.isNotBlank()) {
                        viewModel.chat(message)
                        message = ""
                    }
                }
            ) {
                Text("Enviar")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (state.chatResponse.isNotBlank()) {
            Text("🤖 IA responde:")
            Text(state.chatResponse)
        }
    }
}