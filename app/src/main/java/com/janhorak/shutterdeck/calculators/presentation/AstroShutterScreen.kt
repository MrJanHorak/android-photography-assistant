package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.janhorak.shutterdeck.calculators.domain.npfMaxShutterSeconds
import com.janhorak.shutterdeck.calculators.domain.ruleBasedMaxShutterSeconds
import com.janhorak.shutterdeck.metering.domain.formatExposureTime
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

@Composable
fun AstroShutterScreen(modifier: Modifier = Modifier) {
    var focalLength by rememberInput("20")
    var cropFactor by rememberInput("1.0")
    var aperture by rememberInput("2.8")
    var pixelPitch by rememberInput("4.3")

    val focal = focalLength.toDoubleOrNull()
    val crop = cropFactor.toDoubleOrNull()
    val n = aperture.toDoubleOrNull()
    val pitch = pixelPitch.toDoubleOrNull()

    val rule500 = if (focal != null && crop != null) ruleBasedMaxShutterSeconds(focal, crop, 500.0) else null
    val rule300 = if (focal != null && crop != null) ruleBasedMaxShutterSeconds(focal, crop, 300.0) else null
    val npf = if (focal != null && n != null && pitch != null) {
        // NPF uses the actual focal length; pixel pitch already encodes the sensor.
        npfMaxShutterSeconds(n, pitch, focal)
    } else {
        null
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Astro shutter",
            subtitle = "Longest exposure before stars trail (500 rule and the sharper NPF rule).",
        )
        LabeledField("Focal length", focalLength, { focalLength = it }, suffix = "mm")
        LabeledField("Crop factor", cropFactor, { cropFactor = it }, suffix = "x")
        LabeledField("Aperture (NPF)", aperture, { aperture = it }, suffix = "f/")
        LabeledField("Pixel pitch (NPF)", pixelPitch, { pixelPitch = it }, suffix = "µm")

        if (rule500 != null || npf != null) {
            ResultCard {
                if (rule500 != null) ResultRow("500 rule", formatExposureTime(rule500))
                if (rule300 != null) ResultRow("300 rule (stricter)", formatExposureTime(rule300))
                if (npf != null) {
                    HorizontalDivider()
                    ResultRow("NPF rule", formatExposureTime(npf))
                    ResultRow("NPF in seconds", formatOneDecimal(npf) + " s")
                }
            }
        } else {
            CalculatorHint("Enter at least a focal length and crop factor. Add aperture and pixel pitch for the NPF rule.")
        }
    }
}
