package com.janhorak.shutterdeck.core.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val structuredTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val structuredDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun parseStructuredDateOrNull(text: String): LocalDate? {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return null

    return try {
        LocalDate.parse(trimmed)
    } catch (_: DateTimeParseException) {
        null
    }
}

fun formatStructuredDate(date: LocalDate): String = date.toString()

fun parseStructuredTimeOrNull(text: String): LocalTime? {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return null

    return try {
        LocalTime.parse(trimmed, structuredTimeFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
}

fun formatStructuredTime(time: LocalTime): String = time.withSecond(0).withNano(0).format(structuredTimeFormatter)

fun parseStructuredDateTimeOrNull(text: String): LocalDateTime? {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return null

    return try {
        LocalDateTime.parse(trimmed, structuredDateTimeFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
}

fun formatStructuredDateTime(dateTime: LocalDateTime): String {
    return dateTime.withSecond(0).withNano(0).format(structuredDateTimeFormatter)
}
