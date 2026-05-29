package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CelestialPositionTest {

    @Test
    fun sunSolarNoonSolstice_highAndDueSouth() {
        // NYC (40.7128, -74.0060), 2024-06-20 ~13:00 EDT, near solar noon.
        // Max altitude ≈ 90 - lat + declination = 90 - 40.71 + 23.44 ≈ 72.7°.
        val pos = sunPosition(2024, 6, 20, 13, 0, 40.7128, -74.0060, -4.0)!!
        assertEquals(72.7, pos.altitudeDeg, 1.0)
        assertTrue("azimuth near south", pos.azimuthDeg in 170.0..192.0)
    }

    @Test
    fun sunIsBelowHorizonAtNight() {
        val pos = sunPosition(2024, 6, 20, 1, 0, 40.7128, -74.0060, -4.0)!!
        assertTrue("sun below horizon at 1am", pos.altitudeDeg < 0.0)
    }

    @Test
    fun moonIllumination_fullMoon() {
        // Full moon 2024-06-22 ~01:08 UTC.
        val illum = moonIllumination(2024, 6, 22, 0, 0, 0.0)!!
        assertTrue("nearly fully lit", illum.fraction > 0.98)
        assertEquals(0.5, illum.phase, 0.03)
        assertEquals("Full moon", illum.phaseName)
    }

    @Test
    fun moonIllumination_newMoon() {
        // New moon 2024-06-06 ~12:38 UTC.
        val illum = moonIllumination(2024, 6, 6, 12, 0, 0.0)!!
        assertTrue("almost dark", illum.fraction < 0.02)
        assertEquals("New moon", illum.phaseName)
    }

    @Test
    fun moonIllumination_firstQuarter() {
        // First quarter 2024-06-14 ~05:18 UTC.
        val illum = moonIllumination(2024, 6, 14, 4, 0, 0.0)!!
        assertEquals(0.5, illum.fraction, 0.05)
        assertEquals(0.25, illum.phase, 0.02)
    }

    @Test
    fun moonPosition_returnsDistanceInPlausibleRange() {
        val pos = moonPosition(2024, 6, 22, 0, 0, 40.7128, -74.0060, -4.0)!!
        assertTrue("moon distance plausible", pos.distanceKm!! in 356000.0..407000.0)
        assertTrue("azimuth normalized", pos.azimuthDeg in 0.0..360.0)
    }

    @Test
    fun compassDirectionMapsCardinals() {
        assertEquals("N", compassDirection(0.0))
        assertEquals("E", compassDirection(90.0))
        assertEquals("S", compassDirection(180.0))
        assertEquals("W", compassDirection(270.0))
        assertEquals("N", compassDirection(359.0))
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(sunPosition(2024, 13, 1, 12, 0, 0.0, 0.0, 0.0))
        assertNull(sunPosition(2024, 6, 20, 25, 0, 0.0, 0.0, 0.0))
        assertNull(moonPosition(2024, 6, 20, 12, 0, 95.0, 0.0, 0.0))
        assertNull(moonIllumination(2024, 6, 20, 12, 99, 0.0))
    }
}
