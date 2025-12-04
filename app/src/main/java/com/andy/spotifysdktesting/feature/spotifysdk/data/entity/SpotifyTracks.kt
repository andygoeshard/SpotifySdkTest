package com.andy.spotifysdktesting.feature.spotifysdk.data.entity

import kotlinx.serialization.SerialName
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
    val items: List<TrackItem> = emptyList(),
    val next: String? = null,
    val total: Int? = null
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

data class DetailedTrackInfo(
    val trackId: String,
    val artistId: String,
    val artistName: String,
    val durationMs: Long,
    val previewUrl: String? = null,
    val genres: List<String> = emptyList()
)
@Serializable
data class FullTrackResponse(
    // ¡HACER TODOS LOS CAMPOS SUSCEPTIBLES A FALTAR COMO OPCIONALES!

    // Campos del Track
    val id: String? = null, // CORREGIDO: Debe ser opcional
    val name: String? = null, // CORREGIDO: Debe ser opcional

    // Duración en milisegundos
    @SerialName("duration_ms")
    val durationMs: Long? = null, // CORREGIDO: Debe ser opcional

    // URL de previsualización
    @SerialName("preview_url")
    val previewUrl: String? = null, // CORREGIDO: Debe ser opcional

    // Lista de artistas
    val artists: List<ArtistItem> = emptyList(),

    // Si tienes AlbumItem, también debe ser opcional:
    @SerialName("album")
    val album: AlbumItem? = null
)
@Serializable
data class AlbumItem(
    val id: String? = null,
    val images: List<ImageItem> = emptyList()
)

@Serializable
data class ImageItem(
    val url: String?,
    val height: Int?,
    val width: Int?
)
@Serializable
data class RecommendationsResponse(
    val seeds: List<RecommendationSeedObject> = emptyList(),
    val tracks: List<TrackObject> = emptyList()
)


@Serializable
data class RecommendationSeedObject(
    @SerialName("afterFilteringSize") val afterFilteringSize: Int? = null,
    @SerialName("afterRelinkingSize") val afterRelinkingSize: Int? = null,
    val href: String? = null,
    val id: String? = null,
    @SerialName("initialPoolSize") val initialPoolSize: Int? = null,
    val type: String? = null
)

@Serializable
data class TrackObject(
    val album: AlbumObject? = null,
    val artists: List<SimplifiedArtistObject> = emptyList(),
    @SerialName("available_markets") val availableMarkets: List<String> = emptyList(),
    @SerialName("disc_number") val discNumber: Int? = null,
    @SerialName("duration_ms") val durationMs: Int? = null,
    val explicit: Boolean? = null,
    @SerialName("external_ids") val externalIds: Map<String, String>? = null,
    @SerialName("external_urls") val externalUrls: Map<String, String>? = null,
    val href: String? = null,
    val id: String? = null,
    @SerialName("is_playable") val isPlayable: Boolean? = null,
    @SerialName("linked_from") val linkedFrom: Map<String, String>? = null,
    val restrictions: Map<String, String>? = null,
    val name: String? = null,
    val popularity: Int? = null,
    @SerialName("preview_url") val previewUrl: String? = null,
    @SerialName("track_number") val trackNumber: Int? = null,
    val type: String? = null,
    val uri: String? = null,
    @SerialName("is_local") val isLocal: Boolean? = null
)


@Serializable
data class AlbumObject(
    @SerialName("album_type") val albumType: String? = null,
    @SerialName("total_tracks") val totalTracks: Int? = null,
    @SerialName("available_markets") val availableMarkets: List<String> = emptyList(),
    @SerialName("external_urls") val externalUrls: Map<String, String>? = null,
    val href: String? = null,
    val id: String? = null,
    val images: List<ImageItem> = emptyList(),
    val name: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("release_date_precision") val releaseDatePrecision: String? = null,
    val restrictions: Map<String, String>? = null,
    val type: String? = null,
    val uri: String? = null,
    val artists: List<SimplifiedArtistObject> = emptyList()
)

@Serializable
data class SimplifiedArtistObject(
    @SerialName("external_urls") val externalUrls: Map<String, String>? = null,
    val href: String? = null,
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
    val uri: String? = null
)
