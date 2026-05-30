package com.janhorak.shutterdeck.film.domain

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

enum class PushPullDirection {
    BOX_SPEED,
    PUSH,
    PULL,
}

enum class PushPullLatitudeStatus {
    WITHIN_SAVED_RANGE,
    OUTSIDE_SAVED_RANGE,
    UNKNOWN,
}

data class PushPullGuidance(
    val baseIso: Int,
    val targetExposureIndex: Int,
    val rawStopDelta: Double,
    val roundedStopDelta: Double,
    val direction: PushPullDirection,
    val isExactBoxSpeed: Boolean,
    val adjustmentLabel: String,
    val exposureSummary: String,
    val processingSummary: String,
    val latitudeStatus: PushPullLatitudeStatus,
    val latitudeSummary: String,
)

fun evaluatePushPull(
    baseIso: Int,
    targetExposureIndex: Int,
    maxPushStops: Int?,
    maxPullStops: Int?,
): PushPullGuidance? {
    if (
        baseIso <= 0 ||
        targetExposureIndex <= 0 ||
        maxPushStops?.let { value -> value < 0 } == true ||
        maxPullStops?.let { value -> value < 0 } == true
    ) {
        return null
    }

    val rawStopDelta = log2(targetExposureIndex.toDouble() / baseIso.toDouble())
    val roundedStopDelta = roundToNearestThirdStop(rawStopDelta)
    val isExactBoxSpeed = targetExposureIndex == baseIso

    val direction = when {
        roundedStopDelta > 0.0 -> PushPullDirection.PUSH
        roundedStopDelta < 0.0 -> PushPullDirection.PULL
        else -> PushPullDirection.BOX_SPEED
    }

    val formattedRoundedStops = formatStopAdjustment(roundedStopDelta)
    val adjustmentLabel = when {
        isExactBoxSpeed -> "Box speed"
        direction == PushPullDirection.BOX_SPEED -> "~Box speed"
        direction == PushPullDirection.PUSH -> "+$formattedRoundedStops push"
        else -> "-$formattedRoundedStops pull"
    }

    val exposureSummary = when {
        isExactBoxSpeed -> "Expose and meter this stock at its box speed of ISO $baseIso."
        direction == PushPullDirection.BOX_SPEED -> "EI $targetExposureIndex is under 1/3 stop from ISO $baseIso, so it is effectively a box-speed exposure."
        direction == PushPullDirection.PUSH -> "Expose ISO $baseIso film at EI $targetExposureIndex and label it clearly for push processing."
        else -> "Expose ISO $baseIso film at EI $targetExposureIndex and plan a pull workflow for the lower effective speed."
    }

    val processingSummary = when {
        isExactBoxSpeed -> "Normal development is appropriate at box speed."
        direction == PushPullDirection.BOX_SPEED -> "The requested EI is very close to box speed, so normal development is usually appropriate."
        direction == PushPullDirection.PUSH -> "Plan roughly +$formattedRoundedStops of push development to support the higher EI."
        else -> "Plan roughly -$formattedRoundedStops of pull development to protect highlights at the lower EI."
    }

    val latitudeStatus = when (direction) {
        PushPullDirection.BOX_SPEED -> PushPullLatitudeStatus.WITHIN_SAVED_RANGE
        PushPullDirection.PUSH -> when {
            maxPushStops == null -> PushPullLatitudeStatus.UNKNOWN
            rawStopDelta <= maxPushStops.toDouble() -> PushPullLatitudeStatus.WITHIN_SAVED_RANGE
            else -> PushPullLatitudeStatus.OUTSIDE_SAVED_RANGE
        }

        PushPullDirection.PULL -> when {
            maxPullStops == null -> PushPullLatitudeStatus.UNKNOWN
            abs(rawStopDelta) <= maxPullStops.toDouble() -> PushPullLatitudeStatus.WITHIN_SAVED_RANGE
            else -> PushPullLatitudeStatus.OUTSIDE_SAVED_RANGE
        }
    }

    val latitudeSummary = when {
        isExactBoxSpeed -> "Saved push/pull latitude is not needed at box speed."
        direction == PushPullDirection.BOX_SPEED -> "The EI shift is less than 1/3 stop, so saved push/pull latitude is not critical."
        direction == PushPullDirection.PUSH && latitudeStatus == PushPullLatitudeStatus.WITHIN_SAVED_RANGE ->
            "Inside the stock's saved +${formatWholeStops(maxPushStops)} push latitude."

        direction == PushPullDirection.PUSH && latitudeStatus == PushPullLatitudeStatus.OUTSIDE_SAVED_RANGE ->
            "Beyond the stock's saved +${formatWholeStops(maxPushStops)} push latitude."

        direction == PushPullDirection.PUSH ->
            "No saved push latitude is available for this stock."

        direction == PushPullDirection.PULL && latitudeStatus == PushPullLatitudeStatus.WITHIN_SAVED_RANGE ->
            "Inside the stock's saved -${formatWholeStops(maxPullStops)} pull latitude."

        direction == PushPullDirection.PULL && latitudeStatus == PushPullLatitudeStatus.OUTSIDE_SAVED_RANGE ->
            "Beyond the stock's saved -${formatWholeStops(maxPullStops)} pull latitude."

        else -> "No saved pull latitude is available for this stock."
    }

    return PushPullGuidance(
        baseIso = baseIso,
        targetExposureIndex = targetExposureIndex,
        rawStopDelta = rawStopDelta,
        roundedStopDelta = roundedStopDelta,
        direction = direction,
        isExactBoxSpeed = isExactBoxSpeed,
        adjustmentLabel = adjustmentLabel,
        exposureSummary = exposureSummary,
        processingSummary = processingSummary,
        latitudeStatus = latitudeStatus,
        latitudeSummary = latitudeSummary,
    )
}

fun buildPushPullNote(
    stockDisplayName: String,
    rollDisplayTitle: String?,
    processingType: String,
    guidance: PushPullGuidance,
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
        add("Exposure plan: ISO ${guidance.baseIso} -> EI ${guidance.targetExposureIndex}")
        add("Adjustment: ${guidance.adjustmentLabel}")
        add("Exposure: ${guidance.exposureSummary}")
        add("Processing: ${guidance.processingSummary}")
        add("Saved latitude: ${guidance.latitudeSummary}")
        developerNotes.trim().takeIf { value -> value.isNotBlank() }?.let { notes ->
            add("Stock notes: $notes")
        }
        sessionNotes.trim().takeIf { value -> value.isNotBlank() }?.let { notes ->
            add("Session notes: $notes")
        }
    }
    return sections.joinToString(separator = "\n")
}

fun formatStopAdjustment(stopDelta: Double): String {
    val roundedThirds = (abs(roundToNearestThirdStop(stopDelta)) * 3).roundToInt()
    if (roundedThirds == 0) {
        return "0"
    }
    val wholeStops = roundedThirds / 3
    val thirdStops = roundedThirds % 3
    val fractionText = when (thirdStops) {
        1 -> "1/3"
        2 -> "2/3"
        else -> ""
    }
    return when {
        wholeStops == 0 -> fractionText
        fractionText.isEmpty() -> wholeStops.toString()
        else -> "$wholeStops $fractionText"
    }
}

private fun roundToNearestThirdStop(value: Double): Double = ((value * 3.0).roundToInt()) / 3.0

private fun formatWholeStops(stops: Int?): String = when (stops) {
    null -> "?"
    1 -> "1 stop"
    else -> "$stops stops"
}
