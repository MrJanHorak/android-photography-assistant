package com.janhorak.shutterdeck.metering.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

private const val STOP_TOLERANCE = 0.05

/**
 * WorkflowPriority entry names are persisted through SharedPreferences via `.name`.
 * Do not rename ISO_FIRST / APERTURE_FIRST / SHUTTER_FIRST without a migration.
 */
enum class WorkflowPriority(
    val label: String,
    val description: String,
) {
    ISO_FIRST(
        label = "ISO first",
        description = "Keep the chosen aperture as long as possible and raise ISO first when the shutter is too slow.",
    ),
    APERTURE_FIRST(
        label = "Aperture first",
        description = "Open the lens first, then raise ISO only if the shutter still needs help.",
    ),
    SHUTTER_FIRST(
        label = "Shutter first",
        description = "Set a safe shutter target first and let the app show what ISO or aperture that requires.",
    ),
}

fun buildExposureWorkflowSuggestion(
    workflowPriority: WorkflowPriority,
    measuredShutterSeconds: Double?,
    recommendedMinimumShutterSeconds: Double,
    currentAperture: Float,
    currentIso: Int,
    apertureOptions: List<ExposureOption<Float>>,
    isoOptions: List<ExposureOption<Int>>,
): String {
    val measuredSeconds = measuredShutterSeconds
        ?: return "Awaiting a metered shutter before workflow coaching can suggest what to change first."

    if (measuredSeconds <= recommendedMinimumShutterSeconds) {
        return "Current settings already meet the practical shutter target. Use ${workflowPriority.label.lowercase(Locale.getDefault())} only if you want more safety margin."
    }

    val requiredGainStops = log2(measuredSeconds / recommendedMinimumShutterSeconds)
    return when (workflowPriority) {
        WorkflowPriority.ISO_FIRST -> buildIsoFirstWorkflowSuggestion(
            requiredGainStops = requiredGainStops,
            recommendedMinimumShutterSeconds = recommendedMinimumShutterSeconds,
            currentAperture = currentAperture,
            currentIso = currentIso,
            apertureOptions = apertureOptions,
            isoOptions = isoOptions,
        )

        WorkflowPriority.APERTURE_FIRST -> buildApertureFirstWorkflowSuggestion(
            requiredGainStops = requiredGainStops,
            recommendedMinimumShutterSeconds = recommendedMinimumShutterSeconds,
            currentAperture = currentAperture,
            currentIso = currentIso,
            apertureOptions = apertureOptions,
            isoOptions = isoOptions,
        )

        WorkflowPriority.SHUTTER_FIRST -> buildShutterFirstWorkflowSuggestion(
            requiredGainStops = requiredGainStops,
            recommendedMinimumShutterSeconds = recommendedMinimumShutterSeconds,
            currentAperture = currentAperture,
            currentIso = currentIso,
            apertureOptions = apertureOptions,
            isoOptions = isoOptions,
        )
    }
}

fun <T : Number> nearestExposureIndex(
    options: List<ExposureOption<T>>,
    selectedValue: Double,
): Int {
    return options.indices.minByOrNull { index ->
        abs(log2(options[index].value.toDouble() / selectedValue))
    } ?: 0
}

private fun buildIsoFirstWorkflowSuggestion(
    requiredGainStops: Double,
    recommendedMinimumShutterSeconds: Double,
    currentAperture: Float,
    currentIso: Int,
    apertureOptions: List<ExposureOption<Float>>,
    isoOptions: List<ExposureOption<Int>>,
): String {
    val targetIsoOption = pickIsoOptionForGain(currentIso, requiredGainStops, isoOptions)
    val isoGainStops = log2(targetIsoOption.value.toDouble() / currentIso.toDouble())
    val remainingStops = (requiredGainStops - isoGainStops).coerceAtLeast(0.0)

    if (remainingStops <= STOP_TOLERANCE) {
        return "Raise ISO first to ${targetIsoOption.label} and keep f/${formatDecimal(currentAperture.toDouble())}. That should support ${formatExposureTime(recommendedMinimumShutterSeconds)}."
    }

    val targetApertureOption = pickApertureOptionForGain(currentAperture, remainingStops, apertureOptions)
    val apertureGainStops = apertureGainStops(currentAperture, targetApertureOption.value)
    val unresolvedStops = (remainingStops - apertureGainStops).coerceAtLeast(0.0)

    return if (unresolvedStops <= STOP_TOLERANCE) {
        "Raise ISO to ${targetIsoOption.label} first, then open to ${targetApertureOption.label} to support ${formatExposureTime(recommendedMinimumShutterSeconds)}."
    } else {
        "Even at ${targetIsoOption.label} and ${targetApertureOption.label}, you are still about ${formatStopCount(unresolvedStops)} short. Add light, accept blur, or add support."
    }
}

