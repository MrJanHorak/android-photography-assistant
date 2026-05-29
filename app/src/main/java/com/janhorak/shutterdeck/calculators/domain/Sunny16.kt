package com.janhorak.shutterdeck.calculators.domain

import kotlin.math.ln
import kotlin.math.pow

/**
 * Pure, Android-free "Sunny 16" exposure reference and long-exposure reciprocity
 * correction for film.
 */

/** A lighting condition expressed as stops darker than full sun (Sunny 16 baseline). */
enum class LightingCondition(val label: String, val stopsDarkerThanSunny: Double) {
    SNOW_OR_SAND("Snow / bright sand", -1.0),
    SUNNY("Bright sun", 0.0),
    SLIGHT_OVERCAST("Slight overcast", 1.0),
    OVERCAST("Overcast", 2.0),
    HEAVY_OVERCAST("Heavy overcast", 3.0),
    OPEN_SHADE("Open shade / sunset", 4.0),
}

/**
 * Recommended shutter (seconds) for the Sunny 16 rule: at f/16 in bright sun the
 * shutter is 1/ISO. Opening up or darker light lengthens the time.
 * t = (1/ISO) * (N^2 / 256) * 2^stopsDarker
 */
fun sunny16ShutterSeconds(iso: Int, aperture: Double, stopsDarkerThanSunny: Double): Double? {
    if (iso <= 0 || aperture <= 0 || !aperture.isFinite() || !stopsDarkerThanSunny.isFinite()) return null
    val base = 1.0 / iso
    return base * (aperture * aperture / 256.0) * 2.0.pow(stopsDarkerThanSunny)
}

/**
 * Reciprocity-corrected exposure (seconds) for film using the common power law
 * t_corrected = t_metered ^ exponent, applied only when t_metered > 1s.
 * exponent 1.0 means no correction (typical digital).
 */
fun reciprocityCorrectedSeconds(meteredSeconds: Double, exponent: Double): Double? {
    if (!meteredSeconds.isFinite() || !exponent.isFinite()) return null
    if (meteredSeconds <= 0 || exponent <= 0) return null
    return if (meteredSeconds <= 1.0) meteredSeconds else meteredSeconds.pow(exponent)
}

/** Additional stops of compensation implied by a reciprocity correction. */
fun reciprocityCompensationStops(meteredSeconds: Double, correctedSeconds: Double): Double? {
    if (meteredSeconds <= 0 || correctedSeconds <= 0) return null
    return ln(correctedSeconds / meteredSeconds) / ln(2.0)
}
