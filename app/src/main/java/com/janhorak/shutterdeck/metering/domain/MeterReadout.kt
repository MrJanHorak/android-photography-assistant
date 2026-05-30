package com.janhorak.shutterdeck.metering.domain

import kotlin.math.abs
import kotlin.math.log2

fun formatSuggestedShutter(
    shutterSpeedSeconds: Double?,
    shutterOptions: List<ShutterOption>,
    supportsBulb: Boolean,
): String {
    val seconds = shutterSpeedSeconds ?: return "Awaiting sensor data"
    if (seconds <= 0.0) return "Awaiting sensor data"

    require(shutterOptions.isNotEmpty()) { "Shutter options must not be empty." }
    val longestStandard = shutterOptions.maxOf { it.seconds }

    if (seconds > longestStandard) {
        return if (supportsBulb) {
            "Bulb ~ ${formatDuration(seconds)}"
        } else {
            "Longer than ${shutterOptions.first().label}"
        }
    }

    val suggestion = shutterOptions.minByOrNull { option ->
        abs(log2(option.seconds / seconds))
    } ?: return "Awaiting sensor data"

    return suggestion.label
}

fun formatCameraExposureSummary(reading: ReflectiveMeterReading): String {
    return "f/${formatDecimal(reading.aperture.toDouble())}  ${formatExposureTime(reading.shutterSeconds)}  ISO ${reading.iso}"
}
