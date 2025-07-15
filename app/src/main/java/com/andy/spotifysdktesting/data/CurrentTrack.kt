package com.andy.spotifysdktesting.data

data class CurrentTrack(
    val trackName: String,
    val artistName: String,
    var imageUri: String?,
    val isPaused: Boolean,
)
