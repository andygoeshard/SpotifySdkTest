package com.andy.spotifysdktesting.core.navigation.presentation.viewmodel

import android.R.attr.data
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andy.spotifysdktesting.core.dj.domain.intent.DjIntent
import com.andy.spotifysdktesting.core.dj.domain.manager.DjManager
import com.andy.spotifysdktesting.core.dj.domain.state.DjState
import com.andy.spotifysdktesting.core.service.DjService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

/* ─────────────────────────── STATE UI ─────────────────────────── */

data class HomeViewState(
    val dj: DjState
)

/* ─────────────────────────── INTENTS UI ─────────────────────────── */

sealed class HomeViewModelIntent {
    data object StartDj : HomeViewModelIntent()
    data object StopDj : HomeViewModelIntent()
    data object StartLogin : HomeViewModelIntent()
    data object OnSpotifyLoginClicked : HomeViewModelIntent()
    data class OnSpotifyCodeReceived(val code: String) : HomeViewModelIntent()
    data object ExplainCurrentSong : HomeViewModelIntent()
    data object NextTrackIA : HomeViewModelIntent()

    data object OnPlay : HomeViewModelIntent()
    data object OnPause : HomeViewModelIntent()
    data object OnNextTrack : HomeViewModelIntent()
    data object OnPreviousTrack : HomeViewModelIntent()

    data object SpotifyTrackChanged : HomeViewModelIntent()
}

/* ─────────────────────────── EVENTS ─────────────────────────── */

sealed class HomeEvent {
    data class ShowSnackbar(val message: String) : HomeEvent()
    data class NavigateToLogin(val message: String) : HomeEvent()
}

/* ─────────────────────────── VIEWMODEL ─────────────────────────── */

class HomeViewModel(
    private val context: Context,
    private val djManager: DjManager
) : ViewModel() {

    private val _event = Channel<HomeEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    val state: StateFlow<HomeViewState> =
        djManager.state
            .map { HomeViewState(dj = it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = HomeViewState(djManager.state.value)
            )

    init{
        startDjService()
    }

    /* ─────────────────────────── INTENT HANDLER ─────────────────────────── */

    fun processIntent(intent: HomeViewModelIntent) {
        when (intent) {

            HomeViewModelIntent.StartDj -> {
                startDjService()
                djManager.onIntent(DjIntent.Start)
            }

            HomeViewModelIntent.OnSpotifyLoginClicked -> {
                djManager.startLogin()
            }
            HomeViewModelIntent.StartLogin -> {
                djManager.startLogin()
            }

            is HomeViewModelIntent.OnSpotifyCodeReceived -> {
                djManager.onIntent(DjIntent.OnSpotifyCodeReceived(intent.code))
            }

            HomeViewModelIntent.StopDj -> {
                djManager.onIntent(DjIntent.Stop)
                stopDjService()
            }

            HomeViewModelIntent.ExplainCurrentSong ->
                djManager.onIntent(DjIntent.ExplainCurrentSong)

            HomeViewModelIntent.NextTrackIA ->
                djManager.onIntent(DjIntent.NextTrackIA)

            HomeViewModelIntent.SpotifyTrackChanged ->
                djManager.onIntent(DjIntent.SpotifyTrackChanged)

            HomeViewModelIntent.OnPlay ->
                djManager.onIntent(DjIntent.OnPlay)
            HomeViewModelIntent.OnPause ->
                djManager.onIntent(DjIntent.OnPause)
            HomeViewModelIntent.OnNextTrack ->
                djManager.onIntent(DjIntent.OnNextTrack)
            HomeViewModelIntent.OnPreviousTrack ->
                djManager.onIntent(DjIntent.OnPreviousTrack)
        }
    }

    /* ─────────────────────────── SERVICE CONTROL ─────────────────────────── */

    private fun startDjService() {
        val intent = Intent(context, DjService::class.java).apply {
            action = DjService.ACTION_START
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopDjService() {
        val intent = Intent(context, DjService::class.java).apply {
            action = DjService.ACTION_STOP
        }
        context.startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        djManager.onIntent(DjIntent.Stop)
    }
}