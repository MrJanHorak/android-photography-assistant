package com.janhorak.shutterdeck.film.domain

import kotlin.math.roundToInt

data class DilutionCalculation(
    val stockMilliliters: Double,
    val waterMilliliters: Double,
    val totalMilliliters: Double,
)

data class FilmDevelopmentStep(
    val name: String,
    val durationSeconds: Int,
    val agitationIntervalSeconds: Int = 0,
    val note: String? = null,
)

private val temperatureCompensationFactors =
    listOf(
        16.0 to 1.50,
        17.0 to 1.35,
        18.0 to 1.20,
        19.0 to 1.10,
        20.0 to 1.00,
        21.0 to 0.90,
        22.0 to 0.80,
        23.0 to 0.71,
        24.0 to 0.63,
        25.0 to 0.56,
        26.0 to 0.50,
    )

fun calculateDilution(
    waterParts: Double,
    totalMilliliters: Double,
    stockParts: Double = 1.0,
): DilutionCalculation? {
    if (stockParts <= 0.0 || waterParts <= 0.0 || totalMilliliters <= 0.0) {
        return null
    }
    val totalParts = stockParts + waterParts
    val stockMilliliters = totalMilliliters * (stockParts / totalParts)
    val waterMilliliters = totalMilliliters - stockMilliliters
    return DilutionCalculation(
        stockMilliliters = stockMilliliters,
        waterMilliliters = waterMilliliters,
        totalMilliliters = totalMilliliters,
    )
}

fun developmentTemperatureFactor(chemistryTemperatureC: Double): Double? {
    val minimum = temperatureCompensationFactors.first().first
    val maximum = temperatureCompensationFactors.last().first
    if (chemistryTemperatureC < minimum || chemistryTemperatureC > maximum) {
        return null
    }
    temperatureCompensationFactors.firstOrNull { (temperature, _) ->
        temperature == chemistryTemperatureC
    }?.let { (_, factor) ->
        return factor
    }

    val upperIndex = temperatureCompensationFactors.indexOfFirst { (temperature, _) ->
        temperature > chemistryTemperatureC
    }
    val (lowerTemperature, lowerFactor) = temperatureCompensationFactors[upperIndex - 1]
    val (upperTemperature, upperFactor) = temperatureCompensationFactors[upperIndex]
    val ratio = (chemistryTemperatureC - lowerTemperature) / (upperTemperature - lowerTemperature)
    return lowerFactor + ((upperFactor - lowerFactor) * ratio)
}

fun adjustDevelopmentTimeSeconds(
    baseTimeAt20CSeconds: Int,
    chemistryTemperatureC: Double,
): Int? {
    if (baseTimeAt20CSeconds <= 0) {
        return null
    }
    val factor = developmentTemperatureFactor(chemistryTemperatureC) ?: return null
    return (baseTimeAt20CSeconds * factor).roundToInt().coerceAtLeast(1)
}

fun buildAgitationCueOffsets(
    durationSeconds: Int,
    agitationIntervalSeconds: Int,
): List<Int> {
    if (durationSeconds <= 0 || agitationIntervalSeconds <= 0) {
        return emptyList()
    }
    return generateSequence(agitationIntervalSeconds) { previous ->
        previous + agitationIntervalSeconds
    }.takeWhile { elapsedSeconds ->
        elapsedSeconds < durationSeconds
    }.toList()
}

fun buildDevelopmentRecipeSteps(
    preSoakSeconds: Int,
    developerBaseSecondsAt20C: Int,
    chemistryTemperatureC: Double,
    stopBathSeconds: Int,
    fixerSeconds: Int,
    washSeconds: Int,
    agitationIntervalSeconds: Int,
): List<FilmDevelopmentStep>? {
    if (
        preSoakSeconds < 0 ||
        developerBaseSecondsAt20C <= 0 ||
        stopBathSeconds < 0 ||
        fixerSeconds < 0 ||
        washSeconds < 0 ||
        agitationIntervalSeconds < 0
    ) {
        return null
    }

    val adjustedDeveloperSeconds = adjustDevelopmentTimeSeconds(
        baseTimeAt20CSeconds = developerBaseSecondsAt20C,
        chemistryTemperatureC = chemistryTemperatureC,
    ) ?: return null

    val recurringAgitationNote = if (agitationIntervalSeconds > 0) {
        "Agitate for the first 10 seconds, then every ${agitationIntervalSeconds}s."
    } else {
        "Stand development: no recurring agitation cues."
    }

    return buildList {
        if (preSoakSeconds > 0) {
            add(
                FilmDevelopmentStep(
                    name = "Pre-soak",
                    durationSeconds = preSoakSeconds,
                    note = "Bring the tank to process temperature before developer.",
                ),
            )
        }
        add(
            FilmDevelopmentStep(
                name = "Developer",
                durationSeconds = adjustedDeveloperSeconds,
                agitationIntervalSeconds = agitationIntervalSeconds,
                note = recurringAgitationNote,
            ),
        )
        if (stopBathSeconds > 0) {
            add(
                FilmDevelopmentStep(
                    name = "Stop bath",
                    durationSeconds = stopBathSeconds,
                    note = "Continuous agitation is typical for this step.",
                ),
            )
        }
        if (fixerSeconds > 0) {
            add(
                FilmDevelopmentStep(
                    name = "Fixer",
                    durationSeconds = fixerSeconds,
                    agitationIntervalSeconds = agitationIntervalSeconds,
                    note = if (agitationIntervalSeconds > 0) {
                        "Agitate briefly on each cue."
                    } else {
                        "Use your normal fixing agitation rhythm."
                    },
                ),
            )
        }
        if (washSeconds > 0) {
            add(
                FilmDevelopmentStep(
                    name = "Wash",
                    durationSeconds = washSeconds,
                    agitationIntervalSeconds = agitationIntervalSeconds,
                    note = if (agitationIntervalSeconds > 0) {
                        "Invert or refresh water on each cue."
                    } else {
                        "Use your normal wash sequence."
                    },
                ),
            )
        }
    }
}
