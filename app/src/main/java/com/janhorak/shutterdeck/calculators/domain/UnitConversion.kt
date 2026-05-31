package com.janhorak.shutterdeck.calculators.domain

const val METERS_PER_FOOT = 0.3048

data class DistanceConversion(
    val meters: Double,
    val feet: Double,
)

data class TemperatureConversion(
    val celsius: Double,
    val fahrenheit: Double,
)

fun feetFromMeters(meters: Double): Double? {
    if (!meters.isFinite() || meters < 0.0) return null
    return (meters / METERS_PER_FOOT).takeIf { it.isFinite() }
}

fun metersFromFeet(feet: Double): Double? {
    if (!feet.isFinite() || feet < 0.0) return null
    return (feet * METERS_PER_FOOT).takeIf { it.isFinite() }
}

fun fahrenheitFromCelsius(celsius: Double): Double? {
    if (!celsius.isFinite()) return null
    return ((celsius * 9.0 / 5.0) + 32.0).takeIf { it.isFinite() }
}

fun celsiusFromFahrenheit(fahrenheit: Double): Double? {
    if (!fahrenheit.isFinite()) return null
    return (((fahrenheit - 32.0) * 5.0) / 9.0).takeIf { it.isFinite() }
}

fun convertDistanceFromMeters(meters: Double): DistanceConversion? {
    val feet = feetFromMeters(meters) ?: return null
    return DistanceConversion(
        meters = meters,
        feet = feet,
    )
}

fun convertDistanceFromFeet(feet: Double): DistanceConversion? {
    val meters = metersFromFeet(feet) ?: return null
    return DistanceConversion(
        meters = meters,
        feet = feet,
    )
}

fun convertTemperatureFromCelsius(celsius: Double): TemperatureConversion? {
    val fahrenheit = fahrenheitFromCelsius(celsius) ?: return null
    return TemperatureConversion(
        celsius = celsius,
        fahrenheit = fahrenheit,
    )
}

fun convertTemperatureFromFahrenheit(fahrenheit: Double): TemperatureConversion? {
    val celsius = celsiusFromFahrenheit(fahrenheit) ?: return null
    return TemperatureConversion(
        celsius = celsius,
        fahrenheit = fahrenheit,
    )
}
