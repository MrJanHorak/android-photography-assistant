package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.calculators.domain.ColorTemperatureConversion
import com.janhorak.shutterdeck.calculators.domain.convertColorTemperatureFromKelvin
import com.janhorak.shutterdeck.calculators.domain.convertColorTemperatureFromMired
import com.janhorak.shutterdeck.calculators.domain.miredFromKelvin
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader
import java.util.Locale

private enum class ColorTemperatureSource(
    val label: String,
    val fieldLabel: String,
    val suffix: String,
    val subtitle: String,
) {
    KELVIN(
        label = "Kelvin",
        fieldLabel = "Color temperature",
        suffix = "K",
        subtitle = "Use camera, light, or gel spec sheet color temperature in Kelvin.",
    ),
    MIRED(
        label = "Mired",
        fieldLabel = "Micro reciprocal degrees",
        suffix = "M",
        subtitle = "Use mired values when matching white-balance shifts or lighting references.",
    ),
}

private data class WhiteBalanceReference(
    val label: String,
    val kelvin: Double,
)

private val commonWhiteBalanceReferences = listOf(
    WhiteBalanceReference("Candle / flame", 1900.0),
    WhiteBalanceReference("Tungsten / halogen", 3200.0),
    WhiteBalanceReference("Fluorescent", 4000.0),
    WhiteBalanceReference("Daylight / flash", 5600.0),
    WhiteBalanceReference("Overcast", 6500.0),
    WhiteBalanceReference("Shade", 7500.0),
)

@Composable
fun ColorTemperatureScreen(modifier: Modifier = Modifier) {
    var selectedSourceName by rememberSaveable { mutableStateOf(ColorTemperatureSource.KELVIN.name) }
    var inputText by rememberInput("5600")

    val selectedSource = ColorTemperatureSource.valueOf(selectedSourceName)
    val parsedValue = inputText.toDoubleOrNull()
    val validationMessage = remember(selectedSource, inputText, parsedValue) {
        buildValidationMessage(inputText, parsedValue)
    }
    val conversion = remember(selectedSource, parsedValue, validationMessage) {
        if (validationMessage != null) {
            null
        } else {
            when (selectedSource) {
                ColorTemperatureSource.KELVIN -> convertColorTemperatureFromKelvin(parsedValue ?: 0.0)
                ColorTemperatureSource.MIRED -> convertColorTemperatureFromMired(parsedValue ?: 0.0)
            }
        }
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Color temperature",
            subtitle = "Convert white-balance values between Kelvin and mired without drifting into gel math.",
        )
        CalculatorHint("Mired means micro reciprocal degrees: mired = 1,000,000 / kelvin.")
        CalculatorHint("Switching units carries the current converted value into the input field so you can keep working from either side.")
        SourceSelectionCard(
            selectedSource = selectedSource,
            onSelect = { nextSource ->
                if (nextSource == selectedSource) return@SourceSelectionCard
                inputText = conversion?.let { formatEditableInput(nextSource, it) } ?: inputText
                selectedSourceName = nextSource.name
            },
        )
        InputCard(
            source = selectedSource,
            value = inputText,
            onValueChange = { inputText = it },
        )
        if (conversion == null) {
            CalculatorHint(validationMessage ?: "Enter a Kelvin or mired value to convert it.")
        } else {
            ConversionResultCard(
                source = selectedSource,
                conversion = conversion,
            )
        }
        WhiteBalanceReferenceCard()
    }
}

@Composable
private fun SourceSelectionCard(
    selectedSource: ColorTemperatureSource,
    onSelect: (ColorTemperatureSource) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = "Convert from",
                subtitle = "Choose the unit you want to type, then read the converted result below.",
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColorTemperatureSource.entries.forEach { source ->
                    FilterChip(
                        selected = source == selectedSource,
                        onClick = { onSelect(source) },
                        label = { Text(source.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InputCard(
    source: ColorTemperatureSource,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = source.label,
                subtitle = source.subtitle,
            )
            LabeledField(
                label = source.fieldLabel,
                value = value,
                onValueChange = onValueChange,
                suffix = source.suffix,
                keyboardType = KeyboardType.Decimal,
            )
        }
    }
}

@Composable
private fun ConversionResultCard(
    source: ColorTemperatureSource,
    conversion: ColorTemperatureConversion,
) {
    val targetLabel = if (source == ColorTemperatureSource.KELVIN) "Mired" else "Kelvin"
    val targetValue = if (source == ColorTemperatureSource.KELVIN) {
        formatMired(conversion.mired)
    } else {
        formatKelvin(conversion.kelvin)
    }

    ResultCard {
        Text(
            text = "Converted value",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ResultRow(
            label = targetLabel,
            value = targetValue,
        )
    }
}

@Composable
private fun WhiteBalanceReferenceCard() {
    ResultCard {
        Text(
            text = "Common WB anchors",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        commonWhiteBalanceReferences.forEachIndexed { index, reference ->
            ResultRow(
                label = reference.label,
                value = "${formatKelvin(reference.kelvin)} · ${formatMired(miredFromKelvin(reference.kelvin) ?: 0.0)}",
            )
            if (index != commonWhiteBalanceReferences.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

private fun buildValidationMessage(
    inputText: String,
    parsedValue: Double?,
): String? = when {
    inputText.isBlank() -> "Choose Kelvin or mired and enter a value."
    parsedValue == null -> "Enter a valid number."
    !parsedValue.isFinite() -> "Enter a finite number."
    parsedValue <= 0.0 -> "Kelvin and mired values must be greater than zero."
    else -> null
}

private fun formatEditableInput(
    source: ColorTemperatureSource,
    conversion: ColorTemperatureConversion,
): String = when (source) {
    ColorTemperatureSource.KELVIN -> trimFormattedNumber(conversion.kelvin, 2)
    ColorTemperatureSource.MIRED -> trimFormattedNumber(conversion.mired, 3)
}

private fun formatKelvin(value: Double): String =
    String.format(Locale.getDefault(), "%,.0f K", value)

private fun formatMired(value: Double): String =
    String.format(Locale.getDefault(), "%.1f M", value)

private fun trimFormattedNumber(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value).trimEnd('0').trimEnd('.')
