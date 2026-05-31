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
import com.janhorak.shutterdeck.metering.domain.IlluminanceConversion
import com.janhorak.shutterdeck.metering.domain.convertIlluminanceFromEv100
import com.janhorak.shutterdeck.metering.domain.convertIlluminanceFromFootCandles
import com.janhorak.shutterdeck.metering.domain.convertIlluminanceFromLux
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader
import java.util.Locale

private enum class IlluminanceSource(
    val label: String,
    val fieldLabel: String,
    val suffix: String?,
    val subtitle: String,
) {
    EV100(
        label = "EV100",
        fieldLabel = "Ambient EV at ISO 100",
        suffix = null,
        subtitle = "Negative EV values are valid for very dim scenes.",
    ),
    LUX(
        label = "Lux",
        fieldLabel = "Ambient illuminance",
        suffix = "lx",
        subtitle = "Use a positive lux reading from a meter, phone or lighting spec sheet.",
    ),
    FOOT_CANDLES(
        label = "Foot-candles",
        fieldLabel = "Ambient illuminance",
        suffix = "fc",
        subtitle = "Use a positive foot-candle reading for imperial lighting references.",
    ),
}

@Composable
fun EvLuxConverterScreen(modifier: Modifier = Modifier) {
    var selectedSourceName by rememberSaveable { mutableStateOf(IlluminanceSource.LUX.name) }
    var ev100Text by rememberInput("9")
    var luxText by rememberInput("1280")
    var footCandlesText by rememberInput("118.9")

    val selectedSource = IlluminanceSource.valueOf(selectedSourceName)
    val sourceInputText = when (selectedSource) {
        IlluminanceSource.EV100 -> ev100Text
        IlluminanceSource.LUX -> luxText
        IlluminanceSource.FOOT_CANDLES -> footCandlesText
    }
    val parsedValue = sourceInputText.toDoubleOrNull()

    val validationMessage = remember(selectedSource, sourceInputText, parsedValue) {
        buildValidationMessage(
            source = selectedSource,
            sourceInputText = sourceInputText,
            parsedValue = parsedValue,
        )
    }
    val conversion = remember(selectedSource, parsedValue, validationMessage) {
        if (validationMessage != null) {
            null
        } else {
            when (selectedSource) {
                IlluminanceSource.EV100 -> convertIlluminanceFromEv100(parsedValue ?: 0.0)
                IlluminanceSource.LUX -> convertIlluminanceFromLux(parsedValue ?: 0.0)
                IlluminanceSource.FOOT_CANDLES -> convertIlluminanceFromFootCandles(parsedValue ?: 0.0)
            }
        }
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "EV / lux / foot-candle converter",
            subtitle = "Translate ambient EV100, lux and foot-candles with the same ISO 100 convention used by the light meter.",
        )
        CalculatorHint("Ambient EV here means EV at ISO 100 and does not apply your meter calibration offset.")
        CalculatorHint("1 foot-candle = 10.7639 lux.")
        SourceSelectionCard(
            selectedSource = selectedSource,
            onSelect = { selectedSourceName = it.name },
        )
        InputCard(
            source = selectedSource,
            value = sourceInputText,
            onValueChange = { updatedText ->
                when (selectedSource) {
                    IlluminanceSource.EV100 -> ev100Text = updatedText
                    IlluminanceSource.LUX -> luxText = updatedText
                    IlluminanceSource.FOOT_CANDLES -> footCandlesText = updatedText
                }
            },
        )
        if (conversion == null) {
            CalculatorHint(
                validationMessage ?: "Enter a source value to convert between ambient EV100, lux and foot-candles.",
            )
        } else {
            ConversionResultCard(conversion = conversion)
        }
    }
}

@Composable
private fun SourceSelectionCard(
    selectedSource: IlluminanceSource,
    onSelect: (IlluminanceSource) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = "Convert from",
                subtitle = "Pick the unit you want to type so the other two stay read-only and stable.",
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IlluminanceSource.entries.forEach { source ->
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
    source: IlluminanceSource,
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
private fun ConversionResultCard(conversion: IlluminanceConversion) {
    ResultCard {
        Text(
            text = "Converted values",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ResultRow(
            label = "Ambient EV100",
            value = formatEv100(conversion.ev100),
        )
        HorizontalDivider()
        ResultRow(
            label = "Lux",
            value = formatIlluminance(conversion.lux, "lx"),
        )
        HorizontalDivider()
        ResultRow(
            label = "Foot-candles",
            value = formatIlluminance(conversion.footCandles, "fc"),
        )
    }
}

private fun buildValidationMessage(
    source: IlluminanceSource,
    sourceInputText: String,
    parsedValue: Double?,
): String? = when {
    sourceInputText.isBlank() -> "Choose a source unit and enter a value."
    parsedValue == null -> "Enter a valid number."
    !parsedValue.isFinite() -> "Enter a finite number."
    source != IlluminanceSource.EV100 && parsedValue <= 0.0 -> "Lux and foot-candles must be greater than zero."
    else -> null
}

private fun formatEv100(value: Double): String =
    String.format(Locale.getDefault(), "%.2f EV", value)

private fun formatIlluminance(value: Double, unit: String): String = when {
    value >= 1000.0 -> String.format(Locale.getDefault(), "%,.0f %s", value, unit)
    value >= 100.0 -> String.format(Locale.getDefault(), "%,.1f %s", value, unit)
    else -> String.format(Locale.getDefault(), "%.2f %s", value, unit)
}
