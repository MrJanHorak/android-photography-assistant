package com.janhorak.shutterdeck.ui.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.janhorak.shutterdeck.core.location.CurrentCoordinates
import com.janhorak.shutterdeck.core.location.CurrentLocationLookupResult
import com.janhorak.shutterdeck.core.location.DeviceLocationProvider
import com.janhorak.shutterdeck.core.location.FailureReason
import kotlinx.coroutines.launch
import java.util.Locale

data class CurrentLocationRequestState(
    val isLoading: Boolean,
    val statusMessage: String?,
    val canOpenSettings: Boolean,
    val requestCurrentLocation: () -> Unit,
    val openSettings: () -> Unit,
)

fun formatCoordinateInput(value: Double): String = String.format(Locale.US, "%.6f", value)

@Composable
fun rememberCurrentLocationRequestState(
    onLocationResolved: (CurrentCoordinates) -> Unit,
): CurrentLocationRequestState {
    val context = LocalContext.current
    val currentOnLocationResolved = rememberUpdatedState(onLocationResolved)
    val provider = remember(context.applicationContext) {
        DeviceLocationProvider(context.applicationContext)
    }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var canOpenSettings by rememberSaveable { mutableStateOf(false) }
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }

    fun updateDeniedState() {
        val activity = context.findActivity()
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
        } ?: false
        canOpenSettings = hasRequestedPermission && !shouldShowRationale
        statusMessage = if (canOpenSettings) {
            "Location permission is off for ShutterDeck. Open Settings to enable current-location lookup."
        } else {
            "Location permission is required to use the phone's current location."
        }
    }

    fun fetchLocation() {
        if (isLoading) return
        coroutineScope.launch {
            isLoading = true
            canOpenSettings = false
            statusMessage = null
            when (val result = provider.getCurrentLocation()) {
                is CurrentLocationLookupResult.Success -> currentOnLocationResolved.value(result.coordinates)
                is CurrentLocationLookupResult.Failure -> {
                    statusMessage = when (result.reason) {
                        FailureReason.PERMISSION_REQUIRED ->
                            "Location permission is required to use the phone's current location."

                        FailureReason.LOCATION_UNAVAILABLE ->
                            "Current location unavailable. Turn on location services and try again."
                    }
                }
            }
            isLoading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        hasRequestedPermission = true
        val granted = results.values.any { it } || context.hasLocationPermission()
        if (granted) {
            fetchLocation()
        } else {
            updateDeniedState()
        }
    }

    return CurrentLocationRequestState(
        isLoading = isLoading,
        statusMessage = statusMessage,
        canOpenSettings = canOpenSettings,
        requestCurrentLocation = {
            statusMessage = null
            canOpenSettings = false
            if (context.hasLocationPermission()) {
                fetchLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        },
        openSettings = { context.openAppSettings() },
    )
}

@Composable
fun CurrentLocationAction(
    state: CurrentLocationRequestState,
    modifier: Modifier = Modifier,
    buttonLabel: String = "Use current location",
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedButton(
            onClick = state.requestCurrentLocation,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isLoading) "Getting current location..." else buttonLabel)
        }
        state.statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.canOpenSettings) {
            TextButton(onClick = state.openSettings) {
                Text("Open Settings")
            }
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).apply {
        if (this@openAppSettings !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    startActivity(intent)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
