package com.andy.spotifysdktesting.core.userprefs.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.andy.spotifysdktesting.core.userprefs.domain.model.UserPrefs
import com.andy.spotifysdktesting.core.userprefs.domain.repository.UserPreferencesDataSource
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class UserPreferencesDataSourceImpl(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesDataSource {

    override suspend fun saveUserPrefs(prefs: UserPrefs) {
        dataStore.edit { settings ->
            settings[stringKey("user_prefs")] = Json.encodeToString(prefs)
        }
    }

    override suspend fun loadUserPrefs(): UserPrefs {
        val json = dataStore.data.first()[stringKey("user_prefs")]
        return json?.let { Json.decodeFromString(it) } ?: UserPrefs()
    }

    private fun stringKey(key: String) = stringPreferencesKey(key)
}
