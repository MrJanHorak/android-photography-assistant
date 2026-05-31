package com.janhorak.shutterdeck.utilities.domain

import kotlin.math.ceil

private const val MEGABYTES_PER_GIGABYTE = 1024.0

data class IntervalExposureCheck(
    val exposureSeconds: Double,
    val slackSeconds: Double,
    val overrunsInterval: Boolean,
)

data class IntervalometerPlan(
    val captureWindowSeconds: Long,
    val clipLengthSeconds: Double,
    val framesPerMinute: Double,
    val storageRequiredMegabytes: Double?,
    val storageRequiredGigabytes: Double?,
    val cardsNeeded: Int?,
    val batteriesNeeded: Int?,
    val exposureCheck: IntervalExposureCheck?,
)

fun buildIntervalometerPlan(
    intervalSeconds: Int,
    frameCount: Int,
    playbackFramesPerSecond: Int,
    averageFrameSizeMegabytes: Double? = null,
    cardCapacityGigabytes: Double? = null,
    shotsPerBattery: Int? = null,
    exposureSeconds: Double? = null,
): IntervalometerPlan? {
    if (intervalSeconds <= 0 || frameCount <= 0 || playbackFramesPerSecond <= 0) return null
    if (averageFrameSizeMegabytes != null && averageFrameSizeMegabytes <= 0.0) return null
    if (cardCapacityGigabytes != null && cardCapacityGigabytes <= 0.0) return null
    if (shotsPerBattery != null && shotsPerBattery <= 0) return null
    if (exposureSeconds != null && exposureSeconds <= 0.0) return null

    val captureWindowSeconds = intervalSeconds.toLong() * (frameCount - 1).toLong()
    val clipLengthSeconds = frameCount.toDouble() / playbackFramesPerSecond.toDouble()
    val framesPerMinute = 60.0 / intervalSeconds.toDouble()
    val storageRequiredMegabytes = averageFrameSizeMegabytes?.let { frameCount.toDouble() * it }
    val storageRequiredGigabytes = storageRequiredMegabytes?.div(MEGABYTES_PER_GIGABYTE)
    val cardsNeeded = if (storageRequiredGigabytes != null && cardCapacityGigabytes != null) {
        ceil(storageRequiredGigabytes / cardCapacityGigabytes).toInt().coerceAtLeast(1)
    } else {
        null
    }
    val batteriesNeeded = shotsPerBattery?.let {
        ceil(frameCount.toDouble() / it.toDouble()).toInt().coerceAtLeast(1)
    }
    val exposureCheck = exposureSeconds?.let {
        val slackSeconds = intervalSeconds.toDouble() - it
        IntervalExposureCheck(
            exposureSeconds = it,
            slackSeconds = slackSeconds,
            overrunsInterval = slackSeconds <= 0.0,
        )
    }

    return IntervalometerPlan(
        captureWindowSeconds = captureWindowSeconds,
        clipLengthSeconds = clipLengthSeconds,
        framesPerMinute = framesPerMinute,
        storageRequiredMegabytes = storageRequiredMegabytes,
        storageRequiredGigabytes = storageRequiredGigabytes,
        cardsNeeded = cardsNeeded,
        batteriesNeeded = batteriesNeeded,
        exposureCheck = exposureCheck,
    )
}
