package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.janhorak.shutterdeck.calculators.domain.goodEnoughDpi
import com.janhorak.shutterdeck.calculators.domain.megapixels
import com.janhorak.shutterdeck.calculators.domain.printSizeAtDpi
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

@Composable
fun PrintSizeScreen(modifier: Modifier = Modifier) {
    var pixelWidth by rememberInput("6000")
    var pixelHeight by rememberInput("4000")
    var dpi by rememberInput("300")
    var viewingDistance by rememberInput("12")

    val width = pixelWidth.toIntOrNull()
    val height = pixelHeight.toIntOrNull()
    val resolution = dpi.toDoubleOrNull()
    val distance = viewingDistance.toDoubleOrNull()

    val print = if (width != null && height != null && resolution != null) {
        printSizeAtDpi(width, height, resolution)
    } else {
        null
    }
    val recommendedDpi = distance?.let { goodEnoughDpi(it) }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Print size",
            subtitle = "Print dimensions at a chosen DPI, plus a viewing-distance DPI target.",
        )
        LabeledField("Image width", pixelWidth, { pixelWidth = it }, suffix = "px")
        LabeledField("Image height", pixelHeight, { pixelHeight = it }, suffix = "px")
        LabeledField("Print resolution", dpi, { dpi = it }, suffix = "DPI")
        LabeledField("Viewing distance", viewingDistance, { viewingDistance = it }, suffix = "in")

        if (print != null) {
            ResultCard {
                ResultRow(
                    "Print size",
                    formatInches(print.widthInches) + " x " + formatInches(print.heightInches),
                )
                ResultRow(
                    "Print size (metric)",
                    formatCm(print.widthCm) + " x " + formatCm(print.heightCm),
                )
                ResultRow("Resolution", formatOneDecimal(megapixels(width!!, height!!)) + " MP")
                if (recommendedDpi != null) {
                    HorizontalDivider()
                    ResultRow("Good-enough DPI", formatOneDecimal(recommendedDpi) + " DPI")
                }
            }
        } else {
            CalculatorHint("Enter image pixel dimensions and a print DPI.")
        }
    }
}
