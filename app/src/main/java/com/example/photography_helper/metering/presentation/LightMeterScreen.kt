package com.example.photography_helper.metering.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

    var stopModeName by rememberSaveable { mutableStateOf(ExposureStopMode.FULL.name) }
    var selectedAperture by rememberSaveable { mutableFloatStateOf(2.8f) }
    var selectedIso by rememberSaveable { mutableIntStateOf(100) }
    var calibrationOffset by rememberSaveable { mutableFloatStateOf(0f) }

    val stopMode = remember(stopModeName) { ExposureStopMode.valueOf(stopModeName) }
    val apertureOptions = remember(stopMode) { stopMode.apertureOptions }
    val isoOptions = remember(stopMode) { stopMode.isoOptions }

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
    val shutterText = formatSuggestedShutter(shutterSpeed, stopMode)

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
            text = "Source: ambient light sensor. This screen does not use the camera.",
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
            text = "Suggestions are snapped to ${stopMode.label.lowercase(Locale.getDefault())} exposure steps. Camera-specific limits can still vary.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        StopModeSelector(
            selectedMode = stopMode,
            onModeSelected = { stopModeName = it.name }
        )

        ExposureStepControl(
            label = "Aperture",
            value = apertureOptions[apertureIndex].label,
            supportingText = stopMode.apertureDescription,
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
            supportingText = stopMode.isoDescription,
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
): String {
    val seconds = shutterSpeedSeconds ?: return "Awaiting sensor data"
    if (seconds <= 0.0) return "Awaiting sensor data"

    val shutterOptions = stopMode.shutterOptions
    val longestStandard = shutterOptions.maxOf { it.seconds }

    if (seconds > longestStandard) {
        return "Bulb ~ ${formatDuration(seconds)}"
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

private enum class ExposureStopMode(
    val label: String,
    val apertureDescription: String,
    val isoDescription: String,
    val apertureOptions: List<ExposureOption<Float>>,
    val isoOptions: List<ExposureOption<Int>>,
    val shutterOptions: List<ShutterOption>,
) {
    FULL(
        label = "Full stop",
        apertureDescription = "Common full-stop lens values from f/1.4 to f/22.",
        isoDescription = "Common full-stop ISO values from 50 to 6400.",
        apertureOptions = FULL_STOP_APERTURES,
        isoOptions = FULL_STOP_ISOS,
        shutterOptions = FULL_STOP_SHUTTERS,
    ),
    THIRD(
        label = "1/3 stop",
        apertureDescription = "One-third-stop lens values that better match many digital cameras.",
        isoDescription = "One-third-stop ISO values commonly offered on modern cameras.",
        apertureOptions = THIRD_STOP_APERTURES,
        isoOptions = THIRD_STOP_ISOS,
        shutterOptions = THIRD_STOP_SHUTTERS,
    )
}

private data class ExposureOption<T : Number>(
    val value: T,
    val label: String,
)

private data class ShutterOption(
    val seconds: Double,
    val label: String,
)

private val FULL_STOP_APERTURES = listOf(
    ExposureOption(1.4f, "f/1.4"),
    ExposureOption(2.0f, "f/2"),
    ExposureOption(2.8f, "f/2.8"),
    ExposureOption(4.0f, "f/4"),
    ExposureOption(5.6f, "f/5.6"),
    ExposureOption(8.0f, "f/8"),
    ExposureOption(11.0f, "f/11"),
    ExposureOption(16.0f, "f/16"),
    ExposureOption(22.0f, "f/22"),
)

private val THIRD_STOP_APERTURES = listOf(
    ExposureOption(1.4f, "f/1.4"),
    ExposureOption(1.6f, "f/1.6"),
    ExposureOption(1.8f, "f/1.8"),
    ExposureOption(2.0f, "f/2"),
    ExposureOption(2.2f, "f/2.2"),
    ExposureOption(2.5f, "f/2.5"),
    ExposureOption(2.8f, "f/2.8"),
    ExposureOption(3.2f, "f/3.2"),
    ExposureOption(3.5f, "f/3.5"),
    ExposureOption(4.0f, "f/4"),
    ExposureOption(4.5f, "f/4.5"),
    ExposureOption(5.0f, "f/5"),
    ExposureOption(5.6f, "f/5.6"),
    ExposureOption(6.3f, "f/6.3"),
    ExposureOption(7.1f, "f/7.1"),
    ExposureOption(8.0f, "f/8"),
    ExposureOption(9.0f, "f/9"),
    ExposureOption(10.0f, "f/10"),
    ExposureOption(11.0f, "f/11"),
    ExposureOption(13.0f, "f/13"),
    ExposureOption(14.0f, "f/14"),
    ExposureOption(16.0f, "f/16"),
    ExposureOption(18.0f, "f/18"),
    ExposureOption(20.0f, "f/20"),
    ExposureOption(22.0f, "f/22"),
)

private val FULL_STOP_ISOS = listOf(
    ExposureOption(50, "ISO 50"),
    ExposureOption(100, "ISO 100"),
    ExposureOption(200, "ISO 200"),
    ExposureOption(400, "ISO 400"),
    ExposureOption(800, "ISO 800"),
    ExposureOption(1600, "ISO 1600"),
    ExposureOption(3200, "ISO 3200"),
    ExposureOption(6400, "ISO 6400"),
)

private val THIRD_STOP_ISOS = listOf(
    ExposureOption(50, "ISO 50"),
    ExposureOption(64, "ISO 64"),
    ExposureOption(80, "ISO 80"),
    ExposureOption(100, "ISO 100"),
    ExposureOption(125, "ISO 125"),
    ExposureOption(160, "ISO 160"),
    ExposureOption(200, "ISO 200"),
    ExposureOption(250, "ISO 250"),
    ExposureOption(320, "ISO 320"),
    ExposureOption(400, "ISO 400"),
    ExposureOption(500, "ISO 500"),
    ExposureOption(640, "ISO 640"),
    ExposureOption(800, "ISO 800"),
    ExposureOption(1000, "ISO 1000"),
    ExposureOption(1250, "ISO 1250"),
    ExposureOption(1600, "ISO 1600"),
    ExposureOption(2000, "ISO 2000"),
    ExposureOption(2500, "ISO 2500"),
    ExposureOption(3200, "ISO 3200"),
    ExposureOption(4000, "ISO 4000"),
    ExposureOption(5000, "ISO 5000"),
    ExposureOption(6400, "ISO 6400"),
)

private val FULL_STOP_SHUTTERS = listOf(
    ShutterOption(30.0, "30 s"),
    ShutterOption(15.0, "15 s"),
    ShutterOption(8.0, "8 s"),
    ShutterOption(4.0, "4 s"),
    ShutterOption(2.0, "2 s"),
    ShutterOption(1.0, "1 s"),
    ShutterOption(0.5, "1/2 s"),
    ShutterOption(0.25, "1/4 s"),
    ShutterOption(0.125, "1/8 s"),
    ShutterOption(1.0 / 15.0, "1/15 s"),
    ShutterOption(1.0 / 30.0, "1/30 s"),
    ShutterOption(1.0 / 60.0, "1/60 s"),
    ShutterOption(1.0 / 125.0, "1/125 s"),
    ShutterOption(1.0 / 250.0, "1/250 s"),
    ShutterOption(1.0 / 500.0, "1/500 s"),
    ShutterOption(1.0 / 1000.0, "1/1000 s"),
    ShutterOption(1.0 / 2000.0, "1/2000 s"),
    ShutterOption(1.0 / 4000.0, "1/4000 s"),
    ShutterOption(1.0 / 8000.0, "1/8000 s"),
)

private val THIRD_STOP_SHUTTERS = listOf(
    ShutterOption(30.0, "30 s"),
    ShutterOption(25.0, "25 s"),
    ShutterOption(20.0, "20 s"),
    ShutterOption(15.0, "15 s"),
    ShutterOption(13.0, "13 s"),
    ShutterOption(10.0, "10 s"),
    ShutterOption(8.0, "8 s"),
    ShutterOption(6.0, "6 s"),
    ShutterOption(5.0, "5 s"),
    ShutterOption(4.0, "4 s"),
    ShutterOption(3.2, "3.2 s"),
    ShutterOption(2.5, "2.5 s"),
    ShutterOption(2.0, "2 s"),
    ShutterOption(1.6, "1.6 s"),
    ShutterOption(1.3, "1.3 s"),
    ShutterOption(1.0, "1 s"),
    ShutterOption(0.8, "0.8 s"),
    ShutterOption(0.6, "0.6 s"),
    ShutterOption(0.5, "1/2 s"),
    ShutterOption(0.4, "0.4 s"),
    ShutterOption(1.0 / 3.0, "1/3 s"),
    ShutterOption(0.25, "1/4 s"),
    ShutterOption(0.2, "1/5 s"),
    ShutterOption(1.0 / 6.0, "1/6 s"),
    ShutterOption(0.125, "1/8 s"),
    ShutterOption(0.1, "1/10 s"),
    ShutterOption(1.0 / 13.0, "1/13 s"),
    ShutterOption(1.0 / 15.0, "1/15 s"),
    ShutterOption(0.05, "1/20 s"),
    ShutterOption(0.04, "1/25 s"),
    ShutterOption(1.0 / 30.0, "1/30 s"),
    ShutterOption(0.025, "1/40 s"),
    ShutterOption(0.02, "1/50 s"),
    ShutterOption(1.0 / 60.0, "1/60 s"),
    ShutterOption(1.0 / 80.0, "1/80 s"),
    ShutterOption(0.01, "1/100 s"),
    ShutterOption(1.0 / 125.0, "1/125 s"),
    ShutterOption(1.0 / 160.0, "1/160 s"),
    ShutterOption(0.005, "1/200 s"),
    ShutterOption(1.0 / 250.0, "1/250 s"),
    ShutterOption(1.0 / 320.0, "1/320 s"),
    ShutterOption(0.0025, "1/400 s"),
    ShutterOption(1.0 / 500.0, "1/500 s"),
    ShutterOption(1.0 / 640.0, "1/640 s"),
    ShutterOption(0.00125, "1/800 s"),
    ShutterOption(1.0 / 1000.0, "1/1000 s"),
    ShutterOption(1.0 / 1250.0, "1/1250 s"),
    ShutterOption(1.0 / 1600.0, "1/1600 s"),
    ShutterOption(1.0 / 2000.0, "1/2000 s"),
    ShutterOption(1.0 / 2500.0, "1/2500 s"),
    ShutterOption(1.0 / 3200.0, "1/3200 s"),
    ShutterOption(1.0 / 4000.0, "1/4000 s"),
    ShutterOption(1.0 / 5000.0, "1/5000 s"),
    ShutterOption(1.0 / 6400.0, "1/6400 s"),
    ShutterOption(1.0 / 8000.0, "1/8000 s"),
)