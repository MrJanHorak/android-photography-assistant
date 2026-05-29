package com.janhorak.shutterdeck.calculators.domain

/**
 * Pure, Android-free depth-of-field math. All distance inputs/outputs are in meters;
 * focal length and circle of confusion are in millimeters. Far distance and the
 * derived totals may be [Double.POSITIVE_INFINITY] when focus is at or beyond the
 * hyperfocal distance.
 */

/** Circle of confusion (mm) for a full-frame 35mm sensor (common 0.030mm convention). */
const val FULL_FRAME_COC_MM: Double = 0.030

data class DepthOfFieldResult(
    val hyperfocalMeters: Double,
    val nearMeters: Double,
    val farMeters: Double,
    val totalMeters: Double,
    val inFrontMeters: Double,
    val behindMeters: Double,
)

/** Circle of confusion (mm) scaled from full frame by [cropFactor] (e.g. 1.5 for APS-C). */
fun circleOfConfusionMm(
    cropFactor: Double,
    fullFrameCoCMm: Double = FULL_FRAME_COC_MM,
): Double = fullFrameCoCMm / cropFactor

/** Hyperfocal distance in meters from focal length (mm), aperture and CoC (mm). */
fun hyperfocalDistanceMeters(
    focalLengthMm: Double,
    aperture: Double,
    circleOfConfusionMm: Double,
): Double? {
    if (!focalLengthMm.isFinite() || !aperture.isFinite() || !circleOfConfusionMm.isFinite()) return null
    if (focalLengthMm <= 0 || aperture <= 0 || circleOfConfusionMm <= 0) return null
    val hMm = (focalLengthMm * focalLengthMm) / (aperture * circleOfConfusionMm) + focalLengthMm
    return hMm / 1000.0
}

/**
 * Depth of field for a lens focused at [focusDistanceMeters].
 * Returns null when any input is non-positive.
 */
fun calculateDepthOfField(
    focalLengthMm: Double,
    aperture: Double,
    focusDistanceMeters: Double,
    circleOfConfusionMm: Double,
): DepthOfFieldResult? {
    if (!focalLengthMm.isFinite() || !aperture.isFinite() ||
        !focusDistanceMeters.isFinite() || !circleOfConfusionMm.isFinite()
    ) {
        return null
    }
    if (focalLengthMm <= 0 || aperture <= 0 || focusDistanceMeters <= 0 || circleOfConfusionMm <= 0) {
        return null
    }
    val f = focalLengthMm
    val s = focusDistanceMeters * 1000.0
    // Thin-lens DoF is only meaningful when focused beyond the focal length.
    if (s <= f) return null
    val hMm = (f * f) / (aperture * circleOfConfusionMm) + f

    val nearMm = (s * (hMm - f)) / (hMm + s - 2 * f)
    val farDenominator = hMm - s
    val farMm = if (farDenominator <= 0.0) Double.POSITIVE_INFINITY else (s * (hMm - f)) / farDenominator

    val nearMeters = nearMm / 1000.0
    val farMeters = if (farMm.isInfinite()) Double.POSITIVE_INFINITY else farMm / 1000.0
    val totalMeters = if (farMeters.isInfinite()) Double.POSITIVE_INFINITY else farMeters - nearMeters
    val inFrontMeters = focusDistanceMeters - nearMeters
    val behindMeters = if (farMeters.isInfinite()) Double.POSITIVE_INFINITY else farMeters - focusDistanceMeters

    return DepthOfFieldResult(
        hyperfocalMeters = hMm / 1000.0,
        nearMeters = nearMeters,
        farMeters = farMeters,
        totalMeters = totalMeters,
        inFrontMeters = inFrontMeters,
        behindMeters = behindMeters,
    )
}
