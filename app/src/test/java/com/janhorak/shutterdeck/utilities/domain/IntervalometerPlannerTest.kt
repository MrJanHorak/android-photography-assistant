package com.janhorak.shutterdeck.utilities.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntervalometerPlannerTest {

    @Test
    fun `single frame has zero capture window`() {
        val plan = buildIntervalometerPlan(
            intervalSeconds = 30,
            frameCount = 1,
            playbackFramesPerSecond = 24,
        )

        assertEquals(0L, plan?.captureWindowSeconds)
    }

    @Test
    fun `capture window runs from first frame to last frame`() {
        val plan = buildIntervalometerPlan(
            intervalSeconds = 30,
            frameCount = 2,
            playbackFramesPerSecond = 24,
        )

        assertEquals(30L, plan?.captureWindowSeconds)
    }

    @Test
    fun `plan computes clip length storage cards and batteries`() {
        val plan = buildIntervalometerPlan(
            intervalSeconds = 5,
            frameCount = 240,
            playbackFramesPerSecond = 24,
            averageFrameSizeMegabytes = 30.0,
            cardCapacityGigabytes = 64.0,
            shotsPerBattery = 400,
        )

        requireNotNull(plan)
        assertEquals(1195L, plan.captureWindowSeconds)
        assertEquals(10.0, plan.clipLengthSeconds, 1e-9)
        assertEquals(12.0, plan.framesPerMinute, 1e-9)
        assertEquals(7200.0, plan.storageRequiredMegabytes!!, 1e-9)
        assertEquals(7.03125, plan.storageRequiredGigabytes!!, 1e-9)
        assertEquals(1, plan.cardsNeeded)
        assertEquals(1, plan.batteriesNeeded)
    }

    @Test
    fun `cards and batteries round up when the plan exceeds one unit`() {
        val plan = buildIntervalometerPlan(
            intervalSeconds = 10,
            frameCount = 700,
            playbackFramesPerSecond = 24,
            averageFrameSizeMegabytes = 50.0,
            cardCapacityGigabytes = 32.0,
            shotsPerBattery = 400,
        )

        requireNotNull(plan)
        assertEquals(35000.0, plan.storageRequiredMegabytes!!, 1e-9)
        assertEquals(34.1796875, plan.storageRequiredGigabytes!!, 1e-9)
        assertEquals(2, plan.cardsNeeded)
        assertEquals(2, plan.batteriesNeeded)
    }

    @Test
    fun `exposure check reports remaining headroom`() {
        val plan = buildIntervalometerPlan(
            intervalSeconds = 5,
            frameCount = 240,
            playbackFramesPerSecond = 24,
            exposureSeconds = 3.2,
        )

        requireNotNull(plan)
        requireNotNull(plan.exposureCheck)
        assertFalse(plan.exposureCheck.overrunsInterval)
        assertEquals(1.8, plan.exposureCheck.slackSeconds, 1e-9)
    }

    @Test
    fun `exposure check flags interval overruns`() {
        val plan = buildIntervalometerPlan(
            intervalSeconds = 5,
            frameCount = 240,
            playbackFramesPerSecond = 24,
            exposureSeconds = 5.5,
        )

        requireNotNull(plan)
        requireNotNull(plan.exposureCheck)
        assertTrue(plan.exposureCheck.overrunsInterval)
        assertEquals(-0.5, plan.exposureCheck.slackSeconds, 1e-9)
    }

    @Test
    fun `invalid values return null`() {
        assertNull(
            buildIntervalometerPlan(
                intervalSeconds = 0,
                frameCount = 240,
                playbackFramesPerSecond = 24,
            ),
        )
        assertNull(
            buildIntervalometerPlan(
                intervalSeconds = 5,
                frameCount = 0,
                playbackFramesPerSecond = 24,
            ),
        )
        assertNull(
            buildIntervalometerPlan(
                intervalSeconds = 5,
                frameCount = 240,
                playbackFramesPerSecond = 0,
            ),
        )
        assertNull(
            buildIntervalometerPlan(
                intervalSeconds = 5,
                frameCount = 240,
                playbackFramesPerSecond = 24,
                averageFrameSizeMegabytes = 0.0,
            ),
        )
        assertNull(
            buildIntervalometerPlan(
                intervalSeconds = 5,
                frameCount = 240,
                playbackFramesPerSecond = 24,
                cardCapacityGigabytes = 0.0,
            ),
        )
        assertNull(
            buildIntervalometerPlan(
                intervalSeconds = 5,
                frameCount = 240,
                playbackFramesPerSecond = 24,
                shotsPerBattery = 0,
            ),
        )
        assertNull(
            buildIntervalometerPlan(
                intervalSeconds = 5,
                frameCount = 240,
                playbackFramesPerSecond = 24,
                exposureSeconds = 0.0,
            ),
        )
    }
}
