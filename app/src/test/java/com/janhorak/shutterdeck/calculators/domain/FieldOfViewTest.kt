package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FieldOfViewTest {

    @Test
    fun fiftyMmFullFrame_matchesKnownAngles() {
        val result = calculateFieldOfView(50.0, 1.0)!!
        // Horizontal AoV for 36mm sensor at 50mm ~= 39.6 degrees.
        assertEquals(39.6, result.horizontalDeg, 0.1)
        // Vertical AoV for 24mm sensor ~= 27.0 degrees.
        assertEquals(27.0, result.verticalDeg, 0.1)
        assertEquals(50.0, result.equivalentFocalLengthMm, 1e-9)
    }

    @Test
    fun cropFactor_scalesEquivalentFocalAndShrinksSensor() {
        val result = calculateFieldOfView(50.0, 1.5)!!
        assertEquals(75.0, result.equivalentFocalLengthMm, 1e-9)
        assertEquals(24.0, result.sensorWidthMm, 1e-9)
        assertEquals(16.0, result.sensorHeightMm, 1e-9)
        // Narrower than full frame at the same focal length.
        val fullFrame = calculateFieldOfView(50.0, 1.0)!!
        assert(result.horizontalDeg < fullFrame.horizontalDeg)
    }

    @Test
    fun angleOfView_invalidInputsReturnNull() {
        assertNull(angleOfViewDegrees(0.0, 36.0))
        assertNull(angleOfViewDegrees(50.0, 0.0))
        assertNull(calculateFieldOfView(0.0, 1.0))
        assertNull(calculateFieldOfView(50.0, 0.0))
    }
}
