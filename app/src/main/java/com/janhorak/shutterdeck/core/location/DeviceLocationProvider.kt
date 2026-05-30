package com.janhorak.shutterdeck.core.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class CurrentCoordinates(
    val latitude: Double,
    val longitude: Double,
)

sealed interface CurrentLocationLookupResult {
    data class Success(val coordinates: CurrentCoordinates) : CurrentLocationLookupResult
    data class Failure(val reason: FailureReason) : CurrentLocationLookupResult
}

enum class FailureReason {
    PERMISSION_REQUIRED,
    LOCATION_UNAVAILABLE,
}

class DeviceLocationProvider(context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): CurrentLocationLookupResult =
        suspendCancellableCoroutine { continuation ->
            val tokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                tokenSource.token,
            )
                .addOnSuccessListener { location ->
                    if (!continuation.isActive) return@addOnSuccessListener
                    continuation.resume(
                        if (location != null) {
                            CurrentLocationLookupResult.Success(
                                CurrentCoordinates(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                ),
                            )
                        } else {
                            CurrentLocationLookupResult.Failure(FailureReason.LOCATION_UNAVAILABLE)
                        },
                    )
                }
                .addOnFailureListener { error ->
                    if (!continuation.isActive) return@addOnFailureListener
                    val reason = if (error is SecurityException) {
                        FailureReason.PERMISSION_REQUIRED
                    } else {
                        FailureReason.LOCATION_UNAVAILABLE
                    }
                    continuation.resume(CurrentLocationLookupResult.Failure(reason))
                }
                .addOnCanceledListener {
                    if (continuation.isActive) {
                        continuation.cancel()
                    }
                }
            continuation.invokeOnCancellation { tokenSource.cancel() }
        }
}
