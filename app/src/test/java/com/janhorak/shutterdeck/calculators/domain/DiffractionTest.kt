package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiffractionTest {

    @Test
    fun airyDisk_forKnownAperture() {
        // 2.44 * 0.55um * 8 = 10.736um.
        assertEquals(10.736, airyDiskDiameterMicrons(8.0)!!, 0.001)
    }

    @Test
    fun diffractionLimitedAperture_forPixelPitch() {
        // 2 * 4um / (2.44 * 0.55um) ~= 5.96.
        assertEquals(5.961, diffractionLimitedAperture(4.0)!!, 0.01)
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(airyDiskDiameterMicrons(0.0))
        assertNull(diffractionLimitedAperture(0.0))
    }
}