private fun buildApertureFirstWorkflowSuggestion(
    requiredGainStops: Double,
    recommendedMinimumShutterSeconds: Double,
    currentAperture: Float,
    currentIso: Int,
    apertureOptions: List<ExposureOption<Float>>,
    isoOptions: List<ExposureOption<Int>>,
): String {
    val targetApertureOption = pickApertureOptionForGain(currentAperture, requiredGainStops, apertureOptions)
    val apertureGainStops = apertureGainStops(currentAperture, targetApertureOption.value)
    val remainingStops = (requiredGainStops - apertureGainStops).coerceAtLeast(0.0)

    if (remainingStops <= STOP_TOLERANCE) {
        return "Open the lens first to ${targetApertureOption.label} and keep ISO $currentIso. That should support ${formatExposureTime(recommendedMinimumShutterSeconds)}."
    }

    val targetIsoOption = pickIsoOptionForGain(currentIso, remainingStops, isoOptions)
    val isoGainStops = log2(targetIsoOption.value.toDouble() / currentIso.toDouble())
    val unresolvedStops = (remainingStops - isoGainStops).coerceAtLeast(0.0)

    return if (unresolvedStops <= STOP_TOLERANCE) {
        "Open to ${targetApertureOption.label} first, then raise ISO to ${targetIsoOption.label} to support ${formatExposureTime(recommendedMinimumShutterSeconds)}."
    } else {
        "Even wide open at ${targetApertureOption.label} and ${targetIsoOption.label}, you are still about ${formatStopCount(unresolvedStops)} short. Add light or relax the shutter goal."
    }
}

private fun buildShutterFirstWorkflowSuggestion(
    requiredGainStops: Double,
    recommendedMinimumShutterSeconds: Double,
    currentAperture: Float,
    currentIso: Int,
    apertureOptions: List<ExposureOption<Float>>,
    isoOptions: List<ExposureOption<Int>>,
): String {
    val targetIsoOption = pickIsoOptionForGain(currentIso, requiredGainStops, isoOptions)
    val isoGainStops = log2(targetIsoOption.value.toDouble() / currentIso.toDouble())
    val remainingStops = (requiredGainStops - isoGainStops).coerceAtLeast(0.0)

    if (remainingStops <= STOP_TOLERANCE) {
        return "Dial ${formatExposureTime(recommendedMinimumShutterSeconds)} first. At f/${formatDecimal(currentAperture.toDouble())}, aim for ${targetIsoOption.label}."
    }

    val targetApertureOption = pickApertureOptionForGain(currentAperture, remainingStops, apertureOptions)
    val apertureGainStops = apertureGainStops(currentAperture, targetApertureOption.value)
    val unresolvedStops = (remainingStops - apertureGainStops).coerceAtLeast(0.0)

    return if (unresolvedStops <= STOP_TOLERANCE) {
        "Dial ${formatExposureTime(recommendedMinimumShutterSeconds)} first. At ${targetApertureOption.label}, ${targetIsoOption.label} should cover it."
    } else {
        "Dial ${formatExposureTime(recommendedMinimumShutterSeconds)} first. Even at ${targetApertureOption.label} and ${targetIsoOption.label}, you are still about ${formatStopCount(unresolvedStops)} short."
    }
}

private fun pickIsoOptionForGain(
    currentIso: Int,
    requiredGainStops: Double,
    isoOptions: List<ExposureOption<Int>>,
): ExposureOption<Int> {
    requireAscendingOptions(isoOptions)
    val targetIso = currentIso * 2.0.pow(requiredGainStops)
    return isoOptions.firstOrNull { option -> option.value >= targetIso.roundToInt() } ?: isoOptions.last()
}

private fun pickApertureOptionForGain(
    currentAperture: Float,
    requiredGainStops: Double,
    apertureOptions: List<ExposureOption<Float>>,
): ExposureOption<Float> {
    requireAscendingOptions(apertureOptions)
    val candidates = apertureOptions.filter { option -> option.value <= currentAperture + 0.01f }
    require(candidates.isNotEmpty()) {
        "Aperture options must include a value at or wider than the current aperture."
    }
    return candidates
        .filter { option -> apertureGainStops(currentAperture, option.value) + STOP_TOLERANCE >= requiredGainStops }
        .maxByOrNull { option -> option.value }
        ?: candidates.first()
}

private fun <T : Number> requireAscendingOptions(options: List<ExposureOption<T>>) {
    require(options.isNotEmpty()) { "Exposure options must not be empty." }
    require(options.zipWithNext().all { (left, right) -> left.value.toDouble() <= right.value.toDouble() }) {
        "Exposure options must be sorted in ascending value order."
    }
}
