package com.janhorak.shutterdeck.film.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilmPushPullTest {
    @Test
    fun evaluatePushPull_returnsBoxSpeedGuidanceForExactIsoMatch() {
        val guidance = evaluatePushPull(
            baseIso = 400,
            targetExposureIndex = 400,
            maxPushStops = 2,
            maxPullStops = 1,
        )

        assertNotNull(guidance)
        assertEquals(PushPullDirection.BOX_SPEED, guidance!!.direction)
        assertTrue(guidance.isExactBoxSpeed)
        assertEquals("Box speed", guidance.adjustmentLabel)
        assertEquals(PushPullLatitudeStatus.WITHIN_SAVED_RANGE, guidance.latitudeStatus)
    }

    @Test
    fun evaluatePushPull_marksSubThirdStopShiftAsNearBoxSpeed() {
        val guidance = evaluatePushPull(
            baseIso = 400,
            targetExposureIndex = 410,
            maxPushStops = 2,
            maxPullStops = 1,
        )

        assertNotNull(guidance)
        assertEquals(PushPullDirection.BOX_SPEED, guidance!!.direction)
        assertTrue(!guidance.isExactBoxSpeed)
        assertEquals("~Box speed", guidance.adjustmentLabel)
    }

    @Test
    fun evaluatePushPull_usesRawDeltaForLatitudeBoundaryChecks() {
        val guidance = evaluatePushPull(
            baseIso = 400,
            targetExposureIndex = 1700,
            maxPushStops = 2,
            maxPullStops = 1,
        )

        assertNotNull(guidance)
        assertEquals(PushPullDirection.PUSH, guidance!!.direction)
        assertEquals(2.0, guidance.roundedStopDelta, 0.0001)
        assertTrue(guidance.rawStopDelta > 2.0)
        assertEquals(PushPullLatitudeStatus.OUTSIDE_SAVED_RANGE, guidance.latitudeStatus)
    }

    @Test
    fun evaluatePushPull_returnsUnknownLatitudeWhenStockRangeIsMissing() {
        val guidance = evaluatePushPull(
            baseIso = 400,
            targetExposureIndex = 200,
            maxPushStops = null,
            maxPullStops = null,
        )

        assertNotNull(guidance)
        assertEquals(PushPullDirection.PULL, guidance!!.direction)
        assertEquals(PushPullLatitudeStatus.UNKNOWN, guidance.latitudeStatus)
    }

    @Test
    fun buildPushPullNote_includesRollAndStockNotes() {
        val guidance = evaluatePushPull(
            baseIso = 400,
            targetExposureIndex = 1600,
            maxPushStops = 2,
            maxPullStops = 1,
        )!!

        val note = buildPushPullNote(
            stockDisplayName = "Kodak Tri-X 400",
            rollDisplayTitle = "Night street roll",
            processingType = "B&W",
            guidance = guidance,
            developerNotes = "Rodinal 1+25 starts to get punchy above +1.",
            sessionNotes = "Meter the alley sequence at EI 1600.",
        )

        assertTrue(note.contains("Stock: Kodak Tri-X 400"))
        assertTrue(note.contains("Roll: Night street roll"))
        assertTrue(note.contains("Adjustment: +2 push"))
        assertTrue(note.contains("Stock notes: Rodinal 1+25 starts to get punchy above +1."))
        assertTrue(note.contains("Session notes: Meter the alley sequence at EI 1600."))
    }

    @Test
    fun evaluatePushPull_returnsNullForInvalidInput() {
        assertNull(evaluatePushPull(baseIso = 0, targetExposureIndex = 400, maxPushStops = 2, maxPullStops = 1))
        assertNull(evaluatePushPull(baseIso = 400, targetExposureIndex = -1, maxPushStops = 2, maxPullStops = 1))
        assertNull(evaluatePushPull(baseIso = 400, targetExposureIndex = 800, maxPushStops = -1, maxPullStops = 1))
    }
}
