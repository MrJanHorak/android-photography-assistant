package com.janhorak.shutterdeck.metering.domain

/** Shared label/value pair for shutter-speed catalogs used by both domain and presentation. */
data class ShutterOption(
    val seconds: Double,
    val label: String,
)
