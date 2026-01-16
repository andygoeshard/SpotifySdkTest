package com.andy.spotifysdktesting.core.userprefs.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomRadio(
    val id: String,
    val name: String,
    val description: String = "",

    val location: LocationPrefs = LocationPrefs(),
    val recentLocations: List<LocationPrefs> = emptyList(),

    val weather: WeatherUserPrefs = WeatherUserPrefs(),
    val news: NewsUserPrefs = NewsUserPrefs(),
    val funny: FunUserPrefs = FunUserPrefs(),

    val djFeatures: DjFeaturesPrefs = DjFeaturesPrefs(),
    val djPresets: List<DjFeaturesPrefs> = emptyList()
)