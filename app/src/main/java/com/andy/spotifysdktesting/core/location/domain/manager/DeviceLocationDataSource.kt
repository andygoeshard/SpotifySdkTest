package com.andy.spotifysdktesting.core.location.domain.manager

import android.annotation.SuppressLint
import android.content.Context
import com.andy.spotifysdktesting.core.location.domain.model.LocationCoordinates
import com.google.android.gms.location.LocationServices
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DeviceLocationDataSource(
    private val context: Context
) {

    private val client =   LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getLocation(): LocationCoordinates? =
        suspendCoroutine { cont ->
            client.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) cont.resume(LocationCoordinates(loc.latitude, loc.longitude))
                    else cont.resume(null)
                }
                .addOnFailureListener { cont.resume(null) }
        }
}
