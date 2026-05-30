package com.janhorak.shutterdeck.film.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.FilmRollEntity
import com.janhorak.shutterdeck.film.domain.DilutionCalculation
import com.janhorak.shutterdeck.film.domain.FilmDevelopmentStep
import com.janhorak.shutterdeck.film.domain.buildDevelopmentRecipeSteps
import com.janhorak.shutterdeck.film.domain.calculateDilution
import com.janhorak.shutterdeck.film.domain.developmentTemperatureFactor
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.SectionHeader
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FilmDevelopmentScreen(
    modifier: Modifier = Modifier,
    viewModel: FilmDevelopmentViewModel = hiltViewModel(),
) {
    val rolls by viewModel.rolls.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()

    var selectedRollId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dilutionWaterPartText by rememberSaveable { mutableStateOf("31") }
    var totalSolutionMlText by rememberSaveable { mutableStateOf("500") }
    var developerTimeMinutesText by rememberSaveable { mutableStateOf("") }
    var chemistryTemperatureCText by rememberSaveable { mutableStateOf("20") }
    var agitationIntervalSecondsText by rememberSaveable { mutableStateOf("60") }
    var preSoakSecondsText by rememberSaveable { mutableStateOf("0") }
    var stopBathSecondsText by rememberSaveable { mutableStateOf("30") }
    var fixerSecondsText by rememberSaveable { mutableStateOf("300") }
    var washSecondsText by rememberSaveable { mutableStateOf("600") }

    val selectedRoll = remember(rolls, selectedRollId) {
        rolls.firstOrNull { roll -> roll.id == selectedRollId }
    }
    val previewContextLabel = selectedRoll?.let(::buildRollContextLabel)

    val dilutionWaterPart = dilutionWaterPartText.toDoubleOrNull()?.takeIf { value -> value > 0.0 }
    val totalSolutionMl = totalSolutionMlText.toDoubleOrNull()?.takeIf { value -> value > 0.0 }
    val dilutionResult = remember(dilutionWaterPart, totalSolutionMl) {
        if (dilutionWaterPart != null && totalSolutionMl != null) {
            calculateDilution(
                waterParts = dilutionWaterPart,
                totalMilliliters = totalSolutionMl,
            )
        } else {
            null
        }
    }

    val developerBaseSecondsAt20C = developerTimeMinutesText
        .toDoubleOrNull()
        ?.takeIf { minutes -> minutes > 0.0 }
        ?.let { minutes -> (minutes * 60.0).roundToInt() }
    val chemistryTemperatureC = chemistryTemperatureCText.toDoubleOrNull()
    val agitationIntervalSeconds = agitationIntervalSecondsText.toIntOrNull()?.takeIf { value -> value >= 0 }
    val preSoakSeconds = preSoakSecondsText.toIntOrNull()?.takeIf { value -> value >= 0 }
    val stopBathSeconds = stopBathSecondsText.toIntOrNull()?.takeIf { value -> value >= 0 }
    val fixerSeconds = fixerSecondsText.toIntOrNull()?.takeIf { value -> value >= 0 }
    val washSeconds = washSecondsText.toIntOrNull()?.takeIf { value -> value >= 0 }

    val previewSteps = remember(
        developerBaseSecondsAt20C,
        chemistryTemperatureC,
        agitationIntervalSeconds,
        preSoakSeconds,
        stopBathSeconds,
        fixerSeconds,
        washSeconds,
    ) {
        if (
            developerBaseSecondsAt20C != null &&
            chemistryTemperatureC != null &&
            agitationIntervalSeconds != null &&
            preSoakSeconds != null &&
            stopBathSeconds != null &&
            fixerSeconds != null &&
            washSeconds != null
        ) {
            buildDevelopmentRecipeSteps(
                preSoakSeconds = preSoakSeconds,
                developerBaseSecondsAt20C = developerBaseSecondsAt20C,
                chemistryTemperatureC = chemistryTemperatureC,
                stopBathSeconds = stopBathSeconds,
                fixerSeconds = fixerSeconds,
                washSeconds = washSeconds,
                agitationIntervalSeconds = agitationIntervalSeconds,
            )
        } else {
            null
        }
    }

    val recipeValidationMessage = buildRecipeValidationMessage(
        developerTimeMinutesText = developerTimeMinutesText,
        developerBaseSecondsAt20C = developerBaseSecondsAt20C,
        chemistryTemperatureCText = chemistryTemperatureCText,
        chemistryTemperatureC = chemistryTemperatureC,
        agitationIntervalSecondsText = agitationIntervalSecondsText,
        agitationIntervalSeconds = agitationIntervalSeconds,
        preSoakSecondsText = preSoakSecondsText,
        preSoakSeconds = preSoakSeconds,
        stopBathSecondsText = stopBathSecondsText,
        stopBathSeconds = stopBathSeconds,
        fixerSecondsText = fixerSecondsText,
        fixerSeconds = fixerSeconds,
        washSecondsText = washSecondsText,
        washSeconds = washSeconds,
        previewSteps = previewSteps,
    )

    Column(modifier = modifier.fillMaxSize()) {
        FilmDevelopmentTimerStatusCard(
            timerState = timerState,
            previewSteps = previewSteps,
            previewContextLabel = previewContextLabel,
            onStartOrRestart = {
                previewSteps?.let { steps ->
                    viewModel.startTimer(
                        steps = steps,
                        contextLabel = previewContextLabel,
                    )
                }
            },
            onPause = viewModel::pauseTimer,
            onResume = viewModel::resumeTimer,
            onReset = viewModel::resetTimer,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionHeader(
                    title = "Development workflow",
                    subtitle = "Use saved roll context, mix chemistry, compensate for temperature and run a guided darkroom timer.",
                )
            }
            item {
                RollContextCard(
                    rolls = rolls,
                    selectedRollId = selectedRollId,
                    onSelectRoll = { selectedRollId = it },
                )
            }
            item {
                DilutionCalculatorCard(
                    waterPartText = dilutionWaterPartText,
                    onWaterPartChange = { dilutionWaterPartText = it },
                    totalSolutionMlText = totalSolutionMlText,
                    onTotalSolutionChange = { totalSolutionMlText = it },
                    result = dilutionResult,
                )
            }
            item {
                DevelopmentRecipeCard(
                    developerTimeMinutesText = developerTimeMinutesText,
                    onDeveloperTimeMinutesChange = { developerTimeMinutesText = it },
                    chemistryTemperatureCText = chemistryTemperatureCText,
                    onChemistryTemperatureChange = { chemistryTemperatureCText = it },
                    agitationIntervalSecondsText = agitationIntervalSecondsText,
                    onAgitationIntervalSecondsChange = { agitationIntervalSecondsText = it },
                    preSoakSecondsText = preSoakSecondsText,
                    onPreSoakSecondsChange = { preSoakSecondsText = it },
                    stopBathSecondsText = stopBathSecondsText,
                    onStopBathSecondsChange = { stopBathSecondsText = it },
                    fixerSecondsText = fixerSecondsText,
                    onFixerSecondsChange = { fixerSecondsText = it },
                    washSecondsText = washSecondsText,
                    onWashSecondsChange = { washSecondsText = it },
                    previewSteps = previewSteps,
                    adjustedDeveloperSeconds = previewSteps
                        ?.firstOrNull { step -> step.name == "Developer" }
                        ?.durationSeconds,
                    validationMessage = recipeValidationMessage,
                )
            }
        }
    }
}

