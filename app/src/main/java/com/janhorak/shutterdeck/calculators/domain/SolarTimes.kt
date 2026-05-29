package com.janhorak.shutterdeck.calculators.domain

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure, Android-free solar event calculator based on the NOAA solar-position
 * algorithm. Times are returned as minutes after local midnight (may be < 0 or
 * > 1440 near the day boundary; normalize when displaying). No platform date types
 * are used so the logic can be shared with iOS.
 *
 * Latitude is positive north, longitude positive east, and [utcOffsetHours] is the
 * local UTC offset in hours (e.g. -4 for EDT).
 */

private fun sinDeg(deg: Double) = sin(Math.toRadians(deg))
private fun cosDeg(deg: Double) = cos(Math.toRadians(deg))
private fun tanDeg(deg: Double) = tan(Math.toRadians(deg))

/** The standard refraction-corrected geometric elevation for sunrise/sunset. */
const val SUNRISE_ELEVATION_DEG: Double = -0.833

/** Sun elevation boundaries used for golden and blue hour. */
const val GOLDEN_HOUR_UPPER_DEG: Double = 6.0
const val GOLDEN_BLUE_BOUNDARY_DEG: Double = -4.0
const val CIVIL_TWILIGHT_DEG: Double = -6.0

data class SunTimes(
    val solarNoonMinutes: Double,
    val sunriseMinutes: Double?,
    val sunsetMinutes: Double?,
    val morningBlueHourStartMinutes: Double?,
    val morningBlueHourEndMinutes: Double?,
    val morningGoldenHourEndMinutes: Double?,
    val eveningGoldenHourStartMinutes: Double?,
    val eveningBlueHourStartMinutes: Double?,
    val eveningBlueHourEndMinutes: Double?,
) {
    val morningGoldenHourStartMinutes: Double? get() = morningBlueHourEndMinutes
    val eveningGoldenHourEndMinutes: Double? get() = eveningBlueHourStartMinutes
}

private class SolarDay(
    val declinationDeg: Double,
    val solarNoonMinutes: Double,
)

private fun julianDayAtMidnightUtc(year: Int, month: Int, day: Int): Double {
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

private fun solarDay(
    year: Int,
    month: Int,
    day: Int,
    longitudeDeg: Double,
    utcOffsetHours: Double,
): SolarDay {
    val jd0 = julianDayAtMidnightUtc(year, month, day)
    val jdNoon = jd0 + (12.0 - utcOffsetHours) / 24.0
    val t = (jdNoon - 2451545.0) / 36525.0

    val l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)).mod(360.0)
    val m = 357.52911 + t * (35999.05029 - 0.0001537 * t)
    val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
    val c = sinDeg(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
        sinDeg(2 * m) * (0.019993 - 0.000101 * t) +
        sinDeg(3 * m) * 0.000289
    val trueLong = l0 + c
    val omega = 125.04 - 1934.136 * t
    val appLong = trueLong - 0.00569 - 0.00478 * sinDeg(omega)
    val meanObliq = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
    val obliqCorr = meanObliq + 0.00256 * cosDeg(omega)
    val declination = Math.toDegrees(asin(sinDeg(obliqCorr) * sinDeg(appLong)))

    val varY = tanDeg(obliqCorr / 2.0) * tanDeg(obliqCorr / 2.0)
    val eqTime = 4.0 * Math.toDegrees(
        varY * sinDeg(2 * l0) -
            2 * e * sinDeg(m) +
            4 * e * varY * sinDeg(m) * cosDeg(2 * l0) -
            0.5 * varY * varY * sinDeg(4 * l0) -
            1.25 * e * e * sinDeg(2 * m),
    )

    val solarNoon = 720.0 - 4.0 * longitudeDeg - eqTime + utcOffsetHours * 60.0
    return SolarDay(declinationDeg = declination, solarNoonMinutes = solarNoon)
}

/**
 * Minutes after local midnight at which the sun reaches [elevationDeg], on the
 * morning ([morning] = true) or evening side of solar noon. Returns null when the sun
 * never reaches that elevation on the given day (polar day/night).
 */
fun sunEventMinutes(
    year: Int,
    month: Int,
    day: Int,
    latitudeDeg: Double,
    longitudeDeg: Double,
    utcOffsetHours: Double,
    elevationDeg: Double,
    morning: Boolean,
): Double? {
    if (latitudeDeg < -90.0 || latitudeDeg > 90.0) return null
    val solar = solarDay(year, month, day, longitudeDeg, utcOffsetHours)
    val decl = solar.declinationDeg
    val cosH = (sinDeg(elevationDeg) - sinDeg(latitudeDeg) * sinDeg(decl)) /
        (cosDeg(latitudeDeg) * cosDeg(decl))
    if (cosH < -1.0 || cosH > 1.0) return null
    val hourAngleDeg = Math.toDegrees(acos(cosH))
    return if (morning) solar.solarNoonMinutes - 4.0 * hourAngleDeg else solar.solarNoonMinutes + 4.0 * hourAngleDeg
}

/** All commonly used solar events for a date and location. */
fun calculateSunTimes(
    year: Int,
    month: Int,
    day: Int,
    latitudeDeg: Double,
    longitudeDeg: Double,
    utcOffsetHours: Double,
): SunTimes? {
    if (latitudeDeg < -90.0 || latitudeDeg > 90.0) return null
    if (longitudeDeg < -180.0 || longitudeDeg > 180.0) return null
    val solar = solarDay(year, month, day, longitudeDeg, utcOffsetHours)

    fun event(elevation: Double, morning: Boolean) =
        sunEventMinutes(year, month, day, latitudeDeg, longitudeDeg, utcOffsetHours, elevation, morning)

    return SunTimes(
        solarNoonMinutes = solar.solarNoonMinutes,
        sunriseMinutes = event(SUNRISE_ELEVATION_DEG, true),
        sunsetMinutes = event(SUNRISE_ELEVATION_DEG, false),
        morningBlueHourStartMinutes = event(CIVIL_TWILIGHT_DEG, true),
        morningBlueHourEndMinutes = event(GOLDEN_BLUE_BOUNDARY_DEG, true),
        morningGoldenHourEndMinutes = event(GOLDEN_HOUR_UPPER_DEG, true),
        eveningGoldenHourStartMinutes = event(GOLDEN_HOUR_UPPER_DEG, false),
        eveningBlueHourStartMinutes = event(GOLDEN_BLUE_BOUNDARY_DEG, false),
        eveningBlueHourEndMinutes = event(CIVIL_TWILIGHT_DEG, false),
    )
}
