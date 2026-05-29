package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusStackingTest {

    @Test
    fun plan_tilesRangeWithIncreasingFocusDistances() {
        val plan = calculateFocusStack(
            focalLengthMm = 100.0,
            aperture = 8.0,
            nearDistanceMeters = 1.0,
            farDistanceMeters = 3.0,
            circleOfConfusionMm = FULL_FRAME_COC_MM,
        )!!
        assertTrue(plan.frameCount >= 2)
        assertTrue(plan.frameCount == plan.focusDistancesMeters.size)
        // Focus distances strictly increase and start beyond the near point.
        val d = plan.focusDistancesMeters
        for (i in 1 until d.size) assertTrue(d[i] > d[i - 1])
        assertTrue(d.first() > 1.0)
    }

    @Test
    fun tighterApertureNeedsMoreFrames() {
        val wide = calculateFocusStack(100.0, 4.0, 1.0, 3.0, FULL_FRAME_COC_MM)!!
        val narrow = calculateFocusStack(100.0, 16.0, 1.0, 3.0, FULL_FRAME_COC_MM)!!
        assertTrue(wide.frameCount > narrow.frameCount)
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(calculateFocusStack(100.0, 8.0, 3.0, 1.0, FULL_FRAME_COC_MM))
        assertNull(calculateFocusStack(0.0, 8.0, 1.0, 3.0, FULL_FRAME_COC_MM))
        assertNull(calculateFocusStack(100.0, 8.0, 1.0, 3.0, FULL_FRAME_COC_MM, overlapFraction = 1.5))
        assertNull(calculateFocusStack(100.0, 8.0, Double.NaN, 3.0, FULL_FRAME_COC_MM))
    }
}
