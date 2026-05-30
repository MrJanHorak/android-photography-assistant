package com.janhorak.shutterdeck.metering.presentation

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
import com.janhorak.shutterdeck.metering.domain.assessShootingAid
import com.janhorak.shutterdeck.metering.domain.buildExposureWorkflowSuggestion
import com.janhorak.shutterdeck.metering.domain.calculateHandheldMinimumShutterSeconds
import com.janhorak.shutterdeck.metering.domain.availableStabilizationModes
import com.janhorak.shutterdeck.metering.domain.formatDecimal
import com.janhorak.shutterdeck.metering.domain.formatCameraExposureSummary
import com.janhorak.shutterdeck.metering.domain.formatExposureTime
import com.janhorak.shutterdeck.metering.domain.formatSuggestedShutter
import com.janhorak.shutterdeck.metering.domain.matchingScenePreset
import com.janhorak.shutterdeck.metering.domain.nearestExposureIndex
import com.janhorak.shutterdeck.metering.domain.ReflectiveMeterReading
import com.janhorak.shutterdeck.metering.domain.ScenePreset
import com.janhorak.shutterdeck.metering.domain.ShootingAidAssessmentLevel
import com.janhorak.shutterdeck.metering.domain.StabilizationMode
import com.janhorak.shutterdeck.metering.domain.SubjectMotionProfile
import com.janhorak.shutterdeck.metering.domain.WorkflowPriority
import java.util.Locale
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
    var gearCatalog by remember(context) { mutableStateOf(GearCatalogLoader.load(context)) }
    var catalogStatusMessage by remember(context) { mutableStateOf<String?>(null) }
    var catalogStatusIsError by remember(context) { mutableStateOf(false) }
    var pendingCatalogImportPreview by remember(context) { mutableStateOf<GearCatalogImportPreview?>(null) }
    val allBodies = gearCatalog.cameraBodyProfiles
    val allLenses = gearCatalog.lensProfiles
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
    val catalogImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            GearCatalogLoader.previewImportFromUri(context, uri)
                .onSuccess { preview ->
                    pendingCatalogImportPreview = preview
                    catalogStatusIsError = false
                    catalogStatusMessage = null
                }
                .onFailure { throwable ->
                    pendingCatalogImportPreview = null
                    catalogStatusIsError = true
                    catalogStatusMessage = throwable.message ?: "Unable to preview the selected JSON catalog."
                }
        },
    )

    var bodyId by rememberSaveable { mutableStateOf(savedGearSelection?.bodyId ?: allBodies.first().id) }
    var lensId by rememberSaveable { mutableStateOf(savedGearSelection?.lensId ?: allLenses.first().id) }
    var selectedFocalLengthMm by rememberSaveable {
        mutableIntStateOf(savedGearSelection?.selectedFocalLengthMm ?: allLenses.first().defaultFocalLengthMm())
    }
    var subjectMotionProfileName by rememberSaveable { mutableStateOf(savedGearSelection?.subjectMotionProfileName ?: SubjectMotionProfile.STILL.name) }
    var stabilizationModeName by rememberSaveable { mutableStateOf(savedGearSelection?.stabilizationModeName ?: StabilizationMode.OFF.name) }
    var workflowPriorityName by rememberSaveable { mutableStateOf(savedGearSelection?.workflowPriorityName ?: WorkflowPriority.ISO_FIRST.name) }
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
    val subjectMotionProfile = remember(subjectMotionProfileName) { SubjectMotionProfile.valueOf(subjectMotionProfileName) }
    val workflowPriority = remember(workflowPriorityName) { WorkflowPriority.valueOf(workflowPriorityName) }
    val availableBodies = remember(bodyCategoryFilter) {
        bodyCategoryFilter.category?.let { category ->
            allBodies.filter { profile -> profile.category == category }
        } ?: allBodies
    }
    val body = remember(bodyId, availableBodies) {
        availableBodies.firstOrNull { it.id == bodyId }
            ?: allBodies.firstOrNull { it.id == bodyId }
            ?: availableBodies.first()
    }
    val stopMode = remember(stopModeName) { ExposureStopMode.valueOf(stopModeName) }
    val compatibleLenses = remember(body, allowAdaptedLenses, allLenses) {
        compatibleLensProfiles(body, allowAdaptedLenses, allLenses)
    }
    val lens = remember(lensId, compatibleLenses) {
        compatibleLenses.firstOrNull { it.id == lensId } ?: compatibleLenses.first()
    }
    val focalLengthMm = remember(lens, selectedFocalLengthMm) { lens.clampFocalLength(selectedFocalLengthMm) }
    val apertureOptions = remember(lens, stopMode, focalLengthMm) { lens.filterApertures(stopMode.apertureOptions, focalLengthMm) }
    val isoOptions = remember(body, stopMode) { body.filterIsos(stopMode.isoOptions) }
    val supportedStabilizationModes = remember(body, lens) {
        availableStabilizationModes(
            hasLensStabilization = lens.hasOpticalStabilization,
            hasBodyStabilization = body.hasInBodyStabilization,
        )
    }
    val stabilizationMode = remember(stabilizationModeName, supportedStabilizationModes) {
        supportedStabilizationModes.firstOrNull { it.name == stabilizationModeName } ?: supportedStabilizationModes.first()
    }
    val scenePreset = remember(subjectMotionProfile, stabilizationMode, supportedStabilizationModes) {
        matchingScenePreset(
            subjectMotionProfile = subjectMotionProfile,
            stabilizationMode = stabilizationMode,
            availableModes = supportedStabilizationModes,
        )
    }

    LaunchedEffect(body.id, compatibleLenses, lensId) {
        if (compatibleLenses.none { it.id == lensId }) {
            lensId = compatibleLenses.first().id
        }
    }

    LaunchedEffect(lens.id, selectedFocalLengthMm) {
        val clampedFocalLength = lens.clampFocalLength(selectedFocalLengthMm)
        if (selectedFocalLengthMm != clampedFocalLength) {
            selectedFocalLengthMm = clampedFocalLength
        }
    }

    LaunchedEffect(bodyCategoryFilter, body.id) {
        if (bodyCategoryFilter.category != null && body.category != bodyCategoryFilter.category) {
            bodyId = availableBodies.first().id
        }
    }

    LaunchedEffect(supportedStabilizationModes, stabilizationMode.name) {
        if (supportedStabilizationModes.none { it.name == stabilizationMode.name }) {
            stabilizationModeName = supportedStabilizationModes.first().name
        }
    }

    LaunchedEffect(
        body.id,
        lens.id,
        focalLengthMm,
        subjectMotionProfile.name,
        stabilizationMode.name,
        workflowPriority.name,
        allowAdaptedLenses,
        meteringSource.name,
        stopMode.name,
        selectedIso,
        selectedAperture,
        calibrationOffset,
    ) {
        gearPreferences.save(
            SavedGearSelection(
                bodyId = body.id,
                lensId = lens.id,
                selectedFocalLengthMm = focalLengthMm,
                subjectMotionProfileName = subjectMotionProfile.name,
                stabilizationModeName = stabilizationMode.name,
                workflowPriorityName = workflowPriority.name,
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
    val shutterText = formatSuggestedShutter(
        shutterSpeedSeconds = shutterSpeed,
        shutterOptions = body.filterShutters(stopMode.shutterOptions),
        supportsBulb = body.supportsBulb,
    )
    val evText = meteringState.ev?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "--"
    val handheldMinimumShutterSeconds = calculateHandheldMinimumShutterSeconds(
        focalLengthMm = focalLengthMm,
        cropFactor = body.cropFactor,
        stabilizationStops = stabilizationMode.stopsBenefit,
    )
    val motionMinimumShutterSeconds = subjectMotionProfile.minimumShutterSeconds
    val recommendedMinimumShutterSeconds = minOf(handheldMinimumShutterSeconds, motionMinimumShutterSeconds)
    val workflowSuggestion = remember(
        workflowPriority,
        shutterSpeed,
        recommendedMinimumShutterSeconds,
        aperture,
        iso,
        apertureOptions,
        isoOptions,
    ) {
        buildExposureWorkflowSuggestion(
            workflowPriority = workflowPriority,
            measuredShutterSeconds = shutterSpeed,
            recommendedMinimumShutterSeconds = recommendedMinimumShutterSeconds,
            currentAperture = aperture,
            currentIso = iso,
            apertureOptions = apertureOptions,
            isoOptions = isoOptions,
        )
    }

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

        CatalogManagementCard(
            gearCatalog = gearCatalog,
            hasImportedCatalog = GearCatalogLoader.hasImportedCatalog(context),
            importPreview = pendingCatalogImportPreview,
            statusMessage = catalogStatusMessage,
            statusIsError = catalogStatusIsError,
            onImportCatalog = {
                catalogImportLauncher.launch(arrayOf("application/json", "text/*"))
            },
            onApplyCatalogImport = {
                pendingCatalogImportPreview?.let { preview ->
                    runCatching { GearCatalogLoader.applyImportPreview(context, preview) }
                        .onSuccess { importedCatalog ->
                            gearCatalog = importedCatalog
                            pendingCatalogImportPreview = null
                            catalogStatusIsError = false
                            catalogStatusMessage = buildCatalogImportSuccessMessage(preview)
                        }
                        .onFailure { throwable ->
                            catalogStatusIsError = true
                            catalogStatusMessage = throwable.message ?: "Unable to apply the selected JSON catalog."
                        }
                }
            },
            onDismissCatalogImportPreview = {
                pendingCatalogImportPreview = null
                catalogStatusIsError = false
                catalogStatusMessage = "Import preview dismissed."
            },
            onResetCatalog = {
                pendingCatalogImportPreview = null
                gearCatalog = GearCatalogLoader.clearImportedCatalog(context)
                catalogStatusIsError = false
                catalogStatusMessage = "Restored the bundled gear catalog."
            },
        )

        LensSetupCard(
            body = body,
            lens = lens,
            focalLengthMm = focalLengthMm,
            onFocalLengthChanged = { selectedFocalLengthMm = it },
        )

        ShootingAidCard(
            scenePreset = scenePreset,
            subjectMotionProfile = subjectMotionProfile,
            stabilizationMode = stabilizationMode,
            workflowPriority = workflowPriority,
            availableStabilizationModes = supportedStabilizationModes,
            handheldMinimumShutterSeconds = handheldMinimumShutterSeconds,
            motionMinimumShutterSeconds = motionMinimumShutterSeconds,
            recommendedMinimumShutterSeconds = recommendedMinimumShutterSeconds,
            measuredShutterSeconds = shutterSpeed,
            workflowSuggestion = workflowSuggestion,
            onScenePresetSelected = { preset ->
                if (preset == ScenePreset.CUSTOM) return@ShootingAidCard
                preset.subjectMotionProfile?.let { profile ->
                    subjectMotionProfileName = profile.name
                }
                preset.resolveStabilizationMode(supportedStabilizationModes)?.let { mode ->
                    stabilizationModeName = mode.name
                }
            },
            onSubjectMotionSelected = { subjectMotionProfileName = it.name },
            onStabilizationModeSelected = { stabilizationModeName = it.name },
            onWorkflowPrioritySelected = { workflowPriorityName = it.name },
        )

        StopModeSelector(
            selectedMode = stopMode,
            onModeSelected = { stopModeName = it.name }
        )

        ExposureStepControl(
            label = "Aperture",
            value = apertureOptions[apertureIndex].label,
            supportingText = "At ${lens.focalLengthLabel(focalLengthMm)}, this lens opens to f/${formatDecimal(lens.effectiveWidestAperture(focalLengthMm).toDouble())}. ${stopMode.apertureDescription}",
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
private fun CatalogManagementCard(
    gearCatalog: GearCatalog,
    hasImportedCatalog: Boolean,
    importPreview: GearCatalogImportPreview?,
    statusMessage: String?,
    statusIsError: Boolean,
    onImportCatalog: () -> Unit,
    onApplyCatalogImport: () -> Unit,
    onDismissCatalogImportPreview: () -> Unit,
    onResetCatalog: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Catalog",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${gearCatalog.source.label}: ${gearCatalog.cameraBodyProfiles.size} bodies and ${gearCatalog.lensProfiles.size} lenses are available right now.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Import a JSON catalog to add or override gear entries by id without editing the bundled asset.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = onImportCatalog,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Import JSON")
                }
                OutlinedButton(
                    onClick = onResetCatalog,
                    enabled = hasImportedCatalog,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reset to bundled")
                }
            }
            importPreview?.let { preview ->
                CatalogImportPreview(
                    preview = preview,
                    onApplyCatalogImport = onApplyCatalogImport,
                    onDismissCatalogImportPreview = onDismissCatalogImportPreview,
                )
            }
            statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CatalogImportPreview(
    preview: GearCatalogImportPreview,
    onApplyCatalogImport: () -> Unit,
    onDismissCatalogImportPreview: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Pending import",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Result: ${preview.mergedCatalog.cameraBodyProfiles.size} bodies and ${preview.mergedCatalog.lensProfiles.size} lenses after merging over the bundled catalog.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatCatalogChangeLine("Bodies", preview.cameraBodyChanges),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatCatalogChangeLine("Lenses", preview.lensChanges),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Applying this replaces any current imported catalog override file.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = onApplyCatalogImport,
                modifier = Modifier.weight(1f),
            ) {
                Text("Apply import")
            }
            OutlinedButton(
                onClick = onDismissCatalogImportPreview,
                modifier = Modifier.weight(1f),
            ) {
                Text("Dismiss")
            }
        }
    }
}

