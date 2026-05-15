package com.example.photography_helper.metering.presentation

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

data class ReflectiveMeterReading(
    val aperture: Float,
    val shutterSeconds: Double,
    val iso: Int,
    val ev100: Float,
)

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