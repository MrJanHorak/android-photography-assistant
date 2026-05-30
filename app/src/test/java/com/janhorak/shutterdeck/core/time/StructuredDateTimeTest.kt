package com.janhorak.shutterdeck.core.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StructuredDateTimeTest {

    @Test
    fun parseStructuredDateRequiresIsoDate() {
        assertEquals(LocalDate.of(2026, 5, 30), parseStructuredDateOrNull("2026-05-30"))
        assertNull(parseStructuredDateOrNull("2026/05/30"))
        assertNull(parseStructuredDateOrNull("  "))
    }

    @Test
    fun formatStructuredTimeUsesZeroPadded24HourClock() {
        assertEquals("07:05", formatStructuredTime(LocalTime.of(7, 5, 42)))
    }

    @Test
    fun parseAndFormatStructuredDateTimeUseSharedTimestampPattern() {
        val dateTime = LocalDateTime.of(2026, 5, 30, 21, 7)

        assertEquals(dateTime, parseStructuredDateTimeOrNull("2026-05-30 21:07"))
        assertEquals("2026-05-30 21:07", formatStructuredDateTime(dateTime))
        assertNull(parseStructuredDateTimeOrNull("2026-05-30T21:07"))
    }
}
