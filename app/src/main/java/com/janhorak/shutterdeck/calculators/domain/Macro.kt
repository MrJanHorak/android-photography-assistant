package com.janhorak.shutterdeck.calculators.domain

import kotlin.math.log2

/**
 * Pure, Android-free macro / extension-tube math. With the lens focused at infinity,
 * adding extension E gives magnification m = E / f. Effective aperture and the required
 * exposure compensation grow with magnification.
 */

data class MacroResult(
    val magnification: Double,
    val effectiveAperture: Double,
    val exposureCompensationStops: Double,
)

/** Magnification from extension (mm) added to a lens of focal length (mm). */
fun magnificationFromExtension(focalLengthMm: Double, extensionMm: Double): Double? {
    if (!focalLengthMm.isFinite() || !extensionMm.isFinite()) return null
    if (focalLengthMm <= 0 || extensionMm < 0) return null
    return extensionMm / focalLengthMm
}

/** Effective aperture at magnification [m] for a marked aperture [aperture]. */
fun effectiveAperture(aperture: Double, magnification: Double): Double? {
    if (!aperture.isFinite() || !magnification.isFinite()) return null
    if (aperture <= 0 || magnification < 0) return null
    return aperture * (1.0 + magnification)
}

/** Exposure compensation (stops) required at magnification [m]: 2 * log2(1 + m). */
fun macroExposureCompensationStops(magnification: Double): Double? {
    if (!magnification.isFinite() || magnification < 0) return null
    return 2.0 * log2(1.0 + magnification)
}

/** Combined macro result from focal length, marked aperture and extension. */
fun calculateMacro(focalLengthMm: Double, aperture: Double, extensionMm: Double): MacroResult? {
    val m = magnificationFromExtension(focalLengthMm, extensionMm) ?: return null
    val effective = effectiveAperture(aperture, m) ?: return null
    val comp = macroExposureCompensationStops(m) ?: return null
    return MacroResult(magnification = m, effectiveAperture = effective, exposureCompensationStops = comp)
}
