package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.janhorak.shutterdeck.calculators.domain.calculateFieldOfView
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

@Composable
fun FieldOfViewScreen(modifier: Modifier = Modifier) {
    var focalLength by rememberInput("35")
    var cropFactor by rememberInput("1.0")

    val focal = focalLength.toDoubleOrNull()
    val crop = cropFactor.toDoubleOrNull()
    val result = if (focal != null && crop != null) calculateFieldOfView(focal, crop) else null

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Field of view",
            subtitle = "Angle of view and 35mm-equivalent focal length for your sensor.",
        )
        LabeledField("Focal length", focalLength, { focalLength = it }, suffix = "mm")
        LabeledField("Crop factor", cropFactor, { cropFactor = it }, suffix = "x")

        if (result != null) {
            ResultCard {
                ResultRow("35mm-equivalent", formatOneDecimal(result.equivalentFocalLengthMm) + " mm")
                HorizontalDivider()
                ResultRow("Horizontal angle", formatDegrees(result.horizontalDeg))
                ResultRow("Vertical angle", formatDegrees(result.verticalDeg))
                ResultRow("Diagonal angle", formatDegrees(result.diagonalDeg))
                HorizontalDivider()
                ResultRow(
                    "Sensor size",
                    formatOneDecimal(result.sensorWidthMm) + " x " +
                        formatOneDecimal(result.sensorHeightMm) + " mm",
                )
            }
        } else {
            CalculatorHint("Enter a focal length and crop factor (1.0 = full frame).")
        }
    }
}
