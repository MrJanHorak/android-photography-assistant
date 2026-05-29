package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.janhorak.shutterdeck.calculators.domain.calculateMacro
import com.janhorak.shutterdeck.metering.domain.formatStopCount
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

@Composable
fun MacroScreen(modifier: Modifier = Modifier) {
    var focalLength by rememberInput("50")
    var aperture by rememberInput("2.8")
    var extension by rememberInput("25")

    val focal = focalLength.toDoubleOrNull()
    val n = aperture.toDoubleOrNull()
    val ext = extension.toDoubleOrNull()
    val result = if (focal != null && n != null && ext != null) calculateMacro(focal, n, ext) else null

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Macro / extension",
            subtitle = "Magnification, effective aperture and exposure compensation for extension tubes.",
        )
        LabeledField("Focal length", focalLength, { focalLength = it }, suffix = "mm")
        LabeledField("Marked aperture", aperture, { aperture = it }, suffix = "f/")
        LabeledField("Extension", extension, { extension = it }, suffix = "mm")

        if (result != null) {
            ResultCard {
                ResultRow("Magnification", formatOneDecimal(result.magnification) + "x")
                HorizontalDivider()
                ResultRow("Effective aperture", "f/" + formatOneDecimal(result.effectiveAperture))
                ResultRow("Exposure compensation", "+" + formatStopCount(result.exposureCompensationStops))
            }
        } else {
            CalculatorHint("Enter focal length, marked aperture and extension length.")
        }
    }
}
