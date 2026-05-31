package com.janhorak.shutterdeck.utilities.domain

import com.janhorak.shutterdeck.core.time.formatStructuredDateTime
import java.time.Instant
import java.time.ZoneId

fun appendShotNoteTranscript(
    currentNoteText: String,
    transcript: String,
): String {
    val trimmedTranscript = transcript.trim()
    if (trimmedTranscript.isBlank()) return currentNoteText

    val trimmedCurrentNoteText = currentNoteText.trimEnd()
    return if (trimmedCurrentNoteText.isBlank()) {
        trimmedTranscript
    } else {
        "$trimmedCurrentNoteText $trimmedTranscript"
    }
}

fun formatShotNoteTimestamp(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = formatStructuredDateTime(
    Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime(),
)
