package com.janhorak.shutterdeck.film.domain

import com.janhorak.shutterdeck.core.time.formatStructuredDate
import com.janhorak.shutterdeck.core.time.formatStructuredDateTime
import java.time.LocalDate
import java.time.LocalDateTime

const val FILM_ROLL_STATUS_ACTIVE = "Active"
const val FILM_ROLL_STATUS_FINISHED = "Finished"

val filmRollStatusOptions = listOf(FILM_ROLL_STATUS_ACTIVE, FILM_ROLL_STATUS_FINISHED)

fun defaultFilmRollStartedOnText(date: LocalDate = LocalDate.now()): String = formatStructuredDate(date)

fun defaultFilmFrameCapturedAtText(dateTime: LocalDateTime = LocalDateTime.now()): String = formatStructuredDateTime(dateTime)
