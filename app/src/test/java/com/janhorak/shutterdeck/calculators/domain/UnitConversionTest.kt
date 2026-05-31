package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitConversionTest {

    @Test
    fun distanceConversions_matchReferenceValues() {
        assertEquals(3.280839895, feetFromMeters(1.0)!!, 1e-9)
        assertEquals(1.8288, metersFromFeet(6.0)!!, 1e-9)
    }

    @Test
    fun temperatureConversions_matchReferenceValues() {
        assertEquals(32.0, fahrenheitFromCelsius(0.0)!!, 1e-9)
        assertEquals(100.0, celsiusFromFahrenheit(212.0)!!, 1e-9)
    }

    @Test
    fun wrapperConversions_keepBothUnitsTogether() {
        val fromMeters = convertDistanceFromMeters(2.0)!!
        assertEquals(2.0, fromMeters.meters, 1e-9)
        assertEquals(6.56167979, fromMeters.feet, 1e-8)

        val fromFahrenheit = convertTemperatureFromFahrenheit(68.0)!!
        assertEquals(20.0, fromFahrenheit.celsius, 1e-9)
        assertEquals(68.0, fromFahrenheit.fahrenheit, 1e-9)
    }

    @Test
    fun conversions_roundTripCleanly() {
        val fieldDistanceFeet = 15.0
        assertEquals(
            fieldDistanceFeet,
            feetFromMeters(metersFromFeet(fieldDistanceFeet)!!)!!,
            1e-9,
        )

        val coldWeatherCelsius = -7.5
        assertEquals(
            coldWeatherCelsius,
            celsiusFromFahrenheit(fahrenheitFromCelsius(coldWeatherCelsius)!!)!!,
            1e-9,
        )
    }

    @Test
    fun invalidInputsReturnNull() {
        assertNull(feetFromMeters(-0.1))
        assertNull(metersFromFeet(Double.NaN))
        assertNull(fahrenheitFromCelsius(Double.NEGATIVE_INFINITY))
        assertNull(celsiusFromFahrenheit(Double.POSITIVE_INFINITY))
        assertNull(convertDistanceFromMeters(Double.NaN))
        assertNull(convertDistanceFromFeet(-3.0))
        assertNull(convertTemperatureFromCelsius(Double.NaN))
        assertNull(convertTemperatureFromFahrenheit(Double.POSITIVE_INFINITY))
    }
}
