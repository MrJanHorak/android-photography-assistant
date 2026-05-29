package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.janhorak.shutterdeck.calculators.domain.calculateDepthOfField
import com.janhorak.shutterdeck.calculators.domain.circleOfConfusionMm
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

@Composable
fun DepthOfFieldScreen(modifier: Modifier = Modifier) {
    var focalLength by rememberInput("50")
    var aperture by rememberInput("8")
    var focusDistance by rememberInput("5")
    var cropFactor by rememberInput("1.0")

    val focal = focalLength.toDoubleOrNull()
    val n = aperture.toDoubleOrNull()
    val distance = focusDistance.toDoubleOrNull()
    val crop = cropFactor.toDoubleOrNull()

    val result = if (focal != null && n != null && distance != null && crop != null && crop > 0) {
        calculateDepthOfField(focal, n, distance, circleOfConfusionMm(crop))
    } else {
        null
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Depth of field",
            subtitle = "Near/far limits and hyperfocal distance for your focus point.",
        )
        LabeledField("Focal length", focalLength, { focalLength = it }, suffix = "mm")
        LabeledField("Aperture (f-number)", aperture, { aperture = it }, suffix = "f/")
        LabeledField("Focus distance", focusDistance, { focusDistance = it }, suffix = "m")
        LabeledField("Crop factor", cropFactor, { cropFactor = it }, suffix = "x")

        if (result != null) {
            ResultCard {
                ResultRow("Total depth of field", formatDistanceSmart(result.totalMeters))
                HorizontalDivider()
                ResultRow("Near limit", formatDistanceSmart(result.nearMeters))
                ResultRow("Far limit", formatDistanceSmart(result.farMeters))
                ResultRow("In front of subject", formatDistanceSmart(result.inFrontMeters))
                ResultRow("Behind subject", formatDistanceSmart(result.behindMeters))
                HorizontalDivider()
                ResultRow("Hyperfocal distance", formatDistanceSmart(result.hyperfocalMeters))
            }
        } else {
            CalculatorHint("Enter focal length, aperture, focus distance and crop factor.")
        }
    }
}
