package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EquivalentExposureTest {

    @Test
    fun aperture_twoStopsDownQuadruplesTime() {
        // f/2.8 -> f/5.6 (two stops) at 1/100 -> 1/25 s.
        assertEquals(0.04, equivalentShutterForAperture(0.01, 2.8, 5.6)!!, 1e-9)
    }

    @Test
    fun iso_higherIsoShortensTime() {
        // ISO 100 -> 400 (two stops) at 1/100 -> 1/400 s.
        assertEquals(0.0025, equivalentShutterForIso(0.01, 100, 400)!!, 1e-9)
    }

    @Test
    fun shutterStops_isLogBase2() {
        assertEquals(2.0, shutterStops(0.01, 0.04)!!, 1e-9)
    }

    @Test
    fun apertureForShutter_roundTrip() {
        assertEquals(5.6, equivalentApertureForShutter(2.8, 0.01, 0.04)!!, 1e-9)
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(equivalentShutterForAperture(0.0, 2.8, 5.6))
        assertNull(equivalentShutterForIso(0.01, 0, 400))
        assertNull(shutterStops(0.0, 0.04))
    }
}
