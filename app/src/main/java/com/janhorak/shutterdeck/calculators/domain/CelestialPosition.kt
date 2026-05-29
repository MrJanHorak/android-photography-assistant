package com.janhorak.shutterdeck.calculators.domain

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure, Android-free sun and moon position + moon phase calculator, based on the
 * widely used SunCalc algorithm (low-precision Meeus). All trigonometry is done in
 * radians and no platform date types are used so the logic can be shared with iOS.
 *
 * Latitude is positive north, longitude positive east, and [utcOffsetHours] is the
 * local UTC offset in hours (e.g. -4 for EDT). Azimuth is a compass bearing measured
 * clockwise from true north (0° = N, 90° = E, 180° = S, 270° = W). Altitude is the
 * angle above the horizon (negative when the body is below the horizon).
 */

private const val DEG = PI / 180.0
private const val OBLIQUITY = DEG * 23.4397
private const val SUN_DISTANCE_KM = 149_598_000.0

/** Horizontal (observer-relative) position of a celestial body. */
data class HorizontalPosition(
    val azimuthDeg: Double,
    val altitudeDeg: Double,
    val distanceKm: Double? = null,
)

/** Moon illumination state for a given instant. */
data class MoonIllumination(
    /** Illuminated fraction of the moon's disc, 0 (new) to 1 (full). */
    val fraction: Double,
    /** Phase as 0=new, 0.25=first quarter, 0.5=full, 0.75=last quarter. */
    val phase: Double,
    /** Phase angle between sun and moon as seen from earth, in degrees. */
    val phaseAngleDeg: Double,
    val phaseName: String,
)

private fun rightAscension(eclipticLon: Double, eclipticLat: Double): Double =
    atan2(
        sin(eclipticLon) * cos(OBLIQUITY) - tan(eclipticLat) * sin(OBLIQUITY),
        cos(eclipticLon),
    )

private fun declination(eclipticLon: Double, eclipticLat: Double): Double =
    asin(sin(eclipticLat) * cos(OBLIQUITY) + cos(eclipticLat) * sin(OBLIQUITY) * sin(eclipticLon))

private fun azimuthFromSouth(hourAngle: Double, latRad: Double, decl: Double): Double =
    atan2(sin(hourAngle), cos(hourAngle) * sin(latRad) - tan(decl) * cos(latRad))

private fun altitude(hourAngle: Double, latRad: Double, decl: Double): Double =
    asin(sin(latRad) * sin(decl) + cos(latRad) * cos(decl) * cos(hourAngle))

private fun siderealTime(days: Double, lwRad: Double): Double =
    DEG * (280.16 + 360.9856235 * days) - lwRad

private fun julianDayAtMidnightUtcCelestial(year: Int, month: Int, day: Int): Double {
    var y = year
    var m = month
    if (m <= 2) {
        y -= 1
        m += 12
    }
    val a = floor(y / 100.0)
    val b = 2 - a + floor(a / 4.0)
    return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
}

/** Days since the J2000.0 epoch for the given local civil date-time. */
private fun daysSinceJ2000(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    utcOffsetHours: Double,
): Double {
    val utcDecimalHour = hour + minute / 60.0 - utcOffsetHours
    return julianDayAtMidnightUtcCelestial(year, month, day) + utcDecimalHour / 24.0 - 2451545.0
}

private fun solarMeanAnomaly(days: Double): Double = DEG * (357.5291 + 0.98560028 * days)

private fun eclipticLongitude(meanAnomaly: Double): Double {
    val center = DEG * (1.9148 * sin(meanAnomaly) + 0.02 * sin(2 * meanAnomaly) + 0.0003 * sin(3 * meanAnomaly))
    val perihelion = DEG * 102.9372
    return meanAnomaly + center + perihelion + PI
}

private class EquatorialCoords(val rightAscension: Double, val declination: Double, val distanceKm: Double)

private fun sunCoords(days: Double): EquatorialCoords {
    val m = solarMeanAnomaly(days)
    val l = eclipticLongitude(m)
    return EquatorialCoords(rightAscension(l, 0.0), declination(l, 0.0), SUN_DISTANCE_KM)
}

private fun moonCoords(days: Double): EquatorialCoords {
    val l = DEG * (218.316 + 13.176396 * days)
    val m = DEG * (134.963 + 13.064993 * days)
    val f = DEG * (93.272 + 13.229350 * days)
    val lon = l + DEG * 6.289 * sin(m)
    val lat = DEG * 5.128 * sin(f)
    val distance = 385001.0 - 20905.0 * cos(m)
    return EquatorialCoords(rightAscension(lon, lat), declination(lon, lat), distance)
}

