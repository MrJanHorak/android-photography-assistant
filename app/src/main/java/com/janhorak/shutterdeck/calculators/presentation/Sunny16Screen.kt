package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.janhorak.shutterdeck.calculators.domain.LightingCondition
import com.janhorak.shutterdeck.calculators.domain.reciprocityCorrectedSeconds
import com.janhorak.shutterdeck.calculators.domain.sunny16ShutterSeconds
import com.janhorak.shutterdeck.metering.domain.formatExposureTime
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader

@Composable
fun Sunny16Screen(modifier: Modifier = Modifier) {
    var iso by rememberInput("100")
    var aperture by rememberInput("16")
    var conditionName by rememberSaveable { mutableStateOf(LightingCondition.SUNNY.name) }
    var reciprocityExponent by rememberInput("1.0")

    val condition = LightingCondition.valueOf(conditionName)
    val isoValue = iso.toIntOrNull()
    val apertureValue = aperture.toDoubleOrNull()
    val shutter = if (isoValue != null && apertureValue != null) {
        sunny16ShutterSeconds(isoValue, apertureValue, condition.stopsDarkerThanSunny)
    } else {
        null
    }
    val exponent = reciprocityExponent.toDoubleOrNull()
    val corrected = if (shutter != null && exponent != null) {
        reciprocityCorrectedSeconds(shutter, exponent)
    } else {
        null
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Sunny 16 & reciprocity",
            subtitle = "A starting exposure from daylight, plus long-exposure film correction.",
        )
        LabeledField("Film / sensor ISO", iso, { iso = it })
        LabeledField("Aperture (f-number)", aperture, { aperture = it }, suffix = "f/")

        LightingChips(selected = condition, onSelect = { conditionName = it.name })

        LabeledField(
            label = "Reciprocity exponent (film)",
            value = reciprocityExponent,
            onValueChange = { reciprocityExponent = it },
        )

        if (shutter != null) {
            ResultCard {
                ResultRow("Suggested shutter", formatExposureTime(shutter))
                if (corrected != null && corrected != shutter) {
                    HorizontalDivider()
                    ResultRow("Reciprocity-corrected", formatExposureTime(corrected))
                }
            }
        } else {
            CalculatorHint("Enter ISO and aperture, then pick the lighting condition.")
        }
    }
}

@Composable
private fun LightingChips(
    selected: LightingCondition,
    onSelect: (LightingCondition) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LightingCondition.entries.forEach { condition ->
            FilterChip(
                selected = condition == selected,
                onClick = { onSelect(condition) },
                label = { Text(condition.label) },
            )
        }
    }
}
