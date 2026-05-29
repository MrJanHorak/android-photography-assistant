package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NdFilterTest {

    @Test
    fun tenStopsOnOneSixtieth_givesAboutSeventeenSeconds() {
        // 1/60s with a 10-stop ND -> 1/60 * 1024 = 17.07s.
        assertEquals(17.07, ndAdjustedShutterSeconds(1.0 / 60.0, 10.0)!!, 0.01)
    }

    @Test
    fun reverseMode_findsStops() {
        // From 1/60 to ~17.07s is 10 stops.
        assertEquals(10.0, ndStopsForDesiredShutter(1.0 / 60.0, 1024.0 / 60.0)!!, 1e-6)
    }

    @Test
    fun opticalDensity_convertsToStops() {
        // ND 0.9 is nominally 3 stops (2.99) and ND 3.0 nominally 10 stops (9.97).
        assertEquals(3.0, ndStopsFromOpticalDensity(0.9)!!, 0.05)
        assertEquals(10.0, ndStopsFromOpticalDensity(3.0)!!, 0.05)
    }

    @Test
    fun ndFactor_convertsToStops() {
        assertEquals(3.0, ndStopsFromFactor(8.0)!!, 1e-9)
        assertEquals(6.0, ndStopsFromFactor(64.0)!!, 1e-9)
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(ndAdjustedShutterSeconds(0.0, 10.0))
        assertNull(ndStopsForDesiredShutter(0.0, 5.0))
        assertNull(ndStopsFromFactor(0.0))
    }
}
