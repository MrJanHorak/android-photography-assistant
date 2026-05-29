package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AstroShutterTest {

    @Test
    fun ruleOf500_matchesSimpleDivision() {
        // 20mm full frame -> 500 / 20 = 25s.
        assertEquals(25.0, ruleBasedMaxShutterSeconds(20.0, 1.0)!!, 1e-9)
        // 20mm on 1.5x crop -> 500 / 30 = 16.67s.
        assertEquals(16.667, ruleBasedMaxShutterSeconds(20.0, 1.5)!!, 0.01)
    }

    @Test
    fun npfRule_matchesFormula() {
        // (35*2.8 + 30*4) / 20 = (98 + 120) / 20 = 10.9s.
        assertEquals(10.9, npfMaxShutterSeconds(2.8, 4.0, 20.0)!!, 1e-9)
    }

    @Test
    fun pixelPitch_fromSensorWidthAndResolution() {
        // 36mm wide, 6000px -> 6 microns.
        assertEquals(6.0, pixelPitchMicrons(36.0, 6000)!!, 1e-9)
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(ruleBasedMaxShutterSeconds(0.0, 1.0))
        assertNull(npfMaxShutterSeconds(2.8, 4.0, 0.0))
        assertNull(pixelPitchMicrons(36.0, 0))
    }
}
