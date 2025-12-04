package com.andy.spotifysdktesting.core.navigation.domain

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "DjStateManager"

class DjStateManager {

    private val _currentDjText = MutableStateFlow("Esperando inicio de sesión...")
    val currentDjText: StateFlow<String> = _currentDjText.asStateFlow()

    private val _messageHistory = MutableStateFlow<List<String>>(emptyList())
    val messageHistory: StateFlow<List<String>> = _messageHistory.asStateFlow()
    private val MAX_HISTORY_SIZE = 15

    private fun addMessageToHistory(message: String) {
        _messageHistory.update { currentList ->
            // Agregamos el nuevo mensaje al final de la lista
            val newList = currentList + message

            // Limitamos la lista para evitar que crezca indefinidamente
            if (newList.size > MAX_HISTORY_SIZE) {
                // Quitamos los mensajes más viejos (los primeros)
                newList.takeLast(MAX_HISTORY_SIZE)
            } else {
                newList
            }
        }
    }




    fun updateDjText(newText: String) {
        addMessageToHistory(newText)
        _currentDjText.update { newText }
        Log.d(TAG, "DJ State actualizado con: $newText")
    }

    fun clearDjText() {
        _currentDjText.update { "DJ inactivo." }
        _messageHistory.update { emptyList() }
        Log.d(TAG, "DJ State limpiado.")
    }
}