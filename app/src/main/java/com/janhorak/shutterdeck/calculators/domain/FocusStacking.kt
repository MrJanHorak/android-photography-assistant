package com.janhorak.shutterdeck.calculators.domain

/**
 * Pure, Android-free focus-stacking planner. Builds a sequence of focus distances that
 * tiles the depth range [nearDistanceMeters, farDistanceMeters] with a configurable
 * overlap between adjacent depth-of-field slices. All internal math is in millimeters.
 */

data class FocusStackPlan(
    val frameCount: Int,
    val focusDistancesMeters: List<Double>,
)

private const val MAX_FOCUS_FRAMES = 1000

/**
 * Plan a focus stack. Returns null on invalid input. [overlapFraction] (0..0.9) is the
 * fraction of each slice that overlaps the next frame for safety.
 */
fun calculateFocusStack(
    focalLengthMm: Double,
    aperture: Double,
    nearDistanceMeters: Double,
    farDistanceMeters: Double,
    circleOfConfusionMm: Double,
    overlapFraction: Double = 0.2,
): FocusStackPlan? {
    if (!focalLengthMm.isFinite() || !aperture.isFinite() ||
        !nearDistanceMeters.isFinite() || !farDistanceMeters.isFinite() ||
        !circleOfConfusionMm.isFinite() || !overlapFraction.isFinite()
    ) {
        return null
    }
    if (focalLengthMm <= 0 || aperture <= 0 || circleOfConfusionMm <= 0) return null
    if (nearDistanceMeters <= 0 || farDistanceMeters <= nearDistanceMeters) return null
    if (overlapFraction < 0.0 || overlapFraction > 0.9) return null

    val f = focalLengthMm
    val nearMm = nearDistanceMeters * 1000.0
    val farMm = farDistanceMeters * 1000.0
    if (nearMm <= f) return null

    val hMm = (f * f) / (aperture * circleOfConfusionMm) + f

    fun farLimitOf(focusMm: Double): Double {
        val denom = hMm - focusMm
        return if (denom <= 0.0) Double.POSITIVE_INFINITY else (focusMm * (hMm - f)) / denom
    }

    val focusDistances = mutableListOf<Double>()
    var coverFrom = nearMm
    var guard = 0
    while (coverFrom < farMm && guard < MAX_FOCUS_FRAMES) {
        guard++
        // Focus distance whose near depth-of-field limit equals coverFrom.
        val denom = hMm - f - coverFrom
        val focusMm = if (denom <= 0.0) Double.POSITIVE_INFINITY else coverFrom * (hMm - 2 * f) / denom
        if (focusMm.isInfinite() || focusMm >= hMm) {
            // Focusing at the hyperfocal distance renders coverFrom..infinity acceptably sharp.
            focusDistances.add(hMm / 1000.0)
            break
        }
        focusDistances.add(focusMm / 1000.0)
        val far = farLimitOf(focusMm)
        if (far.isInfinite() || far >= farMm) break
        val sliceDepth = far - coverFrom
        coverFrom = far - overlapFraction * sliceDepth
    }

    if (focusDistances.isEmpty()) return null
    return FocusStackPlan(frameCount = focusDistances.size, focusDistancesMeters = focusDistances)
}
