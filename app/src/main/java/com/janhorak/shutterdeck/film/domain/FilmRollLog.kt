package com.janhorak.shutterdeck.film.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val FILM_ROLL_STATUS_ACTIVE = "Active"
const val FILM_ROLL_STATUS_FINISHED = "Finished"

val filmRollStatusOptions = listOf(FILM_ROLL_STATUS_ACTIVE, FILM_ROLL_STATUS_FINISHED)

private val filmFrameTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun defaultFilmRollStartedOnText(date: LocalDate = LocalDate.now()): String = date.toString()

fun defaultFilmFrameCapturedAtText(dateTime: LocalDateTime = LocalDateTime.now()): String {
    return dateTime.format(filmFrameTimestampFormatter)
}
