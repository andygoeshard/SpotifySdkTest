package com.andy.spotifysdktesting.feature.spotifysdk.domain.model

import com.andy.spotifysdktesting.feature.spotifysdk.domain.helper.extractIdFromUri

data class CurrentTrack(
    val trackName: String,
    val artistName: String,
    var imageUri: String?,
    val isPaused: Boolean,
    val id: String?,
)