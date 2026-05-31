package com.janhorak.shutterdeck.metering.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IlluminanceMathTest {

    @Test
    fun ev100FromLux_matchesReferenceValues() {
        assertEquals(0.0, ev100FromLux(2.5)!!, 0.0001)
        assertEquals(9.0, ev100FromLux(1280.0)!!, 0.0001)
    }

    @Test
    fun luxFromEv100_matchesInverseReferenceValues() {
        assertEquals(2.5, luxFromEv100(0.0)!!, 0.0001)
        assertEquals(1280.0, luxFromEv100(9.0)!!, 0.0001)
    }

    @Test
    fun footCandleConversions_matchReferenceRatio() {
        assertEquals(10.7639, luxFromFootCandles(1.0)!!, 0.0001)
        assertEquals(1.0, footCandlesFromLux(10.7639)!!, 0.0001)
    }

    @Test
    fun conversions_roundTripAcrossUnits() {
        val brightDayLux = 10_000.0
        assertEquals(
            brightDayLux,
            luxFromEv100(ev100FromLux(brightDayLux)!!)!!,
            0.01,
        )

        val dimInteriorEv100 = -3.5
        assertEquals(
            dimInteriorEv100,
            ev100FromLux(luxFromEv100(dimInteriorEv100)!!)!!,
            0.0001,
        )

        val studioFootCandles = 75.0
        assertEquals(
            studioFootCandles,
            footCandlesFromLux(luxFromFootCandles(studioFootCandles)!!)!!,
            0.0001,
        )
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(ev100FromLux(0.0))
        assertNull(ev100FromLux(-1.0))
        assertNull(luxFromEv100(Double.POSITIVE_INFINITY))
        assertNull(footCandlesFromLux(-0.1))
        assertNull(luxFromFootCandles(Double.NaN))
        assertNull(convertIlluminanceFromLux(0.0))
        assertNull(convertIlluminanceFromFootCandles(0.0))
    }
}
