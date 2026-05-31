package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.calculators.domain.CondensationRisk
import com.janhorak.shutterdeck.calculators.domain.DewPointAnalysis
import com.janhorak.shutterdeck.calculators.domain.analyzeCondensationRiskFromCelsius
import com.janhorak.shutterdeck.calculators.domain.analyzeCondensationRiskFromFahrenheit
import com.janhorak.shutterdeck.calculators.domain.celsiusFromFahrenheit
import com.janhorak.shutterdeck.calculators.domain.fahrenheitFromCelsius
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader
import java.util.Locale

private enum class TemperatureUnit(
    val label: String,
    val symbol: String,
) {
    CELSIUS(label = "Celsius", symbol = "°C"),
    FAHRENHEIT(label = "Fahrenheit", symbol = "°F"),
}

private data class RiskCopy(
    val title: String,
    val description: String,
)

@Composable
fun DewPointScreen(modifier: Modifier = Modifier) {
    var selectedUnitName by rememberSaveable { mutableStateOf(TemperatureUnit.CELSIUS.name) }
    var airTemperatureText by rememberInput("20")
    var relativeHumidityText by rememberInput("60")
    var surfaceTemperatureText by rememberInput("14")

    val selectedUnit = TemperatureUnit.valueOf(selectedUnitName)
    val parsedAirTemperature = airTemperatureText.toDoubleOrNull()
    val parsedRelativeHumidity = relativeHumidityText.toDoubleOrNull()
    val parsedSurfaceTemperature = surfaceTemperatureText.toDoubleOrNull()

    val validationMessage = remember(
        airTemperatureText,
        parsedAirTemperature,
        relativeHumidityText,
        parsedRelativeHumidity,
        surfaceTemperatureText,
        parsedSurfaceTemperature,
    ) {
        buildValidationMessage(
            airTemperatureText = airTemperatureText,
            parsedAirTemperature = parsedAirTemperature,
            relativeHumidityText = relativeHumidityText,
            parsedRelativeHumidity = parsedRelativeHumidity,
            surfaceTemperatureText = surfaceTemperatureText,
            parsedSurfaceTemperature = parsedSurfaceTemperature,
        )
    }

    val analysis = remember(
        selectedUnit,
        parsedAirTemperature,
        parsedRelativeHumidity,
        parsedSurfaceTemperature,
        validationMessage,
    ) {
        if (validationMessage != null) {
            null
        } else {
            when (selectedUnit) {
                TemperatureUnit.CELSIUS -> analyzeCondensationRiskFromCelsius(
                    airTemperatureCelsius = parsedAirTemperature!!,
                    relativeHumidityPercent = parsedRelativeHumidity!!,
                    surfaceTemperatureCelsius = parsedSurfaceTemperature!!,
                )
                TemperatureUnit.FAHRENHEIT -> analyzeCondensationRiskFromFahrenheit(
                    airTemperatureFahrenheit = parsedAirTemperature!!,
                    relativeHumidityPercent = parsedRelativeHumidity!!,
                    surfaceTemperatureFahrenheit = parsedSurfaceTemperature!!,
                )
            }
        }
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Dew Point",
            subtitle = "Compare air temperature, humidity, and your lens or gear temperature to spot fog and condensation risk.",
        )
        CalculatorHint("Condensation forms when the glass or camera body is at or below the ambient dew point.")
        CalculatorHint("Use the colder lens or body reading when gear moves between outdoor air, cars, air-conditioning, and indoor spaces.")
        EnvironmentCard(
            selectedUnit = selectedUnit,
            airTemperatureText = airTemperatureText,
            relativeHumidityText = relativeHumidityText,
            surfaceTemperatureText = surfaceTemperatureText,
            onUnitSelected = { nextUnit ->
                if (nextUnit == selectedUnit) return@EnvironmentCard
                airTemperatureText = parsedAirTemperature?.let {
                    formatEditableTemperature(convertAbsoluteTemperature(it, selectedUnit, nextUnit))
                } ?: airTemperatureText
                surfaceTemperatureText = parsedSurfaceTemperature?.let {
                    formatEditableTemperature(convertAbsoluteTemperature(it, selectedUnit, nextUnit))
                } ?: surfaceTemperatureText
                selectedUnitName = nextUnit.name
            },
            onAirTemperatureChange = { airTemperatureText = it },
            onRelativeHumidityChange = { relativeHumidityText = it },
            onSurfaceTemperatureChange = { surfaceTemperatureText = it },
            onAirTemperatureToggleSign = { airTemperatureText = toggleLeadingMinus(airTemperatureText) },
            onSurfaceTemperatureToggleSign = { surfaceTemperatureText = toggleLeadingMinus(surfaceTemperatureText) },
        )
        if (analysis == null) {
            CalculatorHint(validationMessage ?: "Enter air temperature, humidity, and lens / gear temperature to calculate dew point.")
        } else {
            CondensationResultCard(
                selectedUnit = selectedUnit,
                analysis = analysis,
            )
        }
    }
}

