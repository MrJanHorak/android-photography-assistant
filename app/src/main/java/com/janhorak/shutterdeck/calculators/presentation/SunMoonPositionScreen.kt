package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.calculators.domain.compassDirection
import com.janhorak.shutterdeck.calculators.domain.moonIllumination
import com.janhorak.shutterdeck.calculators.domain.moonPosition
import com.janhorak.shutterdeck.calculators.domain.sunPosition
import com.janhorak.shutterdeck.core.time.formatStructuredDate
import com.janhorak.shutterdeck.core.time.formatStructuredTime
import com.janhorak.shutterdeck.core.time.parseStructuredDateOrNull
import com.janhorak.shutterdeck.core.time.parseStructuredTimeOrNull
import com.janhorak.shutterdeck.ui.components.DatePickerField
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader
import com.janhorak.shutterdeck.ui.components.TimePickerField
import com.janhorak.shutterdeck.ui.location.CurrentLocationAction
import com.janhorak.shutterdeck.ui.location.formatCoordinateInput
import com.janhorak.shutterdeck.ui.location.rememberCurrentLocationRequestState
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private fun horizonLabel(altitudeDeg: Double): String =
    if (altitudeDeg >= 0.0) "above horizon" else "below horizon"

@Composable
fun SunMoonPositionScreen(modifier: Modifier = Modifier) {
    val now = remember { LocalDateTime.now() }
    val defaultOffsetHours = remember {
        ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds / 3600.0
    }

    var latitude by rememberInput("40.7128")
    var longitude by rememberInput("-74.0060")
    var dateText by rememberInput(formatStructuredDate(now.toLocalDate()))
    var timeText by rememberInput(formatStructuredTime(now.toLocalTime()))
    var utcOffset by rememberInput(formatOneDecimal(defaultOffsetHours))
    val currentLocationState = rememberCurrentLocationRequestState { coordinates ->
        latitude = formatCoordinateInput(coordinates.latitude)
        longitude = formatCoordinateInput(coordinates.longitude)
        utcOffset = formatOneDecimal(
            ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds / 3600.0,
        )
    }

    val lat = latitude.toDoubleOrNull()
    val lon = longitude.toDoubleOrNull()
    val selectedDate = parseStructuredDateOrNull(dateText)
    val selectedTime = parseStructuredTimeOrNull(timeText)
    val tz = utcOffset.toDoubleOrNull()

    val ready = lat != null && lon != null && selectedDate != null && selectedTime != null && tz != null

    val sun = if (ready) {
        sunPosition(
            selectedDate!!.year,
            selectedDate.monthValue,
            selectedDate.dayOfMonth,
            selectedTime!!.hour,
            selectedTime.minute,
            lat!!,
            lon!!,
            tz!!,
        )
    } else {
        null
    }
    val moon = if (ready) {
        moonPosition(
            selectedDate!!.year,
            selectedDate.monthValue,
            selectedDate.dayOfMonth,
            selectedTime!!.hour,
            selectedTime.minute,
            lat!!,
            lon!!,
            tz!!,
        )
    } else {
        null
    }
    val illum = if (ready) {
        moonIllumination(
            selectedDate!!.year,
            selectedDate.monthValue,
            selectedDate.dayOfMonth,
            selectedTime!!.hour,
            selectedTime.minute,
            tz!!,
        )
    } else {
        null
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Sun & moon position",
            subtitle = "Offline azimuth, altitude and moon phase for any date, time and place.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField("Latitude", latitude, { latitude = it }, modifier = Modifier.weight(1f), suffix = "°")
            LabeledField("Longitude", longitude, { longitude = it }, modifier = Modifier.weight(1f), suffix = "°")
        }
        CurrentLocationAction(state = currentLocationState)
        DatePickerField("Date", dateText, { dateText = it }, allowClear = false)
        TimePickerField("Time", timeText, { timeText = it }, allowClear = false)
        LabeledField("UTC offset", utcOffset, { utcOffset = it }, suffix = "h")

        if (sun != null && moon != null && illum != null) {
            ResultCard {
                ResultRow("Sun azimuth", formatDegrees(sun.azimuthDeg) + " (" + compassDirection(sun.azimuthDeg) + ")")
                ResultRow("Sun altitude", formatDegrees(sun.altitudeDeg) + " · " + horizonLabel(sun.altitudeDeg))
                HorizontalDivider()
                ResultRow("Moon azimuth", formatDegrees(moon.azimuthDeg) + " (" + compassDirection(moon.azimuthDeg) + ")")
                ResultRow("Moon altitude", formatDegrees(moon.altitudeDeg) + " · " + horizonLabel(moon.altitudeDeg))
                moon.distanceKm?.let { ResultRow("Moon distance", formatOneDecimal(it / 1000.0) + " ×10³ km") }
                HorizontalDivider()
                ResultRow("Moon phase", illum.phaseName)
                ResultRow("Illuminated", formatOneDecimal(illum.fraction * 100.0) + " %")
            }
            CalculatorHint("Azimuth is a compass bearing from true north. Altitude is the angle above the horizon (negative = below).")
        } else {
            CalculatorHint("Enter latitude, longitude, a valid date, a 24h time and the location's UTC offset.")
        }
    }
}
