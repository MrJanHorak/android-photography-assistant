package com.janhorak.shutterdeck.metering.domain

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class ExposureFormattingTest {

    @Before
    fun fixLocale() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun exposureTime_fractionsAndSeconds() {
        assertEquals("1/60 s", formatExposureTime(1.0 / 60.0))
        assertEquals("1/2 s", formatExposureTime(0.5))
        assertEquals("2 s", formatExposureTime(2.0))
        assertEquals("1.5 s", formatExposureTime(1.5))
        assertEquals("--", formatExposureTime(0.0))
    }

    @Test
    fun duration_secondsAndMinutes() {
        assertEquals("30 s", formatDuration(30.0))
        assertEquals("1 min", formatDuration(60.0))
        assertEquals("1 min 30 s", formatDuration(90.0))
    }

    @Test
    fun decimal_roundsWholeNumbers() {
        assertEquals("4", formatDecimal(4.0))
        assertEquals("2.8", formatDecimal(2.8))
    }

    @Test
    fun stopCount_singularAndPlural() {
        assertEquals("1.0 stop", formatStopCount(1.0))
        assertEquals("2.0 stops", formatStopCount(2.0))
    }
}
