package com.andy.spotifysdktesting.core.userprefs.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPrefs(
    val activeRadioId: String? = null,
    val radios: List<CustomRadio> = emptyList()
)
