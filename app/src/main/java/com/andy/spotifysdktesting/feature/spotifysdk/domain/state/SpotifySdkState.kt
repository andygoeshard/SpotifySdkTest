package com.andy.spotifysdktesting.feature.spotifysdk.domain.state

import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack

data class SpotifySdkState(
    val isConnected: Boolean = false,
    val isPaused: Boolean = true,
    val currentTrack: CurrentTrack? = null
)
