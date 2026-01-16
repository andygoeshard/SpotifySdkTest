package com.andy.spotifysdktesting.feature.spotifywebapi.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrackRecommendation(
    val uri: String,
    val name: String,
    val artist: String
)

@Serializable
data class PagingObject(
    val items: List<TrackItem> = emptyList(),
    val next: String? = null,
    val total: Int? = null
)

@Serializable
data class TrackItem(
    val track: SimplifiedTrack? = null, // Para Recently Played
    val name: String? = null,           // Para Top Tracks (a veces es directo)
    val uri: String? = null,
    val artists: List<ArtistItem> = emptyList()
)

@Serializable
data class SimplifiedTrack(
    val uri: String,
    val name: String,
    val artists: List<ArtistItem>
)

@Serializable
data class ArtistItem(
    val name: String,
    val id: String? = null
)
