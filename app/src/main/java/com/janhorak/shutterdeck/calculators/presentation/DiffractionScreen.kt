package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.janhorak.shutterdeck.calculators.domain.airyDiskDiameterMicrons
import com.janhorak.shutterdeck.calculators.domain.diffractionLimitedAperture
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

@Composable
fun DiffractionScreen(modifier: Modifier = Modifier) {
    var pixelPitch by rememberInput("4.3")
    var aperture by rememberInput("8")

    val pitch = pixelPitch.toDoubleOrNull()
    val n = aperture.toDoubleOrNull()

    val limitAperture = pitch?.let { diffractionLimitedAperture(it) }
    val airyDisk = n?.let { airyDiskDiameterMicrons(it) }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Diffraction limit",
            subtitle = "The aperture where diffraction starts to soften fine detail on your sensor.",
        )
        LabeledField("Pixel pitch", pixelPitch, { pixelPitch = it }, suffix = "µm")
        LabeledField("Aperture (f-number)", aperture, { aperture = it }, suffix = "f/")

        if (limitAperture != null || airyDisk != null) {
            ResultCard {
                if (limitAperture != null) {
                    ResultRow("Diffraction-limited aperture", "f/" + formatOneDecimal(limitAperture))
                }
                if (airyDisk != null) {
                    HorizontalDivider()
                    ResultRow("Airy disk at this aperture", formatOneDecimal(airyDisk) + " µm")
                }
            }
        } else {
            CalculatorHint("Enter the sensor pixel pitch (and an aperture for the Airy disk size).")
        }
    }
}
