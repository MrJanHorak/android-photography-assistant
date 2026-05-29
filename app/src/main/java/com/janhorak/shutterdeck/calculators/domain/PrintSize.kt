package com.janhorak.shutterdeck.calculators.domain

import kotlin.math.ceil

/**
 * Pure, Android-free print-size math: convert between pixel dimensions and physical
 * print size at a chosen DPI, and estimate a "good enough" DPI for a viewing distance.
 */

const val INCH_TO_CM: Double = 2.54

data class PrintSizeResult(
    val widthInches: Double,
    val heightInches: Double,
    val widthCm: Double,
    val heightCm: Double,
)

/** Physical print size for [pixelWidth] x [pixelHeight] printed at [dpi]. */
fun printSizeAtDpi(pixelWidth: Int, pixelHeight: Int, dpi: Double): PrintSizeResult? {
    if (!dpi.isFinite() || pixelWidth <= 0 || pixelHeight <= 0 || dpi <= 0) return null
    val widthInches = pixelWidth / dpi
    val heightInches = pixelHeight / dpi
    return PrintSizeResult(
        widthInches = widthInches,
        heightInches = heightInches,
        widthCm = widthInches * INCH_TO_CM,
        heightCm = heightInches * INCH_TO_CM,
    )
}

/** Pixel dimensions required to print [widthInches] x [heightInches] at [dpi]. */
fun requiredPixelsForPrint(widthInches: Double, heightInches: Double, dpi: Double): Pair<Int, Int>? {
    if (!widthInches.isFinite() || !heightInches.isFinite() || !dpi.isFinite()) return null
    if (widthInches <= 0 || heightInches <= 0 || dpi <= 0) return null
    val width = ceil(widthInches * dpi)
    val height = ceil(heightInches * dpi)
    if (width > Int.MAX_VALUE.toDouble() || height > Int.MAX_VALUE.toDouble()) return null
    return width.toInt() to height.toInt()
}

/** Megapixels for a pixel dimension pair. */
fun megapixels(pixelWidth: Int, pixelHeight: Int): Double =
    pixelWidth.toDouble() * pixelHeight.toDouble() / 1_000_000.0

/**
 * "Good enough" DPI for a print viewed at [viewingDistanceInches], based on the
 * 1-arcminute acuity limit (~3438 / distance). Closer viewing demands higher DPI.
 */
fun goodEnoughDpi(viewingDistanceInches: Double): Double? {
    if (!viewingDistanceInches.isFinite() || viewingDistanceInches <= 0) return null
    return 3438.0 / viewingDistanceInches
}
