package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarTimesTest {

    @Test
    fun newYorkSummerSolstice_matchesPublishedTimes() {
        // NYC (40.7128, -74.0060), 2024-06-20, EDT (UTC-4).
        // Published NOAA: sunrise ~5:25, solar noon ~12:57, sunset ~20:31.
        val times = calculateSunTimes(2024, 6, 20, 40.7128, -74.0060, -4.0)!!
        assertTrue("sunrise", times.sunriseMinutes!! in 319.0..329.0) // 5:19-5:29
        assertTrue("solar noon", times.solarNoonMinutes in 772.0..782.0) // 12:52-13:02
        assertTrue("sunset", times.sunsetMinutes!! in 1226.0..1236.0) // 20:26-20:36
    }

    @Test
    fun eventOrderingIsChronological() {
        val t = calculateSunTimes(2024, 6, 20, 40.7128, -74.0060, -4.0)!!
        assertTrue(t.morningBlueHourStartMinutes!! < t.sunriseMinutes!!)
        assertTrue(t.sunriseMinutes!! < t.morningGoldenHourEndMinutes!!)
        assertTrue(t.morningGoldenHourEndMinutes!! < t.solarNoonMinutes)
        assertTrue(t.solarNoonMinutes < t.eveningGoldenHourStartMinutes!!)
        assertTrue(t.eveningGoldenHourStartMinutes!! < t.sunsetMinutes!!)
        assertTrue(t.sunsetMinutes!! < t.eveningBlueHourEndMinutes!!)
    }

    @Test
    fun polarDay_hasNoSunriseButHasSolarNoon() {
        // Far north in summer: the sun never sets.
        val t = calculateSunTimes(2024, 6, 20, 78.0, 15.0, 1.0)!!
        assertNull(t.sunriseMinutes)
        assertNull(t.sunsetMinutes)
        assertNotNull(t.solarNoonMinutes)
    }

    @Test
    fun invalidCoordinatesReturnNull() {
        assertNull(calculateSunTimes(2024, 6, 20, 95.0, 0.0, 0.0))
        assertNull(calculateSunTimes(2024, 6, 20, 0.0, 200.0, 0.0))
    }
}
