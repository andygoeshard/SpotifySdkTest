package com.andy.spotifysdktesting.core.dj.domain.intent

sealed interface DjIntent {
    object Start : DjIntent
    object Stop : DjIntent
    data class OnSpotifyCodeReceived(val code: String) : DjIntent
    object ExplainCurrentSong : DjIntent
    object NextTrackIA : DjIntent

    object SpotifyTrackChanged : DjIntent

    object OnPlay : DjIntent
    object OnPause : DjIntent
    object OnNextTrack : DjIntent
    object OnPreviousTrack : DjIntent
}

