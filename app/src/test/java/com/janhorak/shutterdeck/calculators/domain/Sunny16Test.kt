package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Sunny16Test {

    @Test
    fun sunnySixteenBaseline() {
        // f/16, ISO 100, bright sun -> 1/100 s.
        assertEquals(0.01, sunny16ShutterSeconds(100, 16.0, 0.0)!!, 1e-9)
    }

    @Test
    fun openingUpShortensTime() {
        // f/8 is two stops faster than f/16 -> 1/400 s.
        assertEquals(0.0025, sunny16ShutterSeconds(100, 8.0, 0.0)!!, 1e-9)
    }

    @Test
    fun darkerLightLengthensTime() {
        // Overcast is 2 stops darker than sunny -> 4x the time at f/16.
        assertEquals(0.04, sunny16ShutterSeconds(100, 16.0, 2.0)!!, 1e-9)
    }

    @Test
    fun reciprocity_onlyAppliesAboveOneSecond() {
        assertEquals(0.5, reciprocityCorrectedSeconds(0.5, 1.3)!!, 1e-9)
        assertEquals(Math.pow(2.0, 1.3), reciprocityCorrectedSeconds(2.0, 1.3)!!, 1e-6)
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(sunny16ShutterSeconds(0, 16.0, 0.0))
        assertNull(reciprocityCorrectedSeconds(0.0, 1.3))
    }
}
