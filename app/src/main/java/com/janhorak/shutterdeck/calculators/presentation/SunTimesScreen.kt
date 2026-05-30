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
import com.janhorak.shutterdeck.calculators.domain.calculateSunTimes
import com.janhorak.shutterdeck.core.time.formatStructuredDate
import com.janhorak.shutterdeck.core.time.parseStructuredDateOrNull
import com.janhorak.shutterdeck.ui.components.DatePickerField
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader
import com.janhorak.shutterdeck.ui.location.CurrentLocationAction
import com.janhorak.shutterdeck.ui.location.formatCoordinateInput
import com.janhorak.shutterdeck.ui.location.rememberCurrentLocationRequestState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun SunTimesScreen(modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    val defaultOffsetHours = remember {
        ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds / 3600.0
    }

    var latitude by rememberInput("40.7128")
    var longitude by rememberInput("-74.0060")
    var dateText by rememberInput(formatStructuredDate(today))
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
    val tz = utcOffset.toDoubleOrNull()

    val times = if (lat != null && lon != null && selectedDate != null && tz != null) {
        calculateSunTimes(
            selectedDate.year,
            selectedDate.monthValue,
            selectedDate.dayOfMonth,
            lat,
            lon,
            tz,
        )
    } else {
        null
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Golden hour & sun times",
            subtitle = "Offline sunrise, sunset, golden and blue hour for any date and place.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField("Latitude", latitude, { latitude = it }, modifier = Modifier.weight(1f), suffix = "°")
            LabeledField("Longitude", longitude, { longitude = it }, modifier = Modifier.weight(1f), suffix = "°")
        }
        CurrentLocationAction(state = currentLocationState)
        DatePickerField("Date", dateText, { dateText = it }, allowClear = false)
        LabeledField("UTC offset", utcOffset, { utcOffset = it }, suffix = "h")

        if (times != null) {
            ResultCard {
                ResultRow("Morning blue hour", formatClock(times.morningBlueHourStartMinutes) + " – " + formatClock(times.morningBlueHourEndMinutes))
                ResultRow("Sunrise", formatClock(times.sunriseMinutes))
                ResultRow("Morning golden hour", formatClock(times.morningGoldenHourStartMinutes) + " – " + formatClock(times.morningGoldenHourEndMinutes))
                HorizontalDivider()
                ResultRow("Solar noon", formatClock(times.solarNoonMinutes))
                HorizontalDivider()
                ResultRow("Evening golden hour", formatClock(times.eveningGoldenHourStartMinutes) + " – " + formatClock(times.eveningGoldenHourEndMinutes))
                ResultRow("Sunset", formatClock(times.sunsetMinutes))
                ResultRow("Evening blue hour", formatClock(times.eveningBlueHourStartMinutes) + " – " + formatClock(times.eveningBlueHourEndMinutes))
            }
            CalculatorHint("Times are local to the chosen UTC offset. “—” means the sun stays above or below that angle all day.")
        } else {
            CalculatorHint("Enter latitude, longitude, a valid date and the location's UTC offset.")
        }
    }
}
