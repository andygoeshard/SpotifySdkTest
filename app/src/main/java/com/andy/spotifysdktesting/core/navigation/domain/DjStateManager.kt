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

    fun updateDjText(newText: String) {
        _currentDjText.update { newText }
        Log.d(TAG, "DJ State actualizado con: $newText")
    }

    fun clearDjText() {
        _currentDjText.update { "DJ inactivo." }
        Log.d(TAG, "DJ State limpiado.")
    }
}