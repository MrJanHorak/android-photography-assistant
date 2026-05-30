package com.janhorak.shutterdeck.utilities.domain

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

const val SPIRIT_LEVEL_THRESHOLD_DEGREES = 1.0
const val SPIRIT_LEVEL_DISPLAY_RANGE_DEGREES = 10.0
private const val SPIRIT_LEVEL_STEEP_GUARD_DEGREES = 75.0

data class SpiritLevelReading(
    val pitchDegrees: Double,
    val rollDegrees: Double,
    val pitchOffset: Float,
    val rollOffset: Float,
    val isLevel: Boolean,
    val tooSteep: Boolean,
)

fun calculateSpiritLevelReading(
    x: Float,
    y: Float,
    z: Float,
    levelThresholdDegrees: Double = SPIRIT_LEVEL_THRESHOLD_DEGREES,
    displayRangeDegrees: Double = SPIRIT_LEVEL_DISPLAY_RANGE_DEGREES,
): SpiritLevelReading? {
    val magnitude = sqrt(x.toDouble() * x + y.toDouble() * y + z.toDouble() * z)
    if (!magnitude.isFinite() || magnitude < 0.1) return null

    val normalizedX = x / magnitude
    val normalizedY = y / magnitude
    val normalizedZ = z / magnitude

    val pitchDegrees = Math.toDegrees(atan2(normalizedY, normalizedZ))
    val rollDegrees = Math.toDegrees(atan2(normalizedX, normalizedZ))
    val tooSteep = abs(pitchDegrees) > SPIRIT_LEVEL_STEEP_GUARD_DEGREES ||
        abs(rollDegrees) > SPIRIT_LEVEL_STEEP_GUARD_DEGREES

    val pitchOffset = if (tooSteep) {
        0f
    } else {
        (pitchDegrees / displayRangeDegrees).coerceIn(-1.0, 1.0).toFloat()
    }
    val rollOffset = if (tooSteep) {
        0f
    } else {
        (rollDegrees / displayRangeDegrees).coerceIn(-1.0, 1.0).toFloat()
    }

    return SpiritLevelReading(
        pitchDegrees = pitchDegrees,
        rollDegrees = rollDegrees,
        pitchOffset = pitchOffset,
        rollOffset = rollOffset,
        isLevel = !tooSteep &&
            abs(pitchDegrees) <= levelThresholdDegrees &&
            abs(rollDegrees) <= levelThresholdDegrees,
        tooSteep = tooSteep,
    )
}