@Composable
private fun EnvironmentCard(
    selectedUnit: TemperatureUnit,
    airTemperatureText: String,
    relativeHumidityText: String,
    surfaceTemperatureText: String,
    onUnitSelected: (TemperatureUnit) -> Unit,
    onAirTemperatureChange: (String) -> Unit,
    onRelativeHumidityChange: (String) -> Unit,
    onSurfaceTemperatureChange: (String) -> Unit,
    onAirTemperatureToggleSign: () -> Unit,
    onSurfaceTemperatureToggleSign: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = "Environment",
                subtitle = "Enter the ambient air and current lens / gear readings in ${selectedUnit.label}.",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TemperatureUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = selectedUnit == unit,
                        onClick = { onUnitSelected(unit) },
                        label = { Text(unit.label) },
                    )
                }
            }
            SignedTemperatureField(
                label = "Air temperature",
                value = airTemperatureText,
                symbol = selectedUnit.symbol,
                onValueChange = onAirTemperatureChange,
                onToggleSign = onAirTemperatureToggleSign,
            )
            LabeledField(
                label = "Relative humidity",
                value = relativeHumidityText,
                onValueChange = onRelativeHumidityChange,
                suffix = "%",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.fillMaxWidth(),
            )
            SignedTemperatureField(
                label = "Lens / gear temperature",
                value = surfaceTemperatureText,
                symbol = selectedUnit.symbol,
                onValueChange = onSurfaceTemperatureChange,
                onToggleSign = onSurfaceTemperatureToggleSign,
            )
        }
    }
}

@Composable
private fun SignedTemperatureField(
    label: String,
    value: String,
    symbol: String,
    onValueChange: (String) -> Unit,
    onToggleSign: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabeledField(
                label = label,
                value = value,
                onValueChange = onValueChange,
                suffix = symbol,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleSign) {
                Text(if (value.startsWith("-")) "Make positive" else "Toggle +/-")
            }
        }
    }
}

