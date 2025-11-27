package com.andy.spotifysdktesting.feature.spotifysdk.data

data class CurrentTrack(
    val trackName: String,
    val artistName: String,
    var imageUri: String?,
    val isPaused: Boolean,
)
