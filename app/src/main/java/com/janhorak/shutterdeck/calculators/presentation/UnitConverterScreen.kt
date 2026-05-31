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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.calculators.domain.DistanceConversion
import com.janhorak.shutterdeck.calculators.domain.TemperatureConversion
import com.janhorak.shutterdeck.calculators.domain.convertDistanceFromFeet
import com.janhorak.shutterdeck.calculators.domain.convertDistanceFromMeters
import com.janhorak.shutterdeck.calculators.domain.convertTemperatureFromCelsius
import com.janhorak.shutterdeck.calculators.domain.convertTemperatureFromFahrenheit
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader
import java.util.Locale

private enum class DistanceSource(
    val label: String,
    val suffix: String,
    val resultLabel: String,
    val subtitle: String,
) {
    METERS(
        label = "Meters",
        suffix = "m",
        resultLabel = "Feet",
        subtitle = "Use the exact international foot conversion for lens-to-subject or set distance checks.",
    ),
    FEET(
        label = "Feet",
        suffix = "ft",
        resultLabel = "Meters",
        subtitle = "Useful when translating imperial distances into metric field notes.",
    ),
}

private enum class TemperatureSource(
    val label: String,
    val suffix: String,
    val resultLabel: String,
    val subtitle: String,
) {
    CELSIUS(
        label = "Celsius",
        suffix = "°C",
        resultLabel = "Fahrenheit",
        subtitle = "Convert weather or storage temperatures from Celsius to Fahrenheit.",
    ),
    FAHRENHEIT(
        label = "Fahrenheit",
        suffix = "°F",
        resultLabel = "Celsius",
        subtitle = "Convert Fahrenheit readings back into Celsius for gear and shoot planning.",
    ),
}

@Composable
fun UnitConverterScreen(modifier: Modifier = Modifier) {
    var selectedDistanceSourceName by rememberSaveable { mutableStateOf(DistanceSource.METERS.name) }
    var distanceInputText by rememberInput("2")
    var selectedTemperatureSourceName by rememberSaveable { mutableStateOf(TemperatureSource.CELSIUS.name) }
    var temperatureInputText by rememberInput("20")

    val selectedDistanceSource = DistanceSource.valueOf(selectedDistanceSourceName)
    val parsedDistanceValue = distanceInputText.toDoubleOrNull()
    val distanceValidationMessage = remember(distanceInputText, parsedDistanceValue) {
        buildDistanceValidationMessage(
            inputText = distanceInputText,
            parsedValue = parsedDistanceValue,
        )
    }
    val distanceConversion = remember(
        selectedDistanceSource,
        parsedDistanceValue,
        distanceValidationMessage,
    ) {
        if (distanceValidationMessage != null) {
            null
        } else {
            when (selectedDistanceSource) {
                DistanceSource.METERS -> convertDistanceFromMeters(parsedDistanceValue ?: 0.0)
                DistanceSource.FEET -> convertDistanceFromFeet(parsedDistanceValue ?: 0.0)
            }
        }
    }

    val selectedTemperatureSource = TemperatureSource.valueOf(selectedTemperatureSourceName)
    val parsedTemperatureValue = temperatureInputText.toDoubleOrNull()
    val temperatureValidationMessage = remember(temperatureInputText, parsedTemperatureValue) {
        buildTemperatureValidationMessage(
            inputText = temperatureInputText,
            parsedValue = parsedTemperatureValue,
        )
    }
    val temperatureConversion = remember(
        selectedTemperatureSource,
        parsedTemperatureValue,
        temperatureValidationMessage,
    ) {
        if (temperatureValidationMessage != null) {
            null
        } else {
            when (selectedTemperatureSource) {
                TemperatureSource.CELSIUS -> convertTemperatureFromCelsius(parsedTemperatureValue ?: 0.0)
                TemperatureSource.FAHRENHEIT -> convertTemperatureFromFahrenheit(parsedTemperatureValue ?: 0.0)
            }
        }
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Unit Converter",
            subtitle = "Quick field conversions for distance and temperature without leaving the Tools grid.",
        )
        CalculatorHint("Distance uses the exact international foot: 1 ft = 0.3048 m.")
        CalculatorHint("Temperature accepts negative values; use the +/- button if your keyboard hides the minus sign.")
        DistanceConverterCard(
            selectedSource = selectedDistanceSource,
            value = distanceInputText,
            onSelect = { nextSource ->
                if (nextSource != selectedDistanceSource) {
                    distanceInputText = distanceConversion?.let { formatEditableDistance(nextSource, it) } ?: distanceInputText
                    selectedDistanceSourceName = nextSource.name
                }
            },
            onValueChange = { distanceInputText = it },
            validationMessage = distanceValidationMessage,
            conversion = distanceConversion,
        )
        TemperatureConverterCard(
            selectedSource = selectedTemperatureSource,
            value = temperatureInputText,
            onSelect = { nextSource ->
                if (nextSource != selectedTemperatureSource) {
                    temperatureInputText = temperatureConversion?.let { formatEditableTemperature(nextSource, it) } ?: temperatureInputText
                    selectedTemperatureSourceName = nextSource.name
                }
            },
            onValueChange = { temperatureInputText = it },
            onToggleSign = { temperatureInputText = toggleLeadingMinus(temperatureInputText) },
            validationMessage = temperatureValidationMessage,
            conversion = temperatureConversion,
        )
    }
}

