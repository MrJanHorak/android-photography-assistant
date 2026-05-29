package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.janhorak.shutterdeck.calculators.domain.FULL_FRAME_COC_MM
import com.janhorak.shutterdeck.calculators.domain.calculateFocusStack
import com.janhorak.shutterdeck.calculators.domain.circleOfConfusionMm
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

@Composable
fun FocusStackingScreen(modifier: Modifier = Modifier) {
    var focalLength by rememberInput("100")
    var aperture by rememberInput("8")
    var nearDistance by rememberInput("1")
    var farDistance by rememberInput("3")
    var cropFactor by rememberInput("1.0")

    val focal = focalLength.toDoubleOrNull()
    val n = aperture.toDoubleOrNull()
    val near = nearDistance.toDoubleOrNull()
    val far = farDistance.toDoubleOrNull()
    val crop = cropFactor.toDoubleOrNull()

    val plan = if (focal != null && n != null && near != null && far != null && crop != null && crop > 0) {
        calculateFocusStack(focal, n, near, far, circleOfConfusionMm(crop))
    } else {
        null
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Focus stacking",
            subtitle = "How many frames and which focus distances cover your near-to-far range.",
        )
        LabeledField("Focal length", focalLength, { focalLength = it }, suffix = "mm")
        LabeledField("Aperture (f-number)", aperture, { aperture = it }, suffix = "f/")
        LabeledField("Nearest point", nearDistance, { nearDistance = it }, suffix = "m")
        LabeledField("Farthest point", farDistance, { farDistance = it }, suffix = "m")
        LabeledField("Crop factor", cropFactor, { cropFactor = it }, suffix = "x")

        if (plan != null) {
            ResultCard {
                ResultRow("Frames needed", plan.frameCount.toString())
                HorizontalDivider()
                plan.focusDistancesMeters.forEachIndexed { index, distance ->
                    ResultRow("Frame ${index + 1} focus", formatDistanceSmart(distance))
                }
            }
        } else {
            CalculatorHint("Enter focal length, aperture, near & far distances (far > near) and crop factor.")
        }
    }
}
