package com.example.photography_helper.metering.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

@Composable
fun LightMeterScreen(
    modifier: Modifier = Modifier,
    viewModel: LightMeterViewModel = hiltViewModel()
) {
    val meteringState by viewModel.meteringState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val gearPreferences = remember(context) { GearSelectionPreferences(context) }
    val savedGearSelection = remember(gearPreferences) { gearPreferences.load() }

    var bodyId by rememberSaveable { mutableStateOf(savedGearSelection?.bodyId ?: cameraBodyProfiles.first().id) }
    var lensId by rememberSaveable { mutableStateOf(savedGearSelection?.lensId ?: lensProfiles.first().id) }
    var allowAdaptedLenses by rememberSaveable { mutableStateOf(savedGearSelection?.allowAdaptedLenses ?: false) }
    var stopModeName by rememberSaveable { mutableStateOf(ExposureStopMode.FULL.name) }
    var selectedAperture by rememberSaveable { mutableFloatStateOf(2.8f) }
    var selectedIso by rememberSaveable { mutableIntStateOf(100) }
    var calibrationOffset by rememberSaveable { mutableFloatStateOf(0f) }

    val body = remember(bodyId) { cameraBodyProfiles.firstOrNull { it.id == bodyId } ?: cameraBodyProfiles.first() }
    val stopMode = remember(stopModeName) { ExposureStopMode.valueOf(stopModeName) }
    val compatibleLenses = remember(body, allowAdaptedLenses) { compatibleLensProfiles(body, allowAdaptedLenses) }
    val lens = remember(lensId, compatibleLenses) {
        compatibleLenses.firstOrNull { it.id == lensId } ?: compatibleLenses.first()
    }
    val apertureOptions = remember(lens, stopMode) { lens.filterApertures(stopMode.apertureOptions) }
    val isoOptions = remember(body, stopMode) { body.filterIsos(stopMode.isoOptions) }

    LaunchedEffect(body.id, compatibleLenses, lensId) {
        if (compatibleLenses.none { it.id == lensId }) {
            lensId = compatibleLenses.first().id
        }
    }

    LaunchedEffect(body.id, lens.id, allowAdaptedLenses) {
        gearPreferences.save(
            SavedGearSelection(
                bodyId = body.id,
                lensId = lens.id,
                allowAdaptedLenses = allowAdaptedLenses,
            )
        )
    }

    LaunchedEffect(apertureOptions, selectedAperture) {
        if (apertureOptions.none { it.value == selectedAperture }) {
            selectedAperture = apertureOptions[nearestExposureIndex(apertureOptions, selectedAperture.toDouble())].value
        }
    }

    LaunchedEffect(isoOptions, selectedIso) {
        if (isoOptions.none { it.value == selectedIso }) {
            selectedIso = isoOptions[nearestExposureIndex(isoOptions, selectedIso.toDouble())].value
        }
    }

    val apertureIndex = nearestExposureIndex(apertureOptions, selectedAperture.toDouble())
    val isoIndex = nearestExposureIndex(isoOptions, selectedIso.toDouble())
    val aperture = apertureOptions[apertureIndex].value
    val iso = isoOptions[isoIndex].value

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startMetering()
                Lifecycle.Event.ON_PAUSE -> viewModel.stopMetering()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopMetering()
        }
    }

    val sensorSummary = when {
        !meteringState.sensorAvailable -> "Ambient light sensor not found on this device."
        !meteringState.isMetering -> "Metering is paused."
        meteringState.lux == null -> "Ambient light sensor active. Waiting for the first reading."
        else -> String.format(Locale.getDefault(), "Ambient light: %.0f lux", meteringState.lux)
    }

    val shutterSpeed = viewModel.calculateShutterSpeed(aperture, iso, meteringState.ev)
    val shutterText = formatSuggestedShutter(shutterSpeed, stopMode, body)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Light Meter",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Source: ambient light sensor only. Camera metering is not enabled yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = sensorSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = if (meteringState.sensorAvailable) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Measured EV: ${meteringState.ev?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "--"}",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Suggested Shutter: $shutterText",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Using ${body.label} with ${lens.label} and ${stopMode.label.lowercase(Locale.getDefault())} exposure steps.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        GearSelectionCard(
            body = body,
            lens = lens,
            allowAdaptedLenses = allowAdaptedLenses,
            compatibleLenses = compatibleLenses,
            onBodySelected = { bodyId = it.id },
            onLensSelected = { lensId = it.id },
            onAllowAdaptedLensesChanged = { allowAdaptedLenses = it },
        )

        StopModeSelector(
            selectedMode = stopMode,
            onModeSelected = { stopModeName = it.name }
        )

        ExposureStepControl(
            label = "Aperture",
            value = apertureOptions[apertureIndex].label,
            supportingText = "Available on ${lens.label}. ${stopMode.apertureDescription}",
            previousLabel = "Wider",
            nextLabel = "Narrower",
            canGoPrevious = apertureIndex > 0,
            canGoNext = apertureIndex < apertureOptions.lastIndex,
            onPrevious = { selectedAperture = apertureOptions[apertureIndex - 1].value },
            onNext = { selectedAperture = apertureOptions[apertureIndex + 1].value },
        )

        ExposureStepControl(
            label = "ISO",
            value = isoOptions[isoIndex].label,
            supportingText = "Available on ${body.label}. ${stopMode.isoDescription}",
            previousLabel = "Lower",
            nextLabel = "Higher",
            canGoPrevious = isoIndex > 0,
            canGoNext = isoIndex < isoOptions.lastIndex,
            onPrevious = { selectedIso = isoOptions[isoIndex - 1].value },
            onNext = { selectedIso = isoOptions[isoIndex + 1].value },
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Calibration Offset: ${String.format(Locale.getDefault(), "%+.1f EV", calibrationOffset)}")
            Slider(
                value = calibrationOffset,
                onValueChange = {
                    calibrationOffset = it
                    viewModel.setCalibrationOffset(it)
                },
                valueRange = -3f..3f,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Composable
private fun GearSelectionCard(
    body: CameraBodyProfile,
    lens: LensProfile,
    allowAdaptedLenses: Boolean,
    compatibleLenses: List<LensProfile>,
    onBodySelected: (CameraBodyProfile) -> Unit,
    onLensSelected: (LensProfile) -> Unit,
    onAllowAdaptedLensesChanged: (Boolean) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Gear",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Choose a camera body first. Lens options stay filtered to the mounts that make sense for that body.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (body.adaptedCompatibleMounts.isNotEmpty()) {
                AdapterModeSelector(
                    allowAdaptedLenses = allowAdaptedLenses,
                    adaptedMountSummary = body.adaptedCompatibilitySummary,
                    onAllowAdaptedLensesChanged = onAllowAdaptedLensesChanged,
                )
            }
            SelectionDropdownField(
                label = "Camera body",
                selectedText = body.label,
                supportingText = "${body.description} Native mounts: ${body.nativeCompatibilitySummary}.",
                options = cameraBodyProfiles,
                optionLabel = { profile -> profile.label },
                onSelected = onBodySelected,
            )
            SelectionDropdownField(
                label = "Lens",
                selectedText = lens.label,
                supportingText = buildString {
                    append(lens.description)
                    append(' ')
                    append(lens.mountSummary)
                    append(". ")
                    append(compatibleLenses.size)
                    append(" selectable lens profiles for ")
                    append(body.label)
                    append('.')
                    if (body.adaptedCompatibleMounts.isNotEmpty()) {
                        append(' ')
                        append(
                            if (allowAdaptedLenses) {
                                "Adapted lenses are included."
                            } else {
                                "Showing native lenses only."
                            }
                        )
                    }
                },
                options = compatibleLenses,
                optionLabel = { profile -> profile.label },
                onSelected = onLensSelected,
                emptyResultsText = "No matching lenses for that search.",
            )
        }
    }
}