@Composable
private fun DistanceConverterCard(
    selectedSource: DistanceSource,
    value: String,
    onSelect: (DistanceSource) -> Unit,
    onValueChange: (String) -> Unit,
    validationMessage: String?,
    conversion: DistanceConversion?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = "Distance",
                subtitle = selectedSource.subtitle,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DistanceSource.entries.forEach { source ->
                    FilterChip(
                        selected = source == selectedSource,
                        onClick = { onSelect(source) },
                        label = { Text(source.label) },
                    )
                }
            }
            LabeledField(
                label = "Distance",
                value = value,
                onValueChange = onValueChange,
                suffix = selectedSource.suffix,
                keyboardType = KeyboardType.Decimal,
            )
            HorizontalDivider()
            if (conversion == null) {
                Text(
                    text = validationMessage ?: "Enter a distance to convert it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ResultRow(
                    label = selectedSource.resultLabel,
                    value = when (selectedSource) {
                        DistanceSource.METERS -> formatDistance(conversion.feet, "ft")
                        DistanceSource.FEET -> formatDistance(conversion.meters, "m")
                    },
                )
            }
        }
    }
}

@Composable
private fun TemperatureConverterCard(
    selectedSource: TemperatureSource,
    value: String,
    onSelect: (TemperatureSource) -> Unit,
    onValueChange: (String) -> Unit,
    onToggleSign: () -> Unit,
    validationMessage: String?,
    conversion: TemperatureConversion?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = "Temperature",
                subtitle = selectedSource.subtitle,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TemperatureSource.entries.forEach { source ->
                    FilterChip(
                        selected = source == selectedSource,
                        onClick = { onSelect(source) },
                        label = { Text(source.label) },
                    )
                }
            }
            LabeledField(
                label = "Temperature",
                value = value,
                onValueChange = onValueChange,
                suffix = selectedSource.suffix,
                keyboardType = KeyboardType.Decimal,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onToggleSign) {
                    Text(if (value.startsWith("-")) "Make positive" else "Toggle +/-")
                }
            }
            HorizontalDivider()
            if (conversion == null) {
                Text(
                    text = validationMessage ?: "Enter a temperature to convert it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ResultRow(
                    label = selectedSource.resultLabel,
                    value = when (selectedSource) {
                        TemperatureSource.CELSIUS -> formatTemperature(conversion.fahrenheit, "°F")
                        TemperatureSource.FAHRENHEIT -> formatTemperature(conversion.celsius, "°C")
                    },
                )
            }
        }
    }
}

private fun buildDistanceValidationMessage(
    inputText: String,
    parsedValue: Double?,
): String? = when {
    inputText.isBlank() -> "Choose meters or feet and enter a value."
    parsedValue == null -> "Enter a valid number."
    !parsedValue.isFinite() -> "Enter a finite number."
    parsedValue < 0.0 -> "Distance cannot be negative."
    else -> null
}

private fun buildTemperatureValidationMessage(
    inputText: String,
    parsedValue: Double?,
): String? = when {
    inputText.isBlank() -> "Choose Celsius or Fahrenheit and enter a value."
    parsedValue == null -> "Enter a valid number."
    !parsedValue.isFinite() -> "Enter a finite number."
    else -> null
}

private fun formatEditableDistance(
    source: DistanceSource,
    conversion: DistanceConversion,
): String = when (source) {
    DistanceSource.METERS -> trimFormattedNumber(conversion.meters, 3)
    DistanceSource.FEET -> trimFormattedNumber(conversion.feet, 3)
}

private fun formatEditableTemperature(
    source: TemperatureSource,
    conversion: TemperatureConversion,
): String = when (source) {
    TemperatureSource.CELSIUS -> trimFormattedNumber(conversion.celsius, 2)
    TemperatureSource.FAHRENHEIT -> trimFormattedNumber(conversion.fahrenheit, 2)
}

private fun formatDistance(value: Double, unit: String): String =
    "${trimFormattedNumber(value, 3)} $unit"

private fun formatTemperature(value: Double, unit: String): String =
    "${trimFormattedNumber(value, 1)} $unit"

private fun toggleLeadingMinus(text: String): String = when {
    text.startsWith("-") -> text.removePrefix("-")
    text.isBlank() -> "-"
    else -> "-$text"
}

private fun trimFormattedNumber(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value).trimEnd('0').trimEnd('.')
