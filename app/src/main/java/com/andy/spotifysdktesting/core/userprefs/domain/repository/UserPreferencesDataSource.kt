package com.andy.spotifysdktesting.core.userprefs.domain.repository

import com.andy.spotifysdktesting.core.userprefs.domain.model.UserPrefs

interface UserPreferencesDataSource {
    suspend fun saveUserPrefs(prefs: UserPrefs)
    suspend fun loadUserPrefs(): UserPrefs
}