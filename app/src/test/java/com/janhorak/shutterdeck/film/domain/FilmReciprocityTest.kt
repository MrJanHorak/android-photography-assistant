package com.janhorak.shutterdeck.film.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilmReciprocityTest {
    @Test
    fun evaluateFilmReciprocity_returnsUnavailableGuidanceWhenCurveMissing() {
        val guidance = evaluateFilmReciprocity(
            meteredSeconds = 12.0,
            exponent = null,
            onsetSeconds = null,
        )

        assertNotNull(guidance)
        assertEquals(ReciprocityStatus.NO_CURVE_SAVED, guidance!!.status)
        assertEquals(12.0, guidance.correctedSeconds, 0.0001)
        assertTrue(guidance.timingSummary.contains("No saved reciprocity curve"))
    }

    @Test
    fun evaluateFilmReciprocity_skipsCorrectionBelowOnset() {
        val guidance = evaluateFilmReciprocity(
            meteredSeconds = 0.8,
            exponent = 1.26,
            onsetSeconds = 1.0,
        )

        assertNotNull(guidance)
        assertEquals(ReciprocityStatus.BELOW_ONSET, guidance!!.status)
        assertEquals(0.8, guidance.correctedSeconds, 0.0001)
        assertEquals(0.0, guidance.compensationStops, 0.0001)
    }

    @Test
    fun evaluateFilmReciprocity_correctsLongExposureWithSavedCurve() {
        val guidance = evaluateFilmReciprocity(
            meteredSeconds = 10.0,
            exponent = 1.26,
            onsetSeconds = 1.0,
        )

        assertNotNull(guidance)
        assertEquals(ReciprocityStatus.CORRECTED, guidance!!.status)
        assertEquals(18.197, guidance.correctedSeconds, 0.001)
        assertEquals(8.197, guidance.addedSeconds, 0.001)
        assertEquals(0.864, guidance.compensationStops, 0.001)
    }

    @Test
    fun buildReciprocityNote_includesContextAndSessionNotes() {
        val guidance = evaluateFilmReciprocity(
            meteredSeconds = 10.0,
            exponent = 1.26,
            onsetSeconds = 1.0,
        )!!

        val note = buildReciprocityNote(
            stockDisplayName = "Ilford HP5 Plus 400",
            rollDisplayTitle = "Pier long exposures",
            processingType = "B&W",
            guidance = guidance,
            developerNotes = "Reciprocity fit is a starter value only.",
            sessionNotes = "Bracket one frame with an extra stop just in case.",
        )

        assertTrue(note.contains("Stock: Ilford HP5 Plus 400"))
        assertTrue(note.contains("Roll: Pier long exposures"))
        assertTrue(note.contains("Corrected time: 18 s"))
        assertTrue(note.contains("Stock notes: Reciprocity fit is a starter value only."))
        assertTrue(note.contains("Session notes: Bracket one frame with an extra stop just in case."))
    }

    @Test
    fun evaluateFilmReciprocity_rejectsInvalidInput() {
        assertNull(evaluateFilmReciprocity(meteredSeconds = 0.0, exponent = 1.26, onsetSeconds = 1.0))
        assertNull(evaluateFilmReciprocity(meteredSeconds = 10.0, exponent = -1.0, onsetSeconds = 1.0))
        assertNull(evaluateFilmReciprocity(meteredSeconds = 10.0, exponent = 1.26, onsetSeconds = -1.0))
    }
}
