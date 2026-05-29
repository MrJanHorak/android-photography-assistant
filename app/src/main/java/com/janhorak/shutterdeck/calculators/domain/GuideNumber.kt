package com.janhorak.shutterdeck.calculators.domain

import kotlin.math.sqrt

/**
 * Pure, Android-free flash guide-number math. Guide numbers are quoted at ISO 100 and
 * scale with the square root of the ISO ratio. GN = distance * f-number.
 */

/** Guide number adjusted from its ISO-100 rating to [iso]. */
fun guideNumberAtIso(guideNumberIso100: Double, iso: Int): Double? {
    if (!guideNumberIso100.isFinite() || guideNumberIso100 <= 0 || iso <= 0) return null
    return guideNumberIso100 * sqrt(iso / 100.0)
}

/** Aperture (f-number) for a flash of [guideNumberIso100] at [distanceMeters] and [iso]. */
fun apertureForFlash(guideNumberIso100: Double, distanceMeters: Double, iso: Int): Double? {
    if (!distanceMeters.isFinite() || distanceMeters <= 0) return null
    val gn = guideNumberAtIso(guideNumberIso100, iso) ?: return null
    return gn / distanceMeters
}

/** Flash-to-subject distance (meters) for a given [aperture], guide number and [iso]. */
fun distanceForFlash(guideNumberIso100: Double, aperture: Double, iso: Int): Double? {
    if (!aperture.isFinite() || aperture <= 0) return null
    val gn = guideNumberAtIso(guideNumberIso100, iso) ?: return null
    return gn / aperture
}
