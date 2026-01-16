package com.andy.spotifysdktesting.core.userprefs.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.andy.spotifysdktesting.core.userprefs.domain.model.CustomRadio
import com.andy.spotifysdktesting.core.userprefs.domain.model.UserPrefs
import com.andy.spotifysdktesting.core.userprefs.domain.repository.UserPreferencesDataSource
import com.andy.spotifysdktesting.core.userprefs.domain.repository.UserPrefsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class UserPrefsRepositoryImpl(
    private val dataSource: UserPreferencesDataSource
) : UserPrefsRepository {

    override suspend fun getUserPrefs(): UserPrefs =
        dataSource.loadUserPrefs()

    override suspend fun saveUserPrefs(prefs: UserPrefs) =
        dataSource.saveUserPrefs(prefs)

    override suspend fun getActiveRadio(): CustomRadio? {
        val prefs = getUserPrefs()
        return null
    }

    override suspend fun setActiveRadio(radioId: String) {
        val prefs = getUserPrefs()
        saveUserPrefs(prefs.copy(activeRadioId = radioId))
    }

    override suspend fun createRadio(radio: CustomRadio) {
        val prefs = getUserPrefs()
    }

    override suspend fun updateRadio(radio: CustomRadio) {
        val prefs = getUserPrefs()
    }

    override suspend fun deleteRadio(radioId: String) {
        val prefs = getUserPrefs()
    }
}
