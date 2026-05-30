package com.janhorak.shutterdeck.metering.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.janhorak.shutterdeck.ui.camera.BackCameraPreview

@Composable
internal fun CameraMeterPreview(
    modifier: Modifier = Modifier,
    onExposureSample: (aperture: Float, shutterSeconds: Double, iso: Int) -> Unit,
    onCameraMeteringStopped: () -> Unit,
) {
    DisposableEffect(onCameraMeteringStopped) {
        onDispose {
            onCameraMeteringStopped()
        }
    }

    BackCameraPreview(
        modifier = modifier,
        onExposureSample = onExposureSample,
        onBindingError = onCameraMeteringStopped,
    )
}