@Composable
private fun AdapterModeSelector(
    allowAdaptedLenses: Boolean,
    adaptedMountSummary: String,
    onAllowAdaptedLensesChanged: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Lens compatibility",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = if (allowAdaptedLenses) {
                "Including adapted mounts: $adaptedMountSummary."
            } else {
                "Showing native-mount lenses only for a shorter, cleaner list."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilterChip(
                selected = !allowAdaptedLenses,
                onClick = { onAllowAdaptedLensesChanged(false) },
                label = { Text("Native only") },
            )
            FilterChip(
                selected = allowAdaptedLenses,
                onClick = { onAllowAdaptedLensesChanged(true) },
                label = { Text("Allow adapted") },
            )
        }
    }
}

@Composable
private fun StopModeSelector(
    selectedMode: ExposureStopMode,
    onModeSelected: (ExposureStopMode) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Standard Steps",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Choose whether the exposure controls use full-stop or one-third-stop camera values.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExposureStopMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == selectedMode,
                        onClick = { onModeSelected(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SelectionDropdownField(
    label: String,
    selectedText: String,
    supportingText: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    emptyResultsText: String = "No matching results.",
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(label) { mutableStateOf("") }
    val filteredOptions = remember(options, query) {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        if (normalizedQuery.isBlank()) {
            options
        } else {
            options.filter { option ->
                optionLabel(option).lowercase(Locale.getDefault()).contains(normalizedQuery)
            }
        }
    }

    LaunchedEffect(expanded) {
        if (!expanded) {
            query = ""
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            supportingText = { Text(supportingText) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("Search $label") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )

            if (filteredOptions.isEmpty()) {
                Text(
                    text = emptyResultsText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            } else {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExposureStepControl(
    label: String,
    value: String,
    supportingText: String,
    previousLabel: String,
    nextLabel: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = canGoPrevious,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(previousLabel)
                }
                FilledTonalButton(
                    onClick = onNext,
                    enabled = canGoNext,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(nextLabel)
                }
            }
        }
    }
}

private fun formatSuggestedShutter(
    shutterSpeedSeconds: Double?,
    stopMode: ExposureStopMode,
    body: CameraBodyProfile,
): String {
    val seconds = shutterSpeedSeconds ?: return "Awaiting sensor data"
    if (seconds <= 0.0) return "Awaiting sensor data"

    val shutterOptions = body.filterShutters(stopMode.shutterOptions)
    val longestStandard = shutterOptions.maxOf { it.seconds }

    if (seconds > longestStandard) {
        return if (body.supportsBulb) {
            "Bulb ~ ${formatDuration(seconds)}"
        } else {
            "Longer than ${shutterOptions.first().label}"
        }
    }

    val suggestion = shutterOptions.minByOrNull { option ->
        abs(log2(option.seconds / seconds))
    } ?: return "Awaiting sensor data"

    return suggestion.label
}

private fun formatDuration(seconds: Double): String {
    val rounded = seconds.roundToInt()
    val minutes = rounded / 60
    val remainderSeconds = rounded % 60
    return when {
        rounded < 60 -> String.format(Locale.getDefault(), "%d s", rounded)
        remainderSeconds == 0 -> String.format(Locale.getDefault(), "%d min", minutes)
        else -> String.format(Locale.getDefault(), "%d min %d s", minutes, remainderSeconds)
    }
}

private fun <T : Number> nearestExposureIndex(
    options: List<ExposureOption<T>>,
    selectedValue: Double,
): Int {
    return options.indices.minByOrNull { index ->
        abs(log2(options[index].value.toDouble() / selectedValue))
    } ?: 0
}