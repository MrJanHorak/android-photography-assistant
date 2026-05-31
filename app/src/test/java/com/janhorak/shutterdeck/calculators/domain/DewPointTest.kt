package com.janhorak.shutterdeck.calculators.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DewPointTest {

    @Test
    fun dewPoint_matchesCommonReferenceValue() {
        val dewPoint = dewPointCelsius(
            airTemperatureCelsius = 20.0,
            relativeHumidityPercent = 60.0,
        )!!

        assertEquals(11.995454, dewPoint, 1e-6)
    }

    @Test
    fun dewPoint_matchesAirTemperature_atSaturation() {
        val dewPoint = dewPointCelsius(
            airTemperatureCelsius = 12.5,
            relativeHumidityPercent = 100.0,
        )!!

        assertEquals(12.5, dewPoint, 1e-9)
    }

    @Test
    fun dewPoint_usesIceBranch_belowFreezing() {
        val dewPoint = dewPointCelsius(
            airTemperatureCelsius = -5.0,
            relativeHumidityPercent = 90.0,
        )!!

        assertEquals(-6.226739, dewPoint, 1e-6)
    }

    @Test
    fun condensationRisk_usesExpectedThresholds() {
        val dewPoint = dewPointCelsius(
            airTemperatureCelsius = 20.0,
            relativeHumidityPercent = 60.0,
        )!!

        val active = analyzeCondensationRiskFromCelsius(
            airTemperatureCelsius = 20.0,
            relativeHumidityPercent = 60.0,
            surfaceTemperatureCelsius = dewPoint,
        )!!
        val warning = analyzeCondensationRiskFromCelsius(
            airTemperatureCelsius = 20.0,
            relativeHumidityPercent = 60.0,
            surfaceTemperatureCelsius = dewPoint + 2.0,
        )!!
        val low = analyzeCondensationRiskFromCelsius(
            airTemperatureCelsius = 20.0,
            relativeHumidityPercent = 60.0,
            surfaceTemperatureCelsius = dewPoint + 2.1,
        )!!

        assertEquals(CondensationRisk.ACTIVE, active.risk)
        assertEquals(0.0, active.surfaceMarginCelsius, 1e-9)
        assertEquals(CondensationRisk.WARNING, warning.risk)
        assertEquals(2.0, warning.surfaceMarginCelsius, 1e-9)
        assertEquals(CondensationRisk.LOW, low.risk)
        assertEquals(2.1, low.surfaceMarginCelsius, 1e-9)
    }

    @Test
    fun condensationRisk_supportsFahrenheitInputs() {
        val analysis = analyzeCondensationRiskFromFahrenheit(
            airTemperatureFahrenheit = 68.0,
            relativeHumidityPercent = 60.0,
            surfaceTemperatureFahrenheit = 54.0,
        )!!

        assertEquals(11.995454, analysis.dewPointCelsius, 1e-6)
        assertEquals(0.226768, analysis.surfaceMarginCelsius, 1e-6)
        assertEquals(CondensationRisk.WARNING, analysis.risk)
    }

    @Test
    fun invalidInputs_returnNull() {
        assertNull(dewPointCelsius(20.0, 0.0))
        assertNull(dewPointCelsius(20.0, 101.0))
        assertNull(analyzeCondensationRiskFromCelsius(Double.NaN, 60.0, 10.0))
        assertNull(analyzeCondensationRiskFromFahrenheit(68.0, Double.POSITIVE_INFINITY, 54.0))
    }
}
