package com.andy.spotifysdktesting.core.userprefs.domain.extensions

import com.andy.spotifysdktesting.core.userprefs.domain.model.CustomRadio
import com.andy.spotifysdktesting.core.userprefs.domain.model.UserPrefs

fun UserPrefs.activeRadio(): CustomRadio? =
    radios.firstOrNull { it.id == activeRadioId }
        ?: radios.firstOrNull()

fun UserPrefs.addRadio(radio: CustomRadio): UserPrefs =
    copy(
        radios = radios + radio,
        activeRadioId = radio.id
    )

fun UserPrefs.updateRadio(updated: CustomRadio): UserPrefs =
    copy(
        radios = radios.map {
            if (it.id == updated.id) updated else it
        }
    )

fun UserPrefs.removeRadio(radioId: String): UserPrefs {
    val newRadios = radios.filterNot { it.id == radioId }

    val newActive = when {
        activeRadioId != radioId -> activeRadioId
        newRadios.isNotEmpty() -> newRadios.first().id
        else -> null
    }

    return copy(
        radios = newRadios,
        activeRadioId = newActive
    )
}

