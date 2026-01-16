package com.andy.spotifysdktesting.core.userprefs.domain.repository

import com.andy.spotifysdktesting.core.userprefs.domain.model.CustomRadio
import com.andy.spotifysdktesting.core.userprefs.domain.model.UserPrefs
import kotlinx.coroutines.flow.Flow


interface UserPrefsRepository {

    suspend fun getUserPrefs(): UserPrefs
    suspend fun saveUserPrefs(prefs: UserPrefs)

    suspend fun getActiveRadio(): CustomRadio?
    suspend fun setActiveRadio(radioId: String)

    suspend fun createRadio(radio: CustomRadio)
    suspend fun updateRadio(radio: CustomRadio)
    suspend fun deleteRadio(radioId: String)
}