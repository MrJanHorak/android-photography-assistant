package com.janhorak.shutterdeck.calculators.domain

import kotlin.math.atan
import kotlin.math.sqrt

/**
 * Pure, Android-free field-of-view math based on a full-frame 36x24mm reference.
 * Smaller sensors are modelled by dividing the reference dimensions by the crop factor.
 */

const val FULL_FRAME_WIDTH_MM: Double = 36.0
const val FULL_FRAME_HEIGHT_MM: Double = 24.0

data class FieldOfViewResult(
    val horizontalDeg: Double,
    val verticalDeg: Double,
    val diagonalDeg: Double,
    val equivalentFocalLengthMm: Double,
    val sensorWidthMm: Double,
    val sensorHeightMm: Double,
)

/** Angle of view (degrees) for a sensor dimension (mm) at a focal length (mm). */
fun angleOfViewDegrees(focalLengthMm: Double, sensorDimensionMm: Double): Double? {
    if (!focalLengthMm.isFinite() || !sensorDimensionMm.isFinite()) return null
    if (focalLengthMm <= 0 || sensorDimensionMm <= 0) return null
    return Math.toDegrees(2.0 * atan(sensorDimensionMm / (2.0 * focalLengthMm)))
}

/** 35mm-equivalent focal length for a given crop factor. */
fun equivalentFocalLengthMm(focalLengthMm: Double, cropFactor: Double): Double =
    focalLengthMm * cropFactor

/**
 * Horizontal, vertical and diagonal angles of view plus the 35mm-equivalent focal
 * length for a lens on a sensor described by [cropFactor]. Returns null on bad input.
 */
fun calculateFieldOfView(focalLengthMm: Double, cropFactor: Double): FieldOfViewResult? {
    if (!focalLengthMm.isFinite() || !cropFactor.isFinite()) return null
    if (focalLengthMm <= 0 || cropFactor <= 0) return null
    val width = FULL_FRAME_WIDTH_MM / cropFactor
    val height = FULL_FRAME_HEIGHT_MM / cropFactor
    val diagonal = sqrt(width * width + height * height)
    return FieldOfViewResult(
        horizontalDeg = angleOfViewDegrees(focalLengthMm, width)!!,
        verticalDeg = angleOfViewDegrees(focalLengthMm, height)!!,
        diagonalDeg = angleOfViewDegrees(focalLengthMm, diagonal)!!,
        equivalentFocalLengthMm = equivalentFocalLengthMm(focalLengthMm, cropFactor),
        sensorWidthMm = width,
        sensorHeightMm = height,
    )
}
