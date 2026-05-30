package com.janhorak.shutterdeck.film.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilmDevelopmentTest {
    @Test
    fun calculateDilution_returnsStockAndWaterVolumes() {
        val result = calculateDilution(
            waterParts = 31.0,
            totalMilliliters = 500.0,
        )

        assertNotNull(result)
        assertEquals(15.625, result!!.stockMilliliters, 0.0001)
        assertEquals(484.375, result.waterMilliliters, 0.0001)
        assertEquals(500.0, result.totalMilliliters, 0.0001)
    }

    @Test
    fun developmentTemperatureFactor_interpolatesBetweenChartValues() {
        assertEquals(0.95, developmentTemperatureFactor(20.5)!!, 0.0001)
        assertEquals(0.63, developmentTemperatureFactor(24.0)!!, 0.0001)
    }

    @Test
    fun adjustDevelopmentTimeSeconds_returnsNullOutsideSupportedRange() {
        assertNull(adjustDevelopmentTimeSeconds(baseTimeAt20CSeconds = 480, chemistryTemperatureC = 15.5))
        assertEquals(302, adjustDevelopmentTimeSeconds(baseTimeAt20CSeconds = 480, chemistryTemperatureC = 24.0))
    }

    @Test
    fun buildAgitationCueOffsets_handlesRecurringAndStandModes() {
        assertEquals(listOf(60, 120, 180, 240), buildAgitationCueOffsets(durationSeconds = 300, agitationIntervalSeconds = 60))
        assertTrue(buildAgitationCueOffsets(durationSeconds = 300, agitationIntervalSeconds = 0).isEmpty())
    }

    @Test
    fun buildDevelopmentRecipeSteps_adjustsDeveloperAndSkipsDisabledSteps() {
        val steps = buildDevelopmentRecipeSteps(
            preSoakSeconds = 60,
            developerBaseSecondsAt20C = 480,
            chemistryTemperatureC = 24.0,
            stopBathSeconds = 30,
            fixerSeconds = 300,
            washSeconds = 0,
            agitationIntervalSeconds = 60,
        )

        assertNotNull(steps)
        assertEquals(listOf("Pre-soak", "Developer", "Stop bath", "Fixer"), steps!!.map { step -> step.name })
        assertEquals(302, steps[1].durationSeconds)
        assertEquals(60, steps[1].agitationIntervalSeconds)
    }
}
