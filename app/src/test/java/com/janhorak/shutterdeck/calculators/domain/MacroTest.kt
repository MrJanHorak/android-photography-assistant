package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MacroTest {

    @Test
    fun magnification_fromExtension() {
        // 25mm extension on a 50mm lens -> 0.5x.
        assertEquals(0.5, magnificationFromExtension(50.0, 25.0)!!, 1e-9)
    }

    @Test
    fun effectiveAperture_andCompensation() {
        val result = calculateMacro(50.0, 2.8, 25.0)!!
        assertEquals(0.5, result.magnification, 1e-9)
        assertEquals(4.2, result.effectiveAperture, 1e-9)
        // 2 * log2(1.5) ~= 1.17 stops.
        assertEquals(1.1699, result.exposureCompensationStops, 0.001)
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(magnificationFromExtension(0.0, 25.0))
        assertNull(effectiveAperture(2.8, -0.1))
        assertNull(calculateMacro(0.0, 2.8, 25.0))
    }
}