@Composable
private fun CondensationResultCard(
    selectedUnit: TemperatureUnit,
    analysis: DewPointAnalysis,
) {
    val riskCopy = remember(analysis.risk) { buildRiskCopy(analysis.risk) }
    val dewPointDisplay = displayAbsoluteTemperature(analysis.dewPointCelsius, selectedUnit)
    val marginDisplay = displayTemperatureDelta(analysis.surfaceMarginCelsius, selectedUnit)
    val riskColor = when (analysis.risk) {
        CondensationRisk.ACTIVE -> MaterialTheme.colorScheme.error
        CondensationRisk.WARNING -> MaterialTheme.colorScheme.tertiary
        CondensationRisk.LOW -> MaterialTheme.colorScheme.primary
    }

    ResultCard {
        Text(
            text = riskCopy.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = riskColor,
        )
        Text(
            text = riskCopy.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        ResultRow(
            label = "Dew point",
            value = formatTemperature(dewPointDisplay, selectedUnit.symbol),
        )
        ResultRow(
            label = "Surface margin",
            value = formatMargin(marginDisplay, selectedUnit.symbol),
        )
        if (analysis.surfaceTemperatureCelsius < analysis.airTemperatureCelsius) {
            HorizontalDivider()
            Text(
                text = "Your lens / gear is cooler than the air, which is the most common fogging scenario when cold gear moves into warmer humidity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildRiskValidationMessage(
    relativeHumidityPercent: Double,
): String? = when {
    relativeHumidityPercent <= 0.0 || relativeHumidityPercent > 100.0 ->
        "Relative humidity must be greater than 0% and at most 100%."
    else -> null
}

private fun buildValidationMessage(
    airTemperatureText: String,
    parsedAirTemperature: Double?,
    relativeHumidityText: String,
    parsedRelativeHumidity: Double?,
    surfaceTemperatureText: String,
    parsedSurfaceTemperature: Double?,
): String? = when {
    airTemperatureText.isBlank() || relativeHumidityText.isBlank() || surfaceTemperatureText.isBlank() ->
        "Enter air temperature, relative humidity, and lens / gear temperature."
    parsedAirTemperature == null || parsedRelativeHumidity == null || parsedSurfaceTemperature == null ->
        "Enter valid numbers."
    !parsedAirTemperature.isFinite() || !parsedRelativeHumidity.isFinite() || !parsedSurfaceTemperature.isFinite() ->
        "Enter finite numbers."
    else -> buildRiskValidationMessage(parsedRelativeHumidity)
}

private fun buildRiskCopy(risk: CondensationRisk): RiskCopy = when (risk) {
    CondensationRisk.ACTIVE -> RiskCopy(
        title = "At or below the dew point",
        description = "Moisture or frost can form on the lens, filter, or camera body at this reading. Bag or acclimate the gear before moving deeper into humid air.",
    )
    CondensationRisk.WARNING -> RiskCopy(
        title = "Close to the dew point",
        description = "The surface is only slightly warmer than the dew point. A small drop in glass temperature or a more humid pocket of air can trigger fogging.",
    )
    CondensationRisk.LOW -> RiskCopy(
        title = "Low condensation risk",
        description = "You still have a buffer above the dew point. Re-check after moving between indoor, outdoor, vehicle, or air-conditioned spaces.",
    )
}

private fun convertAbsoluteTemperature(
    value: Double,
    from: TemperatureUnit,
    to: TemperatureUnit,
): Double = when {
    from == to -> value
    from == TemperatureUnit.CELSIUS && to == TemperatureUnit.FAHRENHEIT ->
        fahrenheitFromCelsius(value) ?: value
    else -> celsiusFromFahrenheit(value) ?: value
}

private fun displayAbsoluteTemperature(
    valueCelsius: Double,
    unit: TemperatureUnit,
): Double = when (unit) {
    TemperatureUnit.CELSIUS -> valueCelsius
    TemperatureUnit.FAHRENHEIT -> fahrenheitFromCelsius(valueCelsius) ?: valueCelsius
}

private fun displayTemperatureDelta(
    valueCelsius: Double,
    unit: TemperatureUnit,
): Double = when (unit) {
    TemperatureUnit.CELSIUS -> valueCelsius
    TemperatureUnit.FAHRENHEIT -> valueCelsius * 9.0 / 5.0
}

private fun formatEditableTemperature(value: Double): String =
    trimFormattedNumber(value, 2)

private fun formatTemperature(value: Double, unit: String): String =
    "${trimFormattedNumber(value, 1)} $unit"

private fun formatMargin(value: Double, unit: String): String {
    val relation = if (value >= 0.0) "above" else "below"
    return "${trimFormattedNumber(kotlin.math.abs(value), 1)} $unit $relation dew point"
}

private fun toggleLeadingMinus(text: String): String = when {
    text.startsWith("-") -> text.removePrefix("-")
    text.isBlank() -> "-"
    else -> "-$text"
}

private fun trimFormattedNumber(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value).trimEnd('0').trimEnd('.')
