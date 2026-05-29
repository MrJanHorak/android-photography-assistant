package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuideNumberTest {

    @Test
    fun apertureFromGuideNumber() {
        // GN 56 (m, ISO 100) at 5m -> f/11.2.
        assertEquals(11.2, apertureForFlash(56.0, 5.0, 100)!!, 1e-9)
    }

    @Test
    fun isoScalesGuideNumberBySqrt() {
        // ISO 400 doubles the guide number (two stops -> 2x).
        assertEquals(112.0, guideNumberAtIso(56.0, 400)!!, 1e-9)
    }

    @Test
    fun distanceFromGuideNumber() {
        assertEquals(5.0, distanceForFlash(56.0, 11.2, 100)!!, 1e-9)
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(apertureForFlash(56.0, 0.0, 100))
        assertNull(distanceForFlash(56.0, 0.0, 100))
        assertNull(guideNumberAtIso(0.0, 100))
    }
}
