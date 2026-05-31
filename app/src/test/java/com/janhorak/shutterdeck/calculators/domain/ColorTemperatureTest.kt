package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorTemperatureTest {

    @Test
    fun kelvinToMired_matchesReferenceValues() {
        assertEquals(312.5, miredFromKelvin(3200.0)!!, 1e-9)
        assertEquals(178.5714285714, miredFromKelvin(5600.0)!!, 1e-9)
    }

    @Test
    fun miredToKelvin_matchesReferenceValues() {
        assertEquals(3200.0, kelvinFromMired(312.5)!!, 1e-9)
        assertEquals(5600.0, kelvinFromMired(178.5714285714)!!, 1e-6)
    }

    @Test
    fun conversionWrappers_keepBothUnitsTogether() {
        val fromKelvin = convertColorTemperatureFromKelvin(6500.0)!!
        assertEquals(6500.0, fromKelvin.kelvin, 1e-9)
        assertEquals(153.8461538461, fromKelvin.mired, 1e-9)

        val fromMired = convertColorTemperatureFromMired(200.0)!!
        assertEquals(5000.0, fromMired.kelvin, 1e-9)
        assertEquals(200.0, fromMired.mired, 1e-9)
    }

    @Test
    fun conversions_roundTripCleanly() {
        val daylightKelvin = 5600.0
        assertEquals(
            daylightKelvin,
            kelvinFromMired(miredFromKelvin(daylightKelvin)!!)!!,
            1e-6,
        )

        val tungstenMired = 312.5
        assertEquals(
            tungstenMired,
            miredFromKelvin(kelvinFromMired(tungstenMired)!!)!!,
            1e-9,
        )
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(miredFromKelvin(0.0))
        assertNull(miredFromKelvin(Double.NaN))
        assertNull(kelvinFromMired(0.0))
        assertNull(kelvinFromMired(Double.POSITIVE_INFINITY))
        assertNull(convertColorTemperatureFromKelvin(-3200.0))
        assertNull(convertColorTemperatureFromMired(-200.0))
    }
}
