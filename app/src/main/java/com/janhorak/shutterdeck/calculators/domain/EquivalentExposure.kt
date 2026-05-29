package com.janhorak.shutterdeck.calculators.domain

import kotlin.math.ln

/**
 * Pure, Android-free equivalent-exposure math: keep total exposure constant while
 * trading aperture, shutter and ISO. f-stops follow N^2; ISO and shutter are linear.
 */

/** Stop difference between two shutter times (positive = [target] is brighter/longer). */
fun shutterStops(baseSeconds: Double, targetSeconds: Double): Double? {
    if (baseSeconds <= 0 || targetSeconds <= 0) return null
    return ln(targetSeconds / baseSeconds) / ln(2.0)
}

/** Shutter (seconds) that keeps exposure constant when aperture changes, ISO fixed. */
fun equivalentShutterForAperture(
    baseShutterSeconds: Double,
    baseAperture: Double,
    targetAperture: Double,
): Double? {
    if (baseShutterSeconds <= 0 || baseAperture <= 0 || targetAperture <= 0) return null
    if (!baseShutterSeconds.isFinite() || !baseAperture.isFinite() || !targetAperture.isFinite()) return null
    return baseShutterSeconds * (targetAperture * targetAperture) / (baseAperture * baseAperture)
}

/** Shutter (seconds) that keeps exposure constant when ISO changes, aperture fixed. */
fun equivalentShutterForIso(
    baseShutterSeconds: Double,
    baseIso: Int,
    targetIso: Int,
): Double? {
    if (baseShutterSeconds <= 0 || baseIso <= 0 || targetIso <= 0) return null
    return baseShutterSeconds * baseIso.toDouble() / targetIso.toDouble()
}

/** Aperture that keeps exposure constant when shutter changes, ISO fixed. */
fun equivalentApertureForShutter(
    baseAperture: Double,
    baseShutterSeconds: Double,
    targetShutterSeconds: Double,
): Double? {
    if (baseAperture <= 0 || baseShutterSeconds <= 0 || targetShutterSeconds <= 0) return null
    return baseAperture * kotlin.math.sqrt(targetShutterSeconds / baseShutterSeconds)
}
