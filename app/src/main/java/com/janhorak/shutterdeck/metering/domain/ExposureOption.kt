package com.janhorak.shutterdeck.metering.domain

/** Shared label/value pair for metering option catalogs that both domain and presentation use. */
data class ExposureOption<T : Number>(
    val value: T,
    val label: String,
)
