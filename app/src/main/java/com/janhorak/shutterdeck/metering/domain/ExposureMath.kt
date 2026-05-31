package com.janhorak.shutterdeck.metering.domain

import kotlin.math.log2
import kotlin.math.pow

/**
 * Pure, Android-free exposure math. Kept free of platform imports so it can be
 * unit-tested on the JVM and later shared with iOS via Kotlin Multiplatform.
 */

/** EV at ISO 100 from an ambient lux reading: EV = log2(lux / 2.5). */
fun evFromLux(lux: Float): Float = log2(lux / EV100_ZERO_LUX.toFloat())

/** EV100 for a reflective reading from aperture, shutter (seconds) and ISO, or null if inputs are invalid. */
fun reflectiveEv100(aperture: Float, shutterSeconds: Double, iso: Int): Float? {
    if (aperture <= 0f || shutterSeconds <= 0.0 || iso <= 0) return null
    val nSquared = aperture.toDouble().pow(2.0)
    return log2((nSquared / shutterSeconds) * (100.0 / iso.toDouble())).toFloat()
}

/**
 * Required shutter speed (seconds) for a given aperture and ISO at EV100 [ev].
 *
 * From the exposure equation at ISO 100, 2^EV100 = (N^2 / t) * (100/ISO), so
 *   t = (N^2 * (100/ISO)) / 2^EV100.
 * Raising ISO therefore *shortens* the required shutter, matching [reflectiveEv100].
 */
fun requiredShutterSeconds(aperture: Float, iso: Int, ev: Float?): Double? {
    val exposureValue = ev ?: return null
    if (iso <= 0) return null
    val nSquared = aperture.toDouble().pow(2.0)
    val isoFactor = 100.0 / iso
    val twoPowEv = 2.0.pow(exposureValue.toDouble())
    return if (twoPowEv > 0.0) (nSquared * isoFactor) / twoPowEv else null
}

/** Stops of light gained when moving from [currentAperture] to a wider [targetAperture]. */
fun apertureGainStops(currentAperture: Float, targetAperture: Float): Double =
    log2((currentAperture.toDouble() * currentAperture) / (targetAperture.toDouble() * targetAperture))

/**
 * Handheld minimum shutter (seconds) from focal length, crop factor, and stabilization stops.
 *
 * Starts from the reciprocal rule (1 / effective focal length). Each stop of
 * stabilization lets you hold the shutter open one stop *longer*, so the limit is
 * multiplied by 2^stabilizationStops.
 */
fun calculateHandheldMinimumShutterSeconds(
    focalLengthMm: Int,
    cropFactor: Float,
    stabilizationStops: Float,
): Double {
    val effectiveFocalLength = (focalLengthMm.toFloat() * cropFactor).coerceAtLeast(1.0f)
    val baseRule = 1.0 / effectiveFocalLength
    val stabilizationBenefit = 2.0.pow(stabilizationStops.toDouble())
    return baseRule * stabilizationBenefit
}
