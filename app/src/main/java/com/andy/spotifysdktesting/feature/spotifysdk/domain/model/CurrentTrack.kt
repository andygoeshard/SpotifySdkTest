package com.andy.spotifysdktesting.feature.spotifysdk.domain.model

data class CurrentTrack(
    val trackName: String,
    val artistName: String,
    var imageUri: String?,
    val isPaused: Boolean,
    val id: String?,
)