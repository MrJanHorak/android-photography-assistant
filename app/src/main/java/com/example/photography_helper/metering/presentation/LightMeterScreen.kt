package com.example.photography_helper.metering.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

private enum class BodyCategoryFilter(val label: String, val category: CameraBodyCategory?) {
    ALL("All bodies", null),
    DIGITAL("Digital", CameraBodyCategory.DIGITAL),
    MANUAL_FILM("Manual / film", CameraBodyCategory.MANUAL_FILM),
}

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
    var hasCameraPermission by rememberSaveable { mutableStateOf(isCameraPermissionGranted(context)) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            viewModel.setCameraPermissionGranted(granted)
        },
    )

    var bodyId by rememberSaveable { mutableStateOf(savedGearSelection?.bodyId ?: cameraBodyProfiles.first().id) }
    var lensId by rememberSaveable { mutableStateOf(savedGearSelection?.lensId ?: lensProfiles.first().id) }
    var allowAdaptedLenses by rememberSaveable { mutableStateOf(savedGearSelection?.allowAdaptedLenses ?: false) }
    var bodyCategoryFilterName by rememberSaveable { mutableStateOf(BodyCategoryFilter.ALL.name) }
    var meteringSourceName by rememberSaveable {
        mutableStateOf(savedGearSelection?.meteringSourceName ?: MeteringSource.AMBIENT_SENSOR.name)
    }
    var stopModeName by rememberSaveable { mutableStateOf(savedGearSelection?.stopModeName ?: ExposureStopMode.FULL.name) }
    var selectedAperture by rememberSaveable { mutableFloatStateOf(savedGearSelection?.selectedAperture ?: 2.8f) }
    var selectedIso by rememberSaveable { mutableIntStateOf(savedGearSelection?.selectedIso ?: 100) }
    var calibrationOffset by rememberSaveable { mutableFloatStateOf(savedGearSelection?.calibrationOffset ?: 0f) }

    val bodyCategoryFilter = remember(bodyCategoryFilterName) { BodyCategoryFilter.valueOf(bodyCategoryFilterName) }
    val meteringSource = remember(meteringSourceName) { MeteringSource.valueOf(meteringSourceName) }
    val availableBodies = remember(bodyCategoryFilter) {
        bodyCategoryFilter.category?.let { category ->
            cameraBodyProfiles.filter { profile -> profile.category == category }
        } ?: cameraBodyProfiles
    }
    val body = remember(bodyId, availableBodies) {
        availableBodies.firstOrNull { it.id == bodyId }
            ?: cameraBodyProfiles.firstOrNull { it.id == bodyId }
            ?: availableBodies.first()
    }
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

    LaunchedEffect(bodyCategoryFilter, body.id) {
        if (bodyCategoryFilter.category != null && body.category != bodyCategoryFilter.category) {
            bodyId = availableBodies.first().id
        }
    }

    LaunchedEffect(body.id, lens.id, allowAdaptedLenses, stopMode.name, selectedIso, selectedAperture, calibrationOffset) {
        gearPreferences.save(
            SavedGearSelection(
                bodyId = body.id,
                lensId = lens.id,
                allowAdaptedLenses = allowAdaptedLenses,
                meteringSourceName = meteringSource.name,
                stopModeName = stopMode.name,
                selectedIso = selectedIso,
                selectedAperture = selectedAperture,
                calibrationOffset = calibrationOffset,
            )
        )
    }

    LaunchedEffect(meteringSource) {
        viewModel.setMeteringSource(meteringSource)
    }

    LaunchedEffect(hasCameraPermission) {
        viewModel.setCameraPermissionGranted(hasCameraPermission)
    }

    LaunchedEffect(calibrationOffset) {
        viewModel.setCalibrationOffset(calibrationOffset)
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
                Lifecycle.Event.ON_RESUME -> {
                    hasCameraPermission = isCameraPermissionGranted(context)
                    viewModel.startMetering()
                }
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

    val readingDetailLabel = when (meteringState.selectedSource) {
        MeteringSource.AMBIENT_SENSOR -> "Ambient light"
        MeteringSource.CAMERA_REFLECTIVE -> "Phone auto exposure"
    }
    val readingDetailText = when (meteringState.selectedSource) {
        MeteringSource.AMBIENT_SENSOR -> {
            meteringState.lux?.let { String.format(Locale.getDefault(), "%.0f lux", it) } ?: "--"
        }
        MeteringSource.CAMERA_REFLECTIVE -> {
            meteringState.cameraReading?.let(::formatCameraExposureSummary) ?: "--"
        }
    }
    val sourceSummary = when (meteringState.selectedSource) {
        MeteringSource.AMBIENT_SENSOR -> when {
            !meteringState.sensorAvailable -> "Ambient light sensor not found on this device."
            !meteringState.isMetering -> "Ambient light sensor paused."
            meteringState.lux == null -> "Ambient light sensor active. Waiting for the first reading."
            else -> "Incident-style reading from the ambient light sensor."
        }
        MeteringSource.CAMERA_REFLECTIVE -> when {
            !meteringState.cameraAvailable -> "Back camera not available on this device."
            !meteringState.cameraPermissionGranted -> "Camera permission is required for reflective metering."
            meteringState.cameraReading == null -> "Back camera active. Waiting for auto exposure data."
            else -> "Reflective reading from the back camera auto exposure."
        }
    }

    val shutterSpeed = viewModel.calculateShutterSpeed(aperture, iso, meteringState.ev)
    val shutterText = formatSuggestedShutter(shutterSpeed, stopMode, body)
    val evText = meteringState.ev?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "--"

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

        MeteringSourceCard(
            selectedSource = meteringSource,
            cameraAvailable = meteringState.cameraAvailable,
            onSourceSelected = { meteringSourceName = it.name },
        )

        MeterSummaryCard(
            meteringSource = meteringState.selectedSource,
            evText = evText,
            readingDetailLabel = readingDetailLabel,
            readingDetailText = readingDetailText,
            shutterText = shutterText,
            sourceSummary = sourceSummary,
            body = body,
            lens = lens,
            stopMode = stopMode,
            statusIsError = when (meteringState.selectedSource) {
                MeteringSource.AMBIENT_SENSOR -> !meteringState.sensorAvailable
                MeteringSource.CAMERA_REFLECTIVE -> !meteringState.cameraAvailable || !meteringState.cameraPermissionGranted
            },
        )

        if (meteringSource == MeteringSource.CAMERA_REFLECTIVE) {
            CameraMeterCard(
                cameraAvailable = meteringState.cameraAvailable,
                cameraPermissionGranted = hasCameraPermission,
                cameraReading = meteringState.cameraReading,
                onRequestPermission = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onExposureSample = viewModel::updateCameraReading,
                onCameraMeteringStopped = viewModel::onCameraMeteringStopped,
            )
        }

        GearSelectionCard(
            bodyCategoryFilter = bodyCategoryFilter,
            body = body,
            lens = lens,
            availableBodies = availableBodies,
            allowAdaptedLenses = allowAdaptedLenses,
            compatibleLenses = compatibleLenses,
            onBodyCategoryFilterSelected = { bodyCategoryFilterName = it.name },
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
            supportingText = "Available on ${body.label}. ${body.meteringWorkflowNote} ${stopMode.isoDescription}",
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
                onValueChange = { calibrationOffset = it },
                valueRange = -3f..3f,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Composable
private fun MeteringSourceCard(
    selectedSource: MeteringSource,
    cameraAvailable: Boolean,
    onSourceSelected: (MeteringSource) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Metering source",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Use the ambient sensor for incident-style readings or switch to reflective metering through the phone camera.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MeteringSource.entries.forEach { source ->
                    FilterChip(
                        selected = source == selectedSource,
                        onClick = { onSourceSelected(source) },
                        enabled = source != MeteringSource.CAMERA_REFLECTIVE || cameraAvailable,
                        label = { Text(source.label) },
                    )
                }
            }
            if (!cameraAvailable) {
                Text(
                    text = "Reflective camera metering is disabled because this device does not report a usable camera.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MeterSummaryCard(
    meteringSource: MeteringSource,
    evText: String,
    readingDetailLabel: String,
    readingDetailText: String,
    shutterText: String,
    sourceSummary: String,
    body: CameraBodyProfile,
    lens: LensProfile,
    stopMode: ExposureStopMode,
    statusIsError: Boolean,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Meter Summary",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = meteringSource.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = evText,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Measured EV",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric(
                    label = "Suggested shutter",
                    value = shutterText,
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = readingDetailLabel,
                    value = readingDetailText,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = sourceSummary,
                style = MaterialTheme.typography.bodySmall,
                color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${body.category.label} setup: ${body.label} with ${lens.label} using ${stopMode.label.lowercase(Locale.getDefault())} steps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CameraMeterCard(
    cameraAvailable: Boolean,
    cameraPermissionGranted: Boolean,
    cameraReading: ReflectiveMeterReading?,
    onRequestPermission: () -> Unit,
    onExposureSample: (aperture: Float, shutterSeconds: Double, iso: Int) -> Unit,
    onCameraMeteringStopped: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Reflective meter",
                style = MaterialTheme.typography.titleMedium,
            )
            when {
                !cameraAvailable -> {
                    Text(
                        text = "This device does not expose a usable back camera for reflective metering.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                !cameraPermissionGranted -> {
                    Text(
                        text = "Point the phone at the subject and let the app read the back camera's live auto exposure. Camera access is required first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(onClick = onRequestPermission) {
                        Text("Allow camera access")
                    }
                }

                else -> {
                    Text(
                        text = cameraReading?.let {
                            "Phone camera AE: ${formatCameraExposureSummary(it)}"
                        } ?: "Point the phone at the subject. The meter is waiting for the first auto exposure sample.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CameraMeterPreview(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        onExposureSample = onExposureSample,
                        onCameraMeteringStopped = onCameraMeteringStopped,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun GearSelectionCard(
    bodyCategoryFilter: BodyCategoryFilter,
    body: CameraBodyProfile,
    lens: LensProfile,
    availableBodies: List<CameraBodyProfile>,
    allowAdaptedLenses: Boolean,
    compatibleLenses: List<LensProfile>,
    onBodyCategoryFilterSelected: (BodyCategoryFilter) -> Unit,
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
            BodyCategoryFilterSelector(
                selectedFilter = bodyCategoryFilter,
                onFilterSelected = onBodyCategoryFilterSelected,
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
                supportingText = "${body.description} ${body.category.label}. Native mounts: ${body.nativeCompatibilitySummary}. ${body.meteringWorkflowNote}",
                options = availableBodies,
                optionLabel = { profile -> profile.label },
                onSelected = onBodySelected,
                emptyResultsText = "No camera bodies match that search.",
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
private fun BodyCategoryFilterSelector(
    selectedFilter: BodyCategoryFilter,
    onFilterSelected: (BodyCategoryFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Body type",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Digital bodies suit modern camera workflows. Manual and film bodies keep the same meter but frame ISO as film speed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BodyCategoryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) },
                )
            }
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

private fun formatCameraExposureSummary(reading: ReflectiveMeterReading): String {
    return "f/${formatDecimal(reading.aperture.toDouble())}  ${formatExposureTime(reading.shutterSeconds)}  ISO ${reading.iso}"
}

private fun formatExposureTime(seconds: Double): String {
    if (seconds <= 0.0) return "--"
    if (seconds >= 1.0) {
        return if (seconds >= 10.0 || abs(seconds - seconds.roundToInt()) < 0.05) {
            String.format(Locale.getDefault(), "%d s", seconds.roundToInt())
        } else {
            String.format(Locale.getDefault(), "%.1f s", seconds)
        }
    }

    val denominator = (1.0 / seconds).roundToInt().coerceAtLeast(1)
    return "1/$denominator s"
}

private fun formatDecimal(value: Double): String {
    return if (abs(value - value.roundToInt()) < 0.05) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

private fun isCameraPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}

private fun <T : Number> nearestExposureIndex(
    options: List<ExposureOption<T>>,
    selectedValue: Double,
): Int {
    return options.indices.minByOrNull { index ->
        abs(log2(options[index].value.toDouble() / selectedValue))
    } ?: 0
}