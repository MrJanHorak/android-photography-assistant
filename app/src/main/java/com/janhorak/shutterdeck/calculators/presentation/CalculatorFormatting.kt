package com.janhorak.shutterdeck.calculators.presentation

import java.util.Locale

/** Shared presentation-layer formatting for calculator results. */

private const val INFINITY = "∞"

fun formatMeters(meters: Double): String =
    if (meters.isInfinite()) INFINITY else String.format(Locale.getDefault(), "%.2f m", meters)

fun formatDistanceSmart(meters: Double): String = when {
    meters.isInfinite() -> INFINITY
    meters < 1.0 -> String.format(Locale.getDefault(), "%.0f cm", meters * 100.0)
    meters < 10.0 -> String.format(Locale.getDefault(), "%.2f m", meters)
    else -> String.format(Locale.getDefault(), "%.1f m", meters)
}

fun formatDegrees(degrees: Double): String =
    String.format(Locale.getDefault(), "%.1f°", degrees)

fun formatOneDecimal(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)

fun formatInches(inches: Double): String =
    String.format(Locale.getDefault(), "%.2f in", inches)

fun formatCm(cm: Double): String =
    String.format(Locale.getDefault(), "%.1f cm", cm)
