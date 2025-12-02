package com.andy.spotifysdktesting.feature.spotifysdk.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class TrackRecommendation(
    val uri: String,
    val name: String,
    val artist: String
)

/** Modelo simple para deserializar la respuesta de Top Tracks o Recently Played */
@Serializable
data class PagingObject(
    val items: List<TrackItem>
)

/** Modelo para el item de track dentro de PagingObject */
@Serializable
data class TrackItem(
    val track: SimplifiedTrack? = null, // Para Recently Played
    val name: String? = null,           // Para Top Tracks (a veces es directo)
    val uri: String? = null,
    val artists: List<ArtistItem> = emptyList()
)

/** Modelo simplificado para Top Tracks y Artists */
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