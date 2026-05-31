package com.janhorak.shutterdeck.metering.domain

import kotlin.math.log2
import kotlin.math.pow

internal const val EV100_ZERO_LUX = 2.5
const val LUX_PER_FOOT_CANDLE = 10.7639

data class IlluminanceConversion(
    val ev100: Double,
    val lux: Double,
    val footCandles: Double,
)

/** Ambient EV100 from a lux reading using the same convention as the light meter. */
fun ev100FromLux(lux: Double): Double? {
    if (!lux.isFinite() || lux <= 0.0) return null
    return log2(lux / EV100_ZERO_LUX)
}

/** Lux required for a given ambient EV100. */
fun luxFromEv100(ev100: Double): Double? {
    if (!ev100.isFinite()) return null
    val lux = EV100_ZERO_LUX * 2.0.pow(ev100)
    return lux.takeIf { it.isFinite() && it > 0.0 }
}

/** Foot-candles from a lux reading. */
fun footCandlesFromLux(lux: Double): Double? {
    if (!lux.isFinite() || lux < 0.0) return null
    return (lux / LUX_PER_FOOT_CANDLE).takeIf { it.isFinite() }
}

/** Lux from a foot-candle reading. */
fun luxFromFootCandles(footCandles: Double): Double? {
    if (!footCandles.isFinite() || footCandles < 0.0) return null
    return (footCandles * LUX_PER_FOOT_CANDLE).takeIf { it.isFinite() }
}

fun convertIlluminanceFromEv100(ev100: Double): IlluminanceConversion? {
    val lux = luxFromEv100(ev100) ?: return null
    val footCandles = footCandlesFromLux(lux) ?: return null
    return IlluminanceConversion(
        ev100 = ev100,
        lux = lux,
        footCandles = footCandles,
    )
}

fun convertIlluminanceFromLux(lux: Double): IlluminanceConversion? {
    if (lux <= 0.0) return null
    val ev100 = ev100FromLux(lux) ?: return null
    val footCandles = footCandlesFromLux(lux) ?: return null
    return IlluminanceConversion(
        ev100 = ev100,
        lux = lux,
        footCandles = footCandles,
    )
}

fun convertIlluminanceFromFootCandles(footCandles: Double): IlluminanceConversion? {
    if (footCandles <= 0.0) return null
    val lux = luxFromFootCandles(footCandles) ?: return null
    val ev100 = ev100FromLux(lux) ?: return null
    return IlluminanceConversion(
        ev100 = ev100,
        lux = lux,
        footCandles = footCandles,
    )
}
