package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.janhorak.shutterdeck.calculators.domain.ndAdjustedShutterSeconds
import com.janhorak.shutterdeck.metering.domain.formatExposureTime
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

private fun parseShutter(raw: String): Double? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    if (text.contains('/')) {
        val parts = text.split('/')
        if (parts.size != 2) return null
        val numerator = parts[0].trim().toDoubleOrNull() ?: return null
        val denominator = parts[1].trim().toDoubleOrNull() ?: return null
        if (denominator == 0.0) return null
        return numerator / denominator
    }
    return text.toDoubleOrNull()
}

@Composable
fun NdFilterScreen(modifier: Modifier = Modifier) {
    var baseShutter by rememberInput("1/60")
    var ndStops by rememberInput("10")

    val base = parseShutter(baseShutter)
    val stops = ndStops.toDoubleOrNull()
    val adjusted = if (base != null && stops != null) ndAdjustedShutterSeconds(base, stops) else null

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "ND filter / long exposure",
            subtitle = "Add ND stops to a metered shutter to get the long-exposure time.",
        )
        LabeledField(
            label = "Base shutter (e.g. 1/60 or 2)",
            value = baseShutter,
            onValueChange = { baseShutter = it },
            suffix = "s",
        )
        LabeledField("ND strength", ndStops, { ndStops = it }, suffix = "stops")

        if (adjusted != null) {
            ResultCard {
                ResultRow("Resulting shutter", formatExposureTime(adjusted))
                ResultRow("In seconds", formatOneDecimal(adjusted) + " s")
            }
        } else {
            CalculatorHint("Enter a base shutter (e.g. 1/60) and the ND strength in stops.")
        }
    }
}
