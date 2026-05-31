package com.janhorak.shutterdeck.utilities.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ShotNotesTest {

    @Test
    fun `blank note becomes the transcript`() {
        val merged = appendShotNoteTranscript(
            currentNoteText = "",
            transcript = "  Cloud bank rolling in from the west  ",
        )

        assertEquals("Cloud bank rolling in from the west", merged)
    }

    @Test
    fun `existing note appends transcript with one separator`() {
        val merged = appendShotNoteTranscript(
            currentNoteText = "Tripod moved closer",
            transcript = "need one more frame",
        )

        assertEquals("Tripod moved closer need one more frame", merged)
    }

    @Test
    fun `timestamp formatting uses the requested local zone`() {
        val zoneId = ZoneId.of("America/New_York")
        val epochMillis = LocalDateTime.of(2024, 4, 30, 20, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val formatted = formatShotNoteTimestamp(
            epochMillis = epochMillis,
            zoneId = zoneId,
        )

        assertEquals("2024-04-30 20:00", formatted)
    }
}
