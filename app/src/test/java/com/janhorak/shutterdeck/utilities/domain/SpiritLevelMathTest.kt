package com.janhorak.shutterdeck.utilities.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class SpiritLevelMathTest {

    @Test
    fun flatDevice_reportsLevel() {
        val reading = calculateSpiritLevelReading(
            x = 0f,
            y = 0f,
            z = 9.81f,
        )

        assertNotNull(reading)
        assertEquals(0.0, reading!!.pitchDegrees, 0.0001)
        assertEquals(0.0, reading.rollDegrees, 0.0001)
        assertEquals(0f, reading.pitchOffset)
        assertEquals(0f, reading.rollOffset)
        assertTrue(reading.isLevel)
        assertFalse(reading.tooSteep)
    }

    @Test
    fun rightTilt_increasesRoll() {
        val angleDegrees = 10.0
        val reading = calculateSpiritLevelReading(
            x = sin(Math.toRadians(angleDegrees)).toFloat(),
            y = 0f,
            z = cos(Math.toRadians(angleDegrees)).toFloat(),
        )

        assertNotNull(reading)
        assertEquals(0.0, reading!!.pitchDegrees, 0.05)
        assertEquals(angleDegrees, reading.rollDegrees, 0.05)
        assertFalse(reading.isLevel)
    }

    @Test
    fun topTilt_increasesPitch() {
        val angleDegrees = 5.0
        val reading = calculateSpiritLevelReading(
            x = 0f,
            y = sin(Math.toRadians(angleDegrees)).toFloat(),
            z = cos(Math.toRadians(angleDegrees)).toFloat(),
        )

        assertNotNull(reading)
        assertEquals(angleDegrees, reading!!.pitchDegrees, 0.05)
        assertEquals(0.0, reading.rollDegrees, 0.05)
        assertFalse(reading.isLevel)
    }

    @Test
    fun steepTilt_suppressesBubble() {
        val angleDegrees = 80.0
        val reading = calculateSpiritLevelReading(
            x = 0f,
            y = sin(Math.toRadians(angleDegrees)).toFloat(),
            z = cos(Math.toRadians(angleDegrees)).toFloat(),
        )

        assertNotNull(reading)
        assertTrue(reading!!.tooSteep)
        assertFalse(reading.isLevel)
        assertEquals(0f, reading.pitchOffset)
        assertEquals(0f, reading.rollOffset)
    }
}