private fun formatCatalogChangeLine(
    collectionLabel: String,
    changes: GearCatalogImportChanges,
): String {
    val pieces = buildList {
        if (changes.addedLabels.isNotEmpty()) {
            add("${changes.addedCount} added (${formatCatalogPreviewNames(changes.addedLabels)})")
        }
        if (changes.overriddenLabels.isNotEmpty()) {
            add("${changes.overriddenCount} overridden (${formatCatalogPreviewNames(changes.overriddenLabels)})")
        }
    }
    return if (pieces.isEmpty()) {
        "$collectionLabel: no entries in this import."
    } else {
        "$collectionLabel: ${pieces.joinToString("; ")}"
    }
}

private fun formatCatalogPreviewNames(labels: List<String>): String {
    val visibleLabels = labels.take(3)
    val remainingCount = labels.size - visibleLabels.size
    return buildString {
        append(visibleLabels.joinToString())
        if (remainingCount > 0) {
            append(", +")
            append(remainingCount)
            append(" more")
        }
    }
}

private fun buildCatalogImportSuccessMessage(preview: GearCatalogImportPreview): String {
    return "Applied custom gear JSON: " +
        "${preview.cameraBodyChanges.addedCount} bodies added, " +
        "${preview.cameraBodyChanges.overriddenCount} bodies overridden; " +
        "${preview.lensChanges.addedCount} lenses added, " +
        "${preview.lensChanges.overriddenCount} lenses overridden."
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
private fun LensSetupCard(
    body: CameraBodyProfile,
    lens: LensProfile,
    focalLengthMm: Int,
    onFocalLengthChanged: (Int) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Lens setup",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (lens.isZoom) {
                    "Set the zoom position for the shot. The widest usable aperture and handheld guidance update with focal length."
                } else {
                    "This lens has a fixed focal length, so the framing and widest aperture stay constant."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric(
                    label = "Focal length",
                    value = lens.focalLengthLabel(focalLengthMm),
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "Widest aperture now",
                    value = "f/${formatDecimal(lens.effectiveWidestAperture(focalLengthMm).toDouble())}",
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = buildString {
                    append("Lens range: ")
                    append(lens.focalLengthRangeLabel)
                    append(" with ")
                    append(lens.widestApertureRangeLabel)
                    append(" maximum aperture behavior.")
                    if (body.cropFactor > 1.05f) {
                        append(' ')
                        append("On this body, ")
                        append(lens.focalLengthLabel(focalLengthMm))
                        append(" frames like about ")
                        append(formatDecimal((focalLengthMm * body.cropFactor).toDouble()))
                        append("mm on full frame.")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (lens.isZoom) {
                Slider(
                    value = focalLengthMm.toFloat(),
                    onValueChange = { onFocalLengthChanged(it.roundToInt()) },
                    valueRange = lens.minFocalLengthMm.toFloat()..lens.maxFocalLengthMm.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ShootingAidCard(
    scenePreset: ScenePreset,
    subjectMotionProfile: SubjectMotionProfile,
    stabilizationMode: StabilizationMode,
    workflowPriority: WorkflowPriority,
    availableStabilizationModes: List<StabilizationMode>,
    handheldMinimumShutterSeconds: Double,
    motionMinimumShutterSeconds: Double,
    recommendedMinimumShutterSeconds: Double,
    measuredShutterSeconds: Double?,
    workflowSuggestion: String,
    onScenePresetSelected: (ScenePreset) -> Unit,
    onSubjectMotionSelected: (SubjectMotionProfile) -> Unit,
    onStabilizationModeSelected: (StabilizationMode) -> Unit,
    onWorkflowPrioritySelected: (WorkflowPriority) -> Unit,
) {
    val assessment = remember(measuredShutterSeconds, recommendedMinimumShutterSeconds, subjectMotionProfile, stabilizationMode) {
        assessShootingAid(
            measuredShutterSeconds = measuredShutterSeconds,
            recommendedMinimumShutterSeconds = recommendedMinimumShutterSeconds,
            subjectMotionProfile = subjectMotionProfile,
            stabilizationMode = stabilizationMode,
        )
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Shooting aid",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Use these quick constraints to judge whether the metered shutter is practical for the way you actually want to shoot.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectionDropdownField(
                label = "Scene preset",
                selectedText = scenePreset.label,
                supportingText = scenePreset.description,
                options = ScenePreset.entries,
                optionLabel = { preset -> preset.label },
                onSelected = onScenePresetSelected,
            )
            SelectionDropdownField(
                label = "Subject motion",
                selectedText = subjectMotionProfile.label,
                supportingText = subjectMotionProfile.description,
                options = SubjectMotionProfile.entries,
                optionLabel = { profile -> profile.label },
                onSelected = onSubjectMotionSelected,
            )
            SelectionDropdownField(
                label = "Stabilization",
                selectedText = stabilizationMode.label,
                supportingText = stabilizationMode.description,
                options = availableStabilizationModes,
                optionLabel = { mode -> mode.label },
                onSelected = onStabilizationModeSelected,
            )
            SelectionDropdownField(
                label = "Workflow priority",
                selectedText = workflowPriority.label,
                supportingText = workflowPriority.description,
                options = WorkflowPriority.entries,
                optionLabel = { priority -> priority.label },
                onSelected = onWorkflowPrioritySelected,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric(
                    label = "Handheld minimum",
                    value = formatExposureTime(handheldMinimumShutterSeconds),
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "Motion minimum",
                    value = formatExposureTime(motionMinimumShutterSeconds),
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "Use at least",
                    value = formatExposureTime(recommendedMinimumShutterSeconds),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = workflowSuggestion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = assessment.message,
                style = MaterialTheme.typography.bodySmall,
                color = when (assessment.level) {
                    ShootingAidAssessmentLevel.AWAITING_READING -> MaterialTheme.colorScheme.onSurfaceVariant
                    ShootingAidAssessmentLevel.SUPPORTED -> MaterialTheme.colorScheme.primary
                    ShootingAidAssessmentLevel.BORDERLINE -> MaterialTheme.colorScheme.tertiary
                    ShootingAidAssessmentLevel.TOO_SLOW -> MaterialTheme.colorScheme.error
                },
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

private fun isCameraPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}