private fun toCompassBearing(azimuthFromSouthRad: Double): Double {
    val deg = azimuthFromSouthRad / DEG + 180.0
    return ((deg % 360.0) + 360.0) % 360.0
}

private fun horizontalPosition(
    days: Double,
    latitudeDeg: Double,
    longitudeDeg: Double,
    coords: EquatorialCoords,
): HorizontalPosition {
    val lw = DEG * -longitudeDeg
    val phi = DEG * latitudeDeg
    val hourAngle = siderealTime(days, lw) - coords.rightAscension
    return HorizontalPosition(
        azimuthDeg = toCompassBearing(azimuthFromSouth(hourAngle, phi, coords.declination)),
        altitudeDeg = altitude(hourAngle, phi, coords.declination) / DEG,
        distanceKm = coords.distanceKm,
    )
}

private fun validInputs(year: Int, month: Int, day: Int, hour: Int, minute: Int, latitudeDeg: Double, longitudeDeg: Double): Boolean {
    if (month !in 1..12 || day !in 1..31) return false
    if (hour !in 0..23 || minute !in 0..59) return false
    if (latitudeDeg < -90.0 || latitudeDeg > 90.0) return false
    if (longitudeDeg < -180.0 || longitudeDeg > 180.0) return false
    return true
}

/** Compass azimuth + altitude of the sun for the given local date-time and place. */
fun sunPosition(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    latitudeDeg: Double,
    longitudeDeg: Double,
    utcOffsetHours: Double,
): HorizontalPosition? {
    if (!validInputs(year, month, day, hour, minute, latitudeDeg, longitudeDeg)) return null
    val days = daysSinceJ2000(year, month, day, hour, minute, utcOffsetHours)
    return horizontalPosition(days, latitudeDeg, longitudeDeg, sunCoords(days)).copy(distanceKm = null)
}

/** Compass azimuth + altitude (and distance) of the moon for the given local date-time and place. */
fun moonPosition(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    latitudeDeg: Double,
    longitudeDeg: Double,
    utcOffsetHours: Double,
): HorizontalPosition? {
    if (!validInputs(year, month, day, hour, minute, latitudeDeg, longitudeDeg)) return null
    val days = daysSinceJ2000(year, month, day, hour, minute, utcOffsetHours)
    return horizontalPosition(days, latitudeDeg, longitudeDeg, moonCoords(days))
}

/** Maps a SunCalc phase value (0..1) to a human-readable moon phase name. */
fun moonPhaseName(phase: Double): String {
    val p = ((phase % 1.0) + 1.0) % 1.0
    return when {
        p < 0.0625 || p >= 0.9375 -> "New moon"
        p < 0.1875 -> "Waxing crescent"
        p < 0.3125 -> "First quarter"
        p < 0.4375 -> "Waxing gibbous"
        p < 0.5625 -> "Full moon"
        p < 0.6875 -> "Waning gibbous"
        p < 0.8125 -> "Last quarter"
        else -> "Waning crescent"
    }
}

/** Illuminated fraction, phase and phase name of the moon for the given local date-time. */
fun moonIllumination(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    utcOffsetHours: Double,
): MoonIllumination? {
    if (month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59) return null
    val days = daysSinceJ2000(year, month, day, hour, minute, utcOffsetHours)
    val sun = sunCoords(days)
    val moon = moonCoords(days)
    val phaseAngle = acos(
        sin(sun.declination) * sin(moon.declination) +
            cos(sun.declination) * cos(moon.declination) * cos(sun.rightAscension - moon.rightAscension),
    )
    val inc = atan2(SUN_DISTANCE_KM * sin(phaseAngle), moon.distanceKm - SUN_DISTANCE_KM * cos(phaseAngle))
    val angle = atan2(
        cos(sun.declination) * sin(sun.rightAscension - moon.rightAscension),
        sin(sun.declination) * cos(moon.declination) -
            cos(sun.declination) * sin(moon.declination) * cos(sun.rightAscension - moon.rightAscension),
    )
    val fraction = (1 + cos(inc)) / 2
    val phase = 0.5 + 0.5 * inc * (if (angle < 0) -1.0 else 1.0) / PI
    return MoonIllumination(
        fraction = fraction,
        phase = phase,
        phaseAngleDeg = phaseAngle / DEG,
        phaseName = moonPhaseName(phase),
    )
}

/** Converts a compass bearing in degrees to a 16-point compass label (N, NNE, …). */
fun compassDirection(azimuthDeg: Double): String {
    val points = listOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
    )
    val normalized = ((azimuthDeg % 360.0) + 360.0) % 360.0
    val index = (normalized / 22.5 + 0.5).toInt() % 16
    return points[index]
}
