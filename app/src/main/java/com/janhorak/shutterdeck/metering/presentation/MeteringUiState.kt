package com.janhorak.shutterdeck.metering.presentation

import com.janhorak.shutterdeck.metering.domain.ReflectiveMeterReading

enum class MeteringSource(
    val label: String,
    val description: String,
) {
    AMBIENT_SENSOR(
        label = "Ambient sensor",
        description = "Incident-style reading from the device light sensor.",
    ),
    CAMERA_REFLECTIVE(
        label = "Camera reflective",
        description = "Reflective reading from the back camera auto exposure.",
    ),
}

data class MeteringUiState(
    val selectedSource: MeteringSource = MeteringSource.AMBIENT_SENSOR,
    val sensorAvailable: Boolean = false,
    val cameraAvailable: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    val isMetering: Boolean = false,
    val ev: Float? = null,
    val lux: Float? = null,
    val calibrationOffsetEv: Float = 0f,
    val cameraReading: ReflectiveMeterReading? = null,
)