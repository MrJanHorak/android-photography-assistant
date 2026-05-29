package com.janhorak.shutterdeck.calculators.domain

/**
 * Pure, Android-free maximum-shutter math for astrophotography: the simple "rule of
 * 500" family plus the more accurate NPF rule.
 */

/**
 * Max shutter (seconds) before visible star trailing using the rule-of-N approximation:
 * t = ruleConstant / (focalLength * cropFactor). Common constants are 500, 400 and 300.
 */
fun ruleBasedMaxShutterSeconds(
    focalLengthMm: Double,
    cropFactor: Double,
    ruleConstant: Double = 500.0,
): Double? {
    if (!focalLengthMm.isFinite() || !cropFactor.isFinite() || !ruleConstant.isFinite()) return null
    val effectiveFocal = focalLengthMm * cropFactor
    if (effectiveFocal <= 0 || ruleConstant <= 0) return null
    return ruleConstant / effectiveFocal
}

/**
 * NPF rule max shutter (seconds): t = (35*N + 30*pixelPitch) / focalLength,
 * where N is the aperture, pixelPitch is in microns and focalLength in mm.
 */
fun npfMaxShutterSeconds(
    aperture: Double,
    pixelPitchMicrons: Double,
    focalLengthMm: Double,
): Double? {
    if (!aperture.isFinite() || !pixelPitchMicrons.isFinite() || !focalLengthMm.isFinite()) return null
    if (aperture <= 0 || pixelPitchMicrons <= 0 || focalLengthMm <= 0) return null
    return (35.0 * aperture + 30.0 * pixelPitchMicrons) / focalLengthMm
}

/** Pixel pitch (microns) from sensor width (mm) and horizontal resolution (pixels). */
fun pixelPitchMicrons(sensorWidthMm: Double, horizontalResolutionPx: Int): Double? {
    if (!sensorWidthMm.isFinite() || sensorWidthMm <= 0 || horizontalResolutionPx <= 0) return null
    return sensorWidthMm * 1000.0 / horizontalResolutionPx
}
