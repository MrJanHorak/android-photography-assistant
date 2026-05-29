package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.janhorak.shutterdeck.calculators.domain.apertureForFlash
import com.janhorak.shutterdeck.calculators.domain.distanceForFlash
import com.janhorak.shutterdeck.calculators.domain.guideNumberAtIso
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

@Composable
fun GuideNumberScreen(modifier: Modifier = Modifier) {
    var guideNumber by rememberInput("56")
    var iso by rememberInput("100")
    var distance by rememberInput("5")
    var aperture by rememberInput("8")

    val gn = guideNumber.toDoubleOrNull()
    val isoValue = iso.toIntOrNull()
    val distanceValue = distance.toDoubleOrNull()
    val apertureValue = aperture.toDoubleOrNull()

    val effectiveGn = if (gn != null && isoValue != null) guideNumberAtIso(gn, isoValue) else null
    val apertureResult = if (gn != null && distanceValue != null && isoValue != null) {
        apertureForFlash(gn, distanceValue, isoValue)
    } else {
        null
    }
    val distanceResult = if (gn != null && apertureValue != null && isoValue != null) {
        distanceForFlash(gn, apertureValue, isoValue)
    } else {
        null
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Flash guide number",
            subtitle = "Relate guide number, distance and aperture (GN is quoted at ISO 100).",
        )
        LabeledField("Guide number (ISO 100, m)", guideNumber, { guideNumber = it })
        LabeledField("ISO", iso, { iso = it })
        LabeledField("Flash distance", distance, { distance = it }, suffix = "m")
        LabeledField("Aperture (f-number)", aperture, { aperture = it }, suffix = "f/")

        if (effectiveGn != null) {
            ResultCard {
                ResultRow("Guide number at ISO", formatOneDecimal(effectiveGn) + " m")
                HorizontalDivider()
                if (apertureResult != null) {
                    ResultRow("Aperture for distance", "f/" + formatOneDecimal(apertureResult))
                }
                if (distanceResult != null) {
                    ResultRow("Distance for aperture", formatOneDecimal(distanceResult) + " m")
                }
            }
        } else {
            CalculatorHint("Enter a guide number and ISO, plus a distance and/or aperture.")
        }
    }
}
