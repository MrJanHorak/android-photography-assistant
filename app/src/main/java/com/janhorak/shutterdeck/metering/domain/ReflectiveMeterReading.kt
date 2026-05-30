package com.janhorak.shutterdeck.metering.domain

/** Pure reflective meter sample from camera auto exposure. */
data class ReflectiveMeterReading(
    val aperture: Float,
    val shutterSeconds: Double,
    val iso: Int,
    val ev100: Float,
)
