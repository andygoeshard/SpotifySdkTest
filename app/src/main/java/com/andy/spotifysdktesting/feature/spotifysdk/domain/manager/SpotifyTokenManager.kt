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
        private val KEY_VERIFIER = stringPreferencesKey("code_verifier")

        // 🎯 Margen de seguridad: 5 minutos antes de la expiración real (en milisegundos)
        private const val EXPIRATION_SAFETY_MARGIN_MS = 5 * 60 * 1000L
    }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long
    ) {
        // 🎯 CLAVE: expiresAt es la marca de tiempo futura.
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L) // Usar 'L' para Long

        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS] = accessToken
            refreshToken?.let { prefs[KEY_REFRESH] = it }
            prefs[KEY_EXPIRES] = expiresAt
        }
    }

    suspend fun isAccessTokenExpired(): Boolean {
        // 1. Obtener el tiempo de expiración guardado
        val expirationTime = getExpirationTime()

        // Si es 0L, significa que no hay token o no se ha guardado el tiempo.
        if (expirationTime == 0L) return true

        // 2. Tiempo actual
        val now = System.currentTimeMillis()

        // 3. Comparación: si el tiempo actual es mayor o igual que (la expiración - el margen)
        val isExpired = now >= (expirationTime - EXPIRATION_SAFETY_MARGIN_MS)

        return isExpired
    }


    // --- MÉTODOS DE VERIFIER Y EXISTENTES (Se mantienen) ---

    suspend fun saveVerifier(verifier: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VERIFIER] = verifier
        }
    }

    suspend fun getVerifier(): String? {
        return context.dataStore.data.map { it[KEY_VERIFIER] }.first()
    }

    suspend fun clearVerifier() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_VERIFIER)
        }
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { it[KEY_ACCESS] }.first()
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { it[KEY_REFRESH] }.first()
    }

    // 💡 Método renombrado (era getExpirationTime) para mayor claridad, pero su lógica es correcta.
    suspend fun getExpirationTime(): Long {
        return context.dataStore.data.map { it[KEY_EXPIRES] ?: 0L }.first()
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}