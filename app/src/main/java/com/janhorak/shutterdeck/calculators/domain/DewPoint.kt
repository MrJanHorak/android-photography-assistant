package com.janhorak.shutterdeck.calculators.domain

import kotlin.math.ln

private const val WATER_MAGNUS_A = 17.62
private const val WATER_MAGNUS_B = 243.12
private const val ICE_MAGNUS_A = 22.46
private const val ICE_MAGNUS_B = 272.62
private const val WARNING_MARGIN_CELSIUS = 2.0

enum class CondensationRisk {
    ACTIVE,
    WARNING,
    LOW,
}

data class DewPointAnalysis(
    val airTemperatureCelsius: Double,
    val relativeHumidityPercent: Double,
    val surfaceTemperatureCelsius: Double,
    val dewPointCelsius: Double,
    val surfaceMarginCelsius: Double,
    val risk: CondensationRisk,
)

fun dewPointCelsius(
    airTemperatureCelsius: Double,
    relativeHumidityPercent: Double,
): Double? {
    if (!airTemperatureCelsius.isFinite() || !relativeHumidityPercent.isFinite()) return null
    if (relativeHumidityPercent <= 0.0 || relativeHumidityPercent > 100.0) return null

    val constants = magnusConstantsFor(airTemperatureCelsius)
    val gamma = ln(relativeHumidityPercent / 100.0) +
        (constants.a * airTemperatureCelsius / (constants.b + airTemperatureCelsius))
    val dewPoint = constants.b * gamma / (constants.a - gamma)
    return dewPoint.takeIf(Double::isFinite)
}

fun analyzeCondensationRiskFromCelsius(
    airTemperatureCelsius: Double,
    relativeHumidityPercent: Double,
    surfaceTemperatureCelsius: Double,
): DewPointAnalysis? {
    if (!surfaceTemperatureCelsius.isFinite()) return null
    val dewPointCelsius = dewPointCelsius(
        airTemperatureCelsius = airTemperatureCelsius,
        relativeHumidityPercent = relativeHumidityPercent,
    ) ?: return null
    val surfaceMarginCelsius = surfaceTemperatureCelsius - dewPointCelsius
    return DewPointAnalysis(
        airTemperatureCelsius = airTemperatureCelsius,
        relativeHumidityPercent = relativeHumidityPercent,
        surfaceTemperatureCelsius = surfaceTemperatureCelsius,
        dewPointCelsius = dewPointCelsius,
        surfaceMarginCelsius = surfaceMarginCelsius,
        risk = classifyCondensationRisk(surfaceMarginCelsius),
    )
}

fun analyzeCondensationRiskFromFahrenheit(
    airTemperatureFahrenheit: Double,
    relativeHumidityPercent: Double,
    surfaceTemperatureFahrenheit: Double,
): DewPointAnalysis? {
    val airTemperatureCelsius = celsiusFromFahrenheit(airTemperatureFahrenheit) ?: return null
    val surfaceTemperatureCelsius = celsiusFromFahrenheit(surfaceTemperatureFahrenheit) ?: return null
    return analyzeCondensationRiskFromCelsius(
        airTemperatureCelsius = airTemperatureCelsius,
        relativeHumidityPercent = relativeHumidityPercent,
        surfaceTemperatureCelsius = surfaceTemperatureCelsius,
    )
}

private data class MagnusConstants(
    val a: Double,
    val b: Double,
)

private fun magnusConstantsFor(airTemperatureCelsius: Double): MagnusConstants = when {
    airTemperatureCelsius < 0.0 -> MagnusConstants(a = ICE_MAGNUS_A, b = ICE_MAGNUS_B)
    else -> MagnusConstants(a = WATER_MAGNUS_A, b = WATER_MAGNUS_B)
}

private fun classifyCondensationRisk(surfaceMarginCelsius: Double): CondensationRisk = when {
    surfaceMarginCelsius <= 0.0 -> CondensationRisk.ACTIVE
    surfaceMarginCelsius <= WARNING_MARGIN_CELSIUS -> CondensationRisk.WARNING
    else -> CondensationRisk.LOW
}