@Composable
private fun FilmDevelopmentTimerStatusCard(
    timerState: FilmDevelopmentTimerState,
    previewSteps: List<FilmDevelopmentStep>?,
    previewContextLabel: String?,
    onStartOrRestart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
) {
    val currentStep = timerState.currentStep
    val currentContextLabel = timerState.contextLabel ?: previewContextLabel
    val primaryButtonLabel = if (timerState.steps.isEmpty() || timerState.isComplete) {
        "Start recipe"
    } else {
        "Restart recipe"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = when {
                    timerState.isComplete -> "Recipe complete"
                    currentStep != null -> currentStep.name
                    else -> "Ready for a darkroom recipe"
                },
                style = MaterialTheme.typography.titleLarge,
            )
            currentContextLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = when {
                    currentStep != null -> "${formatTimerDuration(timerState.remainingSeconds)} remaining in step ${timerState.currentStepIndex + 1} of ${timerState.steps.size}"
                    previewSteps != null -> "${previewSteps.size} preview steps - total ${formatTimerDuration(previewSteps.sumOf { step -> step.durationSeconds })}"
                    else -> "Enter a 20C developer time below to preview the recipe."
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            if (timerState.currentStep != null && !timerState.isComplete) {
                Text(
                    text = "Recipe remaining: ${formatTimerDuration(timerState.totalRemainingSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            currentStep?.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            timerState.cueText?.let { cue ->
                Text(
                    text = cue,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            timerState.statusText?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartOrRestart,
                    enabled = previewSteps != null,
                ) {
                    Text(primaryButtonLabel)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        timerState.isRunning -> {
                            OutlinedButton(onClick = onPause) {
                                Text("Pause")
                            }
                        }

                        timerState.isPaused -> {
                            OutlinedButton(onClick = onResume) {
                                Text("Resume")
                            }
                        }
                    }
                    if (timerState.steps.isNotEmpty() || timerState.isComplete) {
                        TextButton(onClick = onReset) {
                            Text("Reset timer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RollContextCard(
    rolls: List<FilmRollEntity>,
    selectedRollId: Long?,
    onSelectRoll: (Long?) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Roll context",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Optional: load a saved roll so the timer header keeps the stock, EI and camera context visible while you process it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (rolls.isEmpty()) {
                Text(
                    text = "No saved rolls yet. The development tools still work without linking to a roll.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedRollId == null,
                        onClick = { onSelectRoll(null) },
                        label = { Text("No roll") },
                    )
                    rolls.forEach { roll ->
                        FilterChip(
                            selected = selectedRollId == roll.id,
                            onClick = { onSelectRoll(roll.id) },
                            label = { Text(roll.displayTitle) },
                        )
                    }
                }
                rolls.firstOrNull { roll -> roll.id == selectedRollId }?.let { roll ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailLine(label = "Stock", value = roll.stockDisplayName)
                        DetailLine(label = "EI", value = roll.exposureIndex.toString())
                        DetailLine(label = "Camera", value = roll.cameraLabel)
                        DetailLine(label = "Lens", value = roll.lensLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun DilutionCalculatorCard(
    waterPartText: String,
    onWaterPartChange: (String) -> Unit,
    totalSolutionMlText: String,
    onTotalSolutionChange: (String) -> Unit,
    result: DilutionCalculation?,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Dilution calculator",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Enter a 1+N dilution and the total working-solution volume you want to mix.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Dilution",
                    value = waterPartText,
                    onValueChange = onWaterPartChange,
                    modifier = Modifier.weight(1f),
                    suffix = "1+N",
                    keyboardType = KeyboardType.Decimal,
                )
                LabeledField(
                    label = "Working solution",
                    value = totalSolutionMlText,
                    onValueChange = onTotalSolutionChange,
                    modifier = Modifier.weight(1f),
                    suffix = "ml",
                    keyboardType = KeyboardType.Decimal,
                )
            }
            if (result != null) {
                Text(
                    text = "Mix ${formatMilliliters(result.stockMilliliters)} ml concentrate with ${formatMilliliters(result.waterMilliliters)} ml water.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = "Results appear once both fields contain values greater than 0.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DevelopmentRecipeCard(
    developerTimeMinutesText: String,
    onDeveloperTimeMinutesChange: (String) -> Unit,
    chemistryTemperatureCText: String,
    onChemistryTemperatureChange: (String) -> Unit,
    agitationIntervalSecondsText: String,
    onAgitationIntervalSecondsChange: (String) -> Unit,
    preSoakSecondsText: String,
    onPreSoakSecondsChange: (String) -> Unit,
    stopBathSecondsText: String,
    onStopBathSecondsChange: (String) -> Unit,
    fixerSecondsText: String,
    onFixerSecondsChange: (String) -> Unit,
    washSecondsText: String,
    onWashSecondsChange: (String) -> Unit,
    previewSteps: List<FilmDevelopmentStep>?,
    adjustedDeveloperSeconds: Int?,
    validationMessage: String?,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Recipe + timer setup",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Developer time is entered at 20C. The preview below applies temperature compensation and builds the guided step sequence.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Developer time",
                    value = developerTimeMinutesText,
                    onValueChange = onDeveloperTimeMinutesChange,
                    modifier = Modifier.weight(1f),
                    suffix = "min @ 20C",
                    keyboardType = KeyboardType.Decimal,
                )
                LabeledField(
                    label = "Chemistry temp",
                    value = chemistryTemperatureCText,
                    onValueChange = onChemistryTemperatureChange,
                    modifier = Modifier.weight(1f),
                    suffix = "C",
                    keyboardType = KeyboardType.Decimal,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Agitation interval",
                    value = agitationIntervalSecondsText,
                    onValueChange = onAgitationIntervalSecondsChange,
                    modifier = Modifier.weight(1f),
                    suffix = "sec",
                )
                LabeledField(
                    label = "Pre-soak",
                    value = preSoakSecondsText,
                    onValueChange = onPreSoakSecondsChange,
                    modifier = Modifier.weight(1f),
                    suffix = "sec",
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Stop bath",
                    value = stopBathSecondsText,
                    onValueChange = onStopBathSecondsChange,
                    modifier = Modifier.weight(1f),
                    suffix = "sec",
                )
                LabeledField(
                    label = "Fixer",
                    value = fixerSecondsText,
                    onValueChange = onFixerSecondsChange,
                    modifier = Modifier.weight(1f),
                    suffix = "sec",
                )
                LabeledField(
                    label = "Wash",
                    value = washSecondsText,
                    onValueChange = onWashSecondsChange,
                    modifier = Modifier.weight(1f),
                    suffix = "sec",
                )
            }

            when {
                adjustedDeveloperSeconds != null -> {
                    Text(
                        text = "Compensated developer time: ${formatTimerDuration(adjustedDeveloperSeconds)}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                validationMessage != null -> {
                    Text(
                        text = validationMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (previewSteps != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Preview steps",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    previewSteps.forEachIndexed { index, step ->
                        StepPreviewRow(
                            index = index + 1,
                            step = step,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepPreviewRow(
    index: Int,
    step: FilmDevelopmentStep,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "$index. ${step.name}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = formatTimerDuration(step.durationSeconds),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (step.agitationIntervalSeconds > 0) {
                Text(
                    text = "Cue every ${step.agitationIntervalSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            step.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun buildRecipeValidationMessage(
    developerTimeMinutesText: String,
    developerBaseSecondsAt20C: Int?,
    chemistryTemperatureCText: String,
    chemistryTemperatureC: Double?,
    agitationIntervalSecondsText: String,
    agitationIntervalSeconds: Int?,
    preSoakSecondsText: String,
    preSoakSeconds: Int?,
    stopBathSecondsText: String,
    stopBathSeconds: Int?,
    fixerSecondsText: String,
    fixerSeconds: Int?,
    washSecondsText: String,
    washSeconds: Int?,
    previewSteps: List<FilmDevelopmentStep>?,
): String? = when {
    developerTimeMinutesText.isBlank() -> "Enter the developer time you use at 20C to preview the recipe."
    developerBaseSecondsAt20C == null -> "Developer time must be greater than 0."
    chemistryTemperatureCText.isBlank() -> "Enter the chemistry temperature you are using."
    chemistryTemperatureC == null -> "Chemistry temperature must be numeric."
    developmentTemperatureFactor(chemistryTemperatureC) == null -> "Temperature compensation currently supports 16C to 26C."
    agitationIntervalSecondsText.isBlank() -> "Enter 0 for stand development or a recurring agitation interval in seconds."
    agitationIntervalSeconds == null -> "Agitation interval must be 0 or greater."
    preSoakSecondsText.isBlank() || preSoakSeconds == null -> "Pre-soak must be 0 or greater."
    stopBathSecondsText.isBlank() || stopBathSeconds == null -> "Stop bath time must be 0 or greater."
    fixerSecondsText.isBlank() || fixerSeconds == null -> "Fixer time must be 0 or greater."
    washSecondsText.isBlank() || washSeconds == null -> "Wash time must be 0 or greater."
    previewSteps == null -> "Check the recipe values and try again."
    else -> null
}

private fun buildRollContextLabel(roll: FilmRollEntity): String =
    listOf(
        roll.stockDisplayName,
        "EI ${roll.exposureIndex}",
        roll.cameraLabel.takeIf { value -> value.isNotBlank() },
    ).filterNotNull().joinToString(" - ")

private fun formatTimerDuration(totalSeconds: Int): String {
    val clampedSeconds = totalSeconds.coerceAtLeast(0)
    val hours = clampedSeconds / 3_600
    val minutes = (clampedSeconds % 3_600) / 60
    val seconds = clampedSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

private fun formatMilliliters(value: Double): String {
    val formatted = String.format(Locale.ROOT, "%.1f", value)
    return formatted.trimEnd('0').trimEnd('.')
}
