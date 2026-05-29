package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.janhorak.shutterdeck.calculators.domain.equivalentShutterForAperture
import com.janhorak.shutterdeck.calculators.domain.equivalentShutterForIso
import com.janhorak.shutterdeck.calculators.domain.shutterStops
import com.janhorak.shutterdeck.metering.domain.formatExposureTime
import com.janhorak.shutterdeck.metering.domain.formatStopCount
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

private fun parseShutterInput(raw: String): Double? {
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
fun EquivalentExposureScreen(modifier: Modifier = Modifier) {
    var baseShutter by rememberInput("1/100")
    var baseAperture by rememberInput("2.8")
    var baseIso by rememberInput("100")
    var targetAperture by rememberInput("5.6")
    var targetIso by rememberInput("400")

    val shutter = parseShutterInput(baseShutter)
    val aperture = baseAperture.toDoubleOrNull()
    val iso = baseIso.toIntOrNull()
    val newAperture = targetAperture.toDoubleOrNull()
    val newIso = targetIso.toIntOrNull()

    val shutterForAperture = if (shutter != null && aperture != null && newAperture != null) {
        equivalentShutterForAperture(shutter, aperture, newAperture)
    } else {
        null
    }
    val shutterForIso = if (shutter != null && iso != null && newIso != null) {
        equivalentShutterForIso(shutter, iso, newIso)
    } else {
        null
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Equivalent exposure",
            subtitle = "Keep the same exposure while trading aperture, shutter and ISO.",
        )
        LabeledField("Base shutter (e.g. 1/100)", baseShutter, { baseShutter = it }, suffix = "s")
        LabeledField("Base aperture", baseAperture, { baseAperture = it }, suffix = "f/")
        LabeledField("Base ISO", baseIso, { baseIso = it })
        LabeledField("Target aperture", targetAperture, { targetAperture = it }, suffix = "f/")
        LabeledField("Target ISO", targetIso, { targetIso = it })

        if (shutterForAperture != null || shutterForIso != null) {
            ResultCard {
                if (shutterForAperture != null) {
                    ResultRow("Shutter at target aperture", formatExposureTime(shutterForAperture))
                    val stops = shutterStops(shutter!!, shutterForAperture)
                    if (stops != null) ResultRow("Aperture change", formatStopCount(kotlin.math.abs(stops)))
                }
                if (shutterForIso != null) {
                    HorizontalDivider()
                    ResultRow("Shutter at target ISO", formatExposureTime(shutterForIso))
                }
            }
        } else {
            CalculatorHint("Enter a base exposure and target aperture/ISO.")
        }
    }
}
