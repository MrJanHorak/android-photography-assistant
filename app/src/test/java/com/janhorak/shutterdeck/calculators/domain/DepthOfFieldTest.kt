package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthOfFieldTest {

    @Test
    fun circleOfConfusion_scalesWithCrop() {
        assertEquals(0.030, circleOfConfusionMm(1.0), 1e-9)
        assertEquals(0.020, circleOfConfusionMm(1.5), 1e-9)
    }

    @Test
    fun depthOfField_matchesReferenceValues() {
        // 50mm, f/8, focus 5m, full-frame CoC 0.030mm.
        val result = calculateDepthOfField(50.0, 8.0, 5.0, FULL_FRAME_COC_MM)!!
        assertEquals(10.47, result.hyperfocalMeters, 0.05)
        assertEquals(3.39, result.nearMeters, 0.05)
        assertEquals(9.53, result.farMeters, 0.05)
        assertEquals(result.farMeters - result.nearMeters, result.totalMeters, 1e-6)
    }

    @Test
    fun depthOfField_farIsInfiniteBeyondHyperfocal() {
        // Hyperfocal is ~10.5m; focusing at 15m yields an infinite far limit.
        val result = calculateDepthOfField(50.0, 8.0, 15.0, FULL_FRAME_COC_MM)!!
        assertTrue(result.farMeters.isInfinite())
        assertTrue(result.totalMeters.isInfinite())
        assertTrue(result.behindMeters.isInfinite())
    }

    @Test
    fun depthOfField_invalidInputsReturnNull() {
        assertNull(calculateDepthOfField(0.0, 8.0, 5.0, FULL_FRAME_COC_MM))
        assertNull(calculateDepthOfField(50.0, 0.0, 5.0, FULL_FRAME_COC_MM))
        assertNull(calculateDepthOfField(50.0, 8.0, 0.0, FULL_FRAME_COC_MM))
        // Non-finite and sub-focal-length focus distances are rejected.
        assertNull(calculateDepthOfField(50.0, 8.0, Double.NaN, FULL_FRAME_COC_MM))
        assertNull(calculateDepthOfField(50.0, 8.0, 0.01, FULL_FRAME_COC_MM))
    }

    @Test
    fun hyperfocal_matchesDofResult() {
        val h = hyperfocalDistanceMeters(50.0, 8.0, FULL_FRAME_COC_MM)!!
        assertEquals(10.47, h, 0.05)
    }
}
