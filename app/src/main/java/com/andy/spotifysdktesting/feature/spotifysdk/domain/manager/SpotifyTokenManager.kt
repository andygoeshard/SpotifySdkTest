package com.andy.spotifysdktesting.feature.spotifysdk.domain.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SpotifyTokenManager(private val context: Context) {

    private val Context.dataStore by preferencesDataStore(name = "spotify_tokens")

    companion object {
        private val KEY_ACCESS = stringPreferencesKey("access_token")
        private val KEY_REFRESH = stringPreferencesKey("refresh_token")
        private val KEY_EXPIRES = longPreferencesKey("expires_at")
        // NUEVA CLAVE PARA EL VERIFIER
        private val KEY_VERIFIER = stringPreferencesKey("code_verifier")
    }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long
    ) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000)

        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS] = accessToken
            refreshToken?.let { prefs[KEY_REFRESH] = it }
            prefs[KEY_EXPIRES] = expiresAt
        }
    }

    // --- MÉTODOS DE VERIFIER (NUEVO) ---

    suspend fun saveVerifier(verifier: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VERIFIER] = verifier
        }
    }

    suspend fun getVerifier(): String? {
        return context.dataStore.data.map { it[KEY_VERIFIER] }.first()
    }

    // Opcional: Para limpiar el verifier después de usarlo (buena práctica)
    suspend fun clearVerifier() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_VERIFIER)
        }
    }

    // --- MÉTODOS EXISTENTES ---

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { it[KEY_ACCESS] }.first()
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { it[KEY_REFRESH] }.first()
    }

    suspend fun getExpirationTime(): Long {
        return context.dataStore.data.map { it[KEY_EXPIRES] ?: 0L }.first()
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}