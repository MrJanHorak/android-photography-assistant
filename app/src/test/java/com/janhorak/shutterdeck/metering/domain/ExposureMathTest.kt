package com.janhorak.shutterdeck.metering.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExposureMathTest {

    @Test
    fun evFromLux_matchesDefinition() {
        // EV = log2(lux / 2.5); 2.5 lux -> 0 EV, 1280 lux -> 9 EV.
        assertEquals(0f, evFromLux(2.5f), 0.0001f)
        assertEquals(9f, evFromLux(1280f), 0.0001f)
    }

    @Test
    fun reflectiveEv100_knownExposure() {
        // f/1, 1s, ISO 100 -> EV100 = 0.
        assertEquals(0f, reflectiveEv100(1f, 1.0, 100)!!, 0.0001f)
        // f/4, 1/60s, ISO 100 -> EV100 = log2(16*60) ~= 9.91.
        assertEquals(9.906f, reflectiveEv100(4f, 1.0 / 60.0, 100)!!, 0.01f)
    }

    @Test
    fun reflectiveEv100_invalidInputsReturnNull() {
        assertNull(reflectiveEv100(0f, 1.0, 100))
        assertNull(reflectiveEv100(2.8f, 0.0, 100))
        assertNull(reflectiveEv100(2.8f, 1.0, 0))
    }

    @Test
    fun requiredShutterSeconds_apertureStops() {
        // At EV100 = 0, f/1, ISO 100 -> 1 second.
        assertEquals(1.0, requiredShutterSeconds(1f, 100, 0f)!!, 0.0001)
        // Closing one stop (f/1 -> f/1.414) roughly doubles the required time (ISO-independent).
        val wideOpen = requiredShutterSeconds(1f, 100, 0f)!!
        val stoppedDown = requiredShutterSeconds(1.4142f, 100, 0f)!!
        assertEquals(2.0, stoppedDown / wideOpen, 0.01)
    }

    @Test
    fun requiredShutterSeconds_nullEvReturnsNull() {
        assertNull(requiredShutterSeconds(2.8f, 100, null))
    }

    @Test
    fun apertureGainStops_openingTwoStops() {
        // f/4 -> f/2 is two stops of extra light.
        assertEquals(2.0, apertureGainStops(4f, 2f), 0.0001)
        // Same aperture is zero stops.
        assertEquals(0.0, apertureGainStops(2.8f, 2.8f), 0.0001)
    }

    @Test
    fun handheldMinimum_followsReciprocalRuleAndCrop() {
        // 50mm, no crop, no stabilization -> ~1/50 s.
        assertEquals(1.0 / 50.0, calculateHandheldMinimumShutterSeconds(50, 1.0f, 0f), 0.0001)
        // Crop factor doubles effective focal length -> halves the time.
        assertEquals(1.0 / 100.0, calculateHandheldMinimumShutterSeconds(50, 2.0f, 0f), 0.0001)
    }
}
