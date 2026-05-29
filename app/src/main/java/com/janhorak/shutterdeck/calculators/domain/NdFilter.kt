package com.janhorak.shutterdeck.calculators.domain

import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow

/**
 * Pure, Android-free neutral-density filter math. ND strength is expressed in stops;
 * helpers convert from optical density and ND factor notations.
 */

/** Resulting shutter (seconds) after adding [ndStops] of ND to [baseShutterSeconds]. */
fun ndAdjustedShutterSeconds(baseShutterSeconds: Double, ndStops: Double): Double? {
    if (!baseShutterSeconds.isFinite() || !ndStops.isFinite() || baseShutterSeconds <= 0) return null
    return baseShutterSeconds * 2.0.pow(ndStops)
}

/** Stops of ND needed to go from [baseShutterSeconds] to [desiredShutterSeconds]. */
fun ndStopsForDesiredShutter(baseShutterSeconds: Double, desiredShutterSeconds: Double): Double? {
    if (!baseShutterSeconds.isFinite() || !desiredShutterSeconds.isFinite()) return null
    if (baseShutterSeconds <= 0 || desiredShutterSeconds <= 0) return null
    return log2(desiredShutterSeconds / baseShutterSeconds)
}

/** Stops from an optical density value (e.g. ND 0.9 -> 3 stops, ND 3.0 -> 10 stops). */
fun ndStopsFromOpticalDensity(opticalDensity: Double): Double? {
    if (!opticalDensity.isFinite() || opticalDensity < 0) return null
    return opticalDensity / 0.30102999566
}

/** Stops from an ND factor / multiplier (e.g. ND8 -> 3 stops, ND1000 -> ~10 stops). */
fun ndStopsFromFactor(ndFactor: Double): Double? {
    if (!ndFactor.isFinite() || ndFactor <= 0) return null
    return ln(ndFactor) / ln(2.0)
}
