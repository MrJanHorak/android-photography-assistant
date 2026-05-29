package com.janhorak.shutterdeck.metering.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Pure formatting helpers for exposure values. No Android imports. */

fun formatDuration(seconds: Double): String {
    val rounded = seconds.roundToInt()
    val minutes = rounded / 60
    val remainderSeconds = rounded % 60
    return when {
        rounded < 60 -> String.format(Locale.getDefault(), "%d s", rounded)
        remainderSeconds == 0 -> String.format(Locale.getDefault(), "%d min", minutes)
        else -> String.format(Locale.getDefault(), "%d min %d s", minutes, remainderSeconds)
    }
}

fun formatExposureTime(seconds: Double): String {
    if (seconds <= 0.0) return "--"
    if (seconds >= 1.0) {
        return if (seconds >= 10.0 || abs(seconds - seconds.roundToInt()) < 0.05) {
            String.format(Locale.getDefault(), "%d s", seconds.roundToInt())
        } else {
            String.format(Locale.getDefault(), "%.1f s", seconds)
        }
    }
    val denominator = (1.0 / seconds).roundToInt().coerceAtLeast(1)
    return "1/$denominator s"
}

fun formatDecimal(value: Double): String {
    return if (abs(value - value.roundToInt()) < 0.05) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

fun formatStopCount(stops: Double): String {
    val value = String.format(Locale.getDefault(), "%.1f", stops)
    return if (abs(stops - 1.0) < 0.05) "$value stop" else "$value stops"
}
