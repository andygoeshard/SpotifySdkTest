package com.andy.spotifysdktesting.core.dj.domain.state

import com.andy.spotifysdktesting.core.dj.domain.model.NextTrackCache
import com.andy.spotifysdktesting.feature.spotifysdk.domain.model.CurrentTrack

data class DjState(
    val enabled: Boolean = false,

    val thinking: Boolean = false,
    val speaking: Boolean = false,

    val currentText: String = "",
    val statusText: String = "",

    val messageHistory: List<String> = emptyList(),

    val currentTrack: CurrentTrack? = null,

    val nextTrack: NextTrackCache? = null,

    val isSdkConnected: Boolean = false,

    val isLoggedIn: Boolean = false,
    val accessToken: String? = null,
    val authUrl: String = "",
)
