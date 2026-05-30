package com.janhorak.shutterdeck.film.domain

import com.janhorak.shutterdeck.calculators.domain.reciprocityCompensationStops
import com.janhorak.shutterdeck.calculators.domain.reciprocityCorrectedSeconds
import com.janhorak.shutterdeck.metering.domain.formatExposureTime
import com.janhorak.shutterdeck.metering.domain.formatStopCount
import java.util.Locale

enum class ReciprocityStatus {
    NO_CURVE_SAVED,
    BELOW_ONSET,
    CORRECTED,
}

data class ReciprocityGuidance(
    val meteredSeconds: Double,
    val correctedSeconds: Double,
    val addedSeconds: Double,
    val compensationStops: Double,
    val exponent: Double?,
    val onsetSeconds: Double?,
    val status: ReciprocityStatus,
    val timingSummary: String,
    val curveSummary: String,
)

fun evaluateFilmReciprocity(
    meteredSeconds: Double,
    exponent: Double?,
    onsetSeconds: Double?,
): ReciprocityGuidance? {
    if (!meteredSeconds.isFinite() || meteredSeconds <= 0.0) {
        return null
    }
    if (exponent != null && (!exponent.isFinite() || exponent <= 0.0)) {
        return null
    }
    if (onsetSeconds != null && (!onsetSeconds.isFinite() || onsetSeconds <= 0.0)) {
        return null
    }

    if (exponent == null || onsetSeconds == null) {
        return ReciprocityGuidance(
            meteredSeconds = meteredSeconds,
            correctedSeconds = meteredSeconds,
            addedSeconds = 0.0,
            compensationStops = 0.0,
            exponent = null,
            onsetSeconds = null,
            status = ReciprocityStatus.NO_CURVE_SAVED,
            timingSummary = "No saved reciprocity curve is available for this stock, so start with the metered ${formatExposureTime(meteredSeconds)} exposure and rely on your own test data.",
            curveSummary = "Curve unavailable: this stock does not have a saved reciprocity exponent and onset threshold yet.",
        )
    }

    val correctedSeconds = reciprocityCorrectedSeconds(
        meteredSeconds = meteredSeconds,
        exponent = exponent,
        onsetSeconds = onsetSeconds,
    ) ?: return null
    val compensationStops = reciprocityCompensationStops(
        meteredSeconds = meteredSeconds,
        correctedSeconds = correctedSeconds,
    ) ?: return null
    val addedSeconds = correctedSeconds - meteredSeconds

    val status = if (meteredSeconds <= onsetSeconds) {
        ReciprocityStatus.BELOW_ONSET
    } else {
        ReciprocityStatus.CORRECTED
    }
    val timingSummary = when (status) {
        ReciprocityStatus.NO_CURVE_SAVED ->
            "No saved reciprocity curve is available for this stock."

        ReciprocityStatus.BELOW_ONSET ->
            "The metered ${formatExposureTime(meteredSeconds)} exposure is at or below the stock's reciprocity onset of ${formatExposureTime(onsetSeconds)}, so no correction is needed."

        ReciprocityStatus.CORRECTED ->
            "Increase the metered ${formatExposureTime(meteredSeconds)} exposure to ${formatExposureTime(correctedSeconds)} (${formatStopCount(compensationStops)} more light)."
    }
    val curveSummary = when (status) {
        ReciprocityStatus.NO_CURVE_SAVED ->
            "Curve unavailable."

        ReciprocityStatus.BELOW_ONSET, ReciprocityStatus.CORRECTED ->
            "Saved curve: t^${formatCompactExponent(exponent)} after ${formatExposureTime(onsetSeconds)}."
    }

    return ReciprocityGuidance(
        meteredSeconds = meteredSeconds,
        correctedSeconds = correctedSeconds,
        addedSeconds = addedSeconds,
        compensationStops = compensationStops,
        exponent = exponent,
        onsetSeconds = onsetSeconds,
        status = status,
        timingSummary = timingSummary,
        curveSummary = curveSummary,
    )
}

fun buildReciprocityNote(
    stockDisplayName: String,
    rollDisplayTitle: String?,
    processingType: String,
    guidance: ReciprocityGuidance,
    developerNotes: String,
    sessionNotes: String,
): String {
    val sections = buildList {
        add("Stock: ${stockDisplayName.trim().ifBlank { "Unknown stock" }}")
        rollDisplayTitle?.trim()?.takeIf { value -> value.isNotBlank() }?.let { title ->
            add("Roll: $title")
        }
        processingType.trim().takeIf { value -> value.isNotBlank() }?.let { type ->
            add("Process: $type")
        }
        add("Metered time: ${formatExposureTime(guidance.meteredSeconds)}")
        add("Corrected time: ${formatExposureTime(guidance.correctedSeconds)}")
        add("Reciprocity: ${guidance.timingSummary}")
        add("Curve: ${guidance.curveSummary}")
        if (guidance.status == ReciprocityStatus.CORRECTED) {
            add("Added time: ${formatExposureTime(guidance.addedSeconds)} (${formatStopCount(guidance.compensationStops)})")
        }
        developerNotes.trim().takeIf { value -> value.isNotBlank() }?.let { notes ->
            add("Stock notes: $notes")
        }
        sessionNotes.trim().takeIf { value -> value.isNotBlank() }?.let { notes ->
            add("Session notes: $notes")
        }
    }
    return sections.joinToString(separator = "\n")
}

private fun formatCompactExponent(value: Double): String =
    String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
