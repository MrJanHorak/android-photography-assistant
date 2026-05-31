package com.janhorak.shutterdeck.calculators.domain

const val MIRED_RECIPROCAL = 1_000_000.0

data class ColorTemperatureConversion(
    val kelvin: Double,
    val mired: Double,
)

fun miredFromKelvin(kelvin: Double): Double? {
    if (!kelvin.isFinite() || kelvin <= 0.0) return null
    return (MIRED_RECIPROCAL / kelvin).takeIf { it.isFinite() && it > 0.0 }
}

fun kelvinFromMired(mired: Double): Double? {
    if (!mired.isFinite() || mired <= 0.0) return null
    return (MIRED_RECIPROCAL / mired).takeIf { it.isFinite() && it > 0.0 }
}

fun convertColorTemperatureFromKelvin(kelvin: Double): ColorTemperatureConversion? {
    val mired = miredFromKelvin(kelvin) ?: return null
    return ColorTemperatureConversion(
        kelvin = kelvin,
        mired = mired,
    )
}

fun convertColorTemperatureFromMired(mired: Double): ColorTemperatureConversion? {
    val kelvin = kelvinFromMired(mired) ?: return null
    return ColorTemperatureConversion(
        kelvin = kelvin,
        mired = mired,
    )
}
