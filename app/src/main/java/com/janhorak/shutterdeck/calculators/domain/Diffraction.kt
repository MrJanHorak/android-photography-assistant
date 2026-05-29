package com.janhorak.shutterdeck.calculators.domain

/**
 * Pure, Android-free diffraction math. The Airy disk diameter grows with aperture;
 * once it exceeds roughly two pixel pitches, diffraction begins to limit resolution.
 */

const val GREEN_LIGHT_WAVELENGTH_NM: Double = 550.0

/** Airy disk diameter (microns) for an [aperture] at [wavelengthNm]: d = 2.44 * λ * N. */
fun airyDiskDiameterMicrons(aperture: Double, wavelengthNm: Double = GREEN_LIGHT_WAVELENGTH_NM): Double? {
    if (!aperture.isFinite() || !wavelengthNm.isFinite()) return null
    if (aperture <= 0 || wavelengthNm <= 0) return null
    return 2.44 * (wavelengthNm / 1000.0) * aperture
}

/**
 * Diffraction-limited aperture: the f-number at which the Airy disk diameter reaches
 * two pixel pitches (Nyquist). Stopping down beyond this softens fine detail.
 */
fun diffractionLimitedAperture(
    pixelPitchMicrons: Double,
    wavelengthNm: Double = GREEN_LIGHT_WAVELENGTH_NM,
): Double? {
    if (!pixelPitchMicrons.isFinite() || !wavelengthNm.isFinite()) return null
    if (pixelPitchMicrons <= 0 || wavelengthNm <= 0) return null
    return (2.0 * pixelPitchMicrons) / (2.44 * (wavelengthNm / 1000.0))
}
