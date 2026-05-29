package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrintSizeTest {

    @Test
    fun printSize_atThreeHundredDpi() {
        // 6000 x 4000 px at 300 dpi -> 20 x 13.33 inches.
        val result = printSizeAtDpi(6000, 4000, 300.0)!!
        assertEquals(20.0, result.widthInches, 1e-9)
        assertEquals(13.333, result.heightInches, 0.001)
        assertEquals(50.8, result.widthCm, 1e-6)
    }

    @Test
    fun requiredPixels_roundsUp() {
        val (w, h) = requiredPixelsForPrint(20.0, 13.333, 300.0)!!
        assertEquals(6000, w)
        assertEquals(4000, h)
    }

    @Test
    fun megapixels_computed() {
        assertEquals(24.0, megapixels(6000, 4000), 1e-9)
    }

    @Test
    fun goodEnoughDpi_fromViewingDistance() {
        // Viewed at 12 inches -> ~286.5 dpi.
        assertEquals(286.5, goodEnoughDpi(12.0)!!, 0.1)
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(printSizeAtDpi(0, 4000, 300.0))
        assertNull(requiredPixelsForPrint(20.0, 0.0, 300.0))
        assertNull(goodEnoughDpi(0.0))
    }
}
