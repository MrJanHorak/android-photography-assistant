package com.janhorak.shutterdeck.film.presentation

import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.FilmFrameEntity
import com.janhorak.shutterdeck.core.data.db.FilmRollEntity
import com.janhorak.shutterdeck.film.domain.FILM_ROLL_STATUS_ACTIVE
import com.janhorak.shutterdeck.film.domain.defaultFilmFrameCapturedAtText
import com.janhorak.shutterdeck.film.domain.defaultFilmRollCsvFileName
import com.janhorak.shutterdeck.metering.domain.formatExposureTime
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.SectionHeader
import java.util.Locale

@Composable
fun FilmRollDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: FilmRollDetailViewModel = hiltViewModel(),
) {
    val roll by viewModel.roll.collectAsStateWithLifecycle()
    val frames by viewModel.frames.collectAsStateWithLifecycle()
    val frameStatus by viewModel.frameStatus.collectAsStateWithLifecycle()
    val exportStatus by viewModel.exportStatus.collectAsStateWithLifecycle()

    var showingNewEditor by remember { mutableStateOf(false) }
    var editingFrame by remember { mutableStateOf<FilmFrameEntity?>(null) }
    var frameEditorSession by remember { mutableStateOf(0) }

    val nextFrameNumber = remember(frames) { (frames.maxOfOrNull { frame -> frame.frameNumber } ?: 0) + 1 }
    val uniqueFrameCount = remember(frames) { frames.map { frame -> frame.frameNumber }.distinct().size }
    val exportFileName = remember(roll?.id, roll?.title, roll?.stockDisplayName) {
        defaultFilmRollCsvFileName(
            title = roll?.title.orEmpty(),
            stockDisplayName = roll?.stockDisplayName.orEmpty(),
        )
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { destination ->
        if (destination != null) {
            viewModel.exportRollCsv(destination)
        }
    }
    val showFrameEditor = showingNewEditor || editingFrame != null
    val frameEditorKey = remember(showingNewEditor, frameEditorSession, editingFrame?.id, nextFrameNumber) {
        editingFrame?.id?.let { id -> "edit_$id" } ?: "new_${frameEditorSession}_$nextFrameNumber"
    }

    val currentRoll = roll

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (currentRoll == null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "This roll is no longer available.",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Go back to the roll list to start another roll or open a different log.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            item {
                SectionHeader(
                    title = currentRoll.displayTitle,
                    subtitle = "Review the roll metadata, log each frame, and export the finished record when you're done.",
                )
            }
            item {
                FilmRollDetailSummaryCard(
                    roll = currentRoll,
                    uniqueFrameCount = uniqueFrameCount,
                    exposureCount = frames.size,
                    frameStatus = frameStatus,
                    exportStatus = exportStatus,
                    onFinishOrReopen = {
                        if (currentRoll.status == FILM_ROLL_STATUS_ACTIVE) {
                            viewModel.finishRoll()
                        } else {
                            viewModel.reopenRoll()
                        }
                    },
                    onExportCsv = {
                        viewModel.clearExportStatus()
                        exportLauncher.launch(exportFileName)
                    },
                    onAddFrame = {
                        viewModel.clearFrameStatus()
                        if (showFrameEditor && editingFrame == null) {
                            showingNewEditor = false
                        } else {
                            editingFrame = null
                            frameEditorSession += 1
                            showingNewEditor = true
                        }
                    },
                )
            }
            if (showFrameEditor) {
                item {
                    FilmFrameEditorCard(
                        editorKey = frameEditorKey,
                        initial = editingFrame,
                        defaultFrameNumber = nextFrameNumber,
                        onCancel = {
                            showingNewEditor = false
                            editingFrame = null
                        },
                        onSave = { initial, frameNumber, exposureSequence, apertureText, shutterSpeedText, focalLengthText, capturedAtText, latitude, longitude, notes ->
                            val saveAccepted = viewModel.saveFrame(
                                existing = initial,
                                frameNumber = frameNumber,
                                exposureSequence = exposureSequence,
                                apertureText = apertureText,
                                shutterSpeedText = shutterSpeedText,
                                focalLengthText = focalLengthText,
                                capturedAtText = capturedAtText,
                                latitude = latitude,
                                longitude = longitude,
                                notes = notes,
                            )
                            if (saveAccepted) {
                                showingNewEditor = false
                                editingFrame = null
                            }
                        },
                    )
                }
            }
            if (frames.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "No frames logged yet.",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = if (currentRoll.status == FILM_ROLL_STATUS_ACTIVE) {
                                    "Use the button above to add the first frame."
                                } else {
                                    "Reopen the roll if you want to add the first frame now."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            items(frames, key = { frame -> frame.id }) { frame ->
                FilmFrameCard(
                    frame = frame,
                    onEdit = {
                        viewModel.clearFrameStatus()
                        showingNewEditor = false
                        editingFrame = frame
                    },
                    onDelete = { viewModel.deleteFrame(frame) },
                )
            }
        }
    }
}

@Composable
private fun FilmRollDetailSummaryCard(
    roll: FilmRollEntity,
    uniqueFrameCount: Int,
    exposureCount: Int,
    frameStatus: String?,
    exportStatus: String?,
    onFinishOrReopen: () -> Unit,
    onExportCsv: () -> Unit,
    onAddFrame: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = roll.status,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            FilmRollDetailLine("Stock", "${roll.stockDisplayName} · EI ${roll.exposureIndex}")
            FilmRollDetailLine("Camera", roll.cameraLabel)
            FilmRollDetailLine("Lens", roll.lensLabel)
            FilmRollDetailLine("Started on", roll.startedOnText)
            if (roll.finishedOnText.isNotBlank()) {
                FilmRollDetailLine("Finished on", roll.finishedOnText)
            }
            FilmRollDetailLine("Progress", buildFilmRollDetailProgressText(roll, uniqueFrameCount, exposureCount))
            FilmRollDetailLine("Processing", roll.stockProcessingType)
            buildReciprocitySnapshot(roll)?.let { reciprocity ->
                FilmRollDetailLine("Reciprocity", reciprocity)
            }
            if (roll.notes.isNotBlank()) {
                Text(
                    text = roll.notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddFrame,
                    enabled = roll.status == FILM_ROLL_STATUS_ACTIVE,
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Text(
                        text = if (roll.status == FILM_ROLL_STATUS_ACTIVE) "Add frame" else "Reopen to add frames",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onFinishOrReopen) {
                        Text(if (roll.status == FILM_ROLL_STATUS_ACTIVE) "Finish roll" else "Reopen roll")
                    }
                    OutlinedButton(onClick = onExportCsv) {
                        Text("Export CSV")
                    }
                }
            }
            frameStatus?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            exportStatus?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun FilmRollDetailLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FilmFrameEditorCard(
    editorKey: String,
    initial: FilmFrameEntity?,
    defaultFrameNumber: Int,
    onSave: (
        FilmFrameEntity?,
        Int?,
        Int?,
        String,
        String,
        String,
        String,
        Double?,
        Double?,
        String,
    ) -> Unit,
    onCancel: () -> Unit,
) {
    var frameNumberText by remember(editorKey) {
        mutableStateOf((initial?.frameNumber ?: defaultFrameNumber).toString())
    }
    var exposureSequenceText by remember(editorKey) {
        mutableStateOf((initial?.exposureSequence ?: 1).toString())
    }
    var apertureText by remember(editorKey) { mutableStateOf(initial?.apertureText.orEmpty()) }
    var shutterSpeedText by remember(editorKey) { mutableStateOf(initial?.shutterSpeedText.orEmpty()) }
    var focalLengthText by remember(editorKey) { mutableStateOf(initial?.focalLengthText.orEmpty()) }
    var capturedAtText by remember(editorKey) {
        mutableStateOf(initial?.capturedAtText ?: defaultFilmFrameCapturedAtText())
    }
    var latitudeText by remember(editorKey) { mutableStateOf(initial?.latitude?.toString().orEmpty()) }
    var longitudeText by remember(editorKey) { mutableStateOf(initial?.longitude?.toString().orEmpty()) }
    var notes by remember(editorKey) { mutableStateOf(initial?.notes.orEmpty()) }

    val parsedFrameNumber = frameNumberText.toIntOrNull()
    val parsedExposureSequence = exposureSequenceText.toIntOrNull()
    val parsedLatitude = latitudeText.takeIf { text -> text.isNotBlank() }?.toDoubleOrNull()
    val parsedLongitude = longitudeText.takeIf { text -> text.isNotBlank() }?.toDoubleOrNull()
    val saveEnabled = parsedFrameNumber != null &&
        parsedFrameNumber > 0 &&
        parsedExposureSequence != null &&
        parsedExposureSequence > 0 &&
        apertureText.isNotBlank() &&
        shutterSpeedText.isNotBlank() &&
        focalLengthText.isNotBlank() &&
        capturedAtText.isNotBlank() &&
        (latitudeText.isBlank() || parsedLatitude != null) &&
        (longitudeText.isBlank() || parsedLongitude != null)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = if (initial == null) "Log frame" else "Edit frame",
                subtitle = "Use exposure sequence 2, 3, and so on when you make multiple exposures on the same frame number.",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Frame number",
                    value = frameNumberText,
                    onValueChange = { frameNumberText = it },
                    modifier = Modifier.weight(1f),
                )
                LabeledField(
                    label = "Exposure",
                    value = exposureSequenceText,
                    onValueChange = { exposureSequenceText = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Aperture",
                    value = apertureText,
                    onValueChange = { apertureText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
                LabeledField(
                    label = "Shutter",
                    value = shutterSpeedText,
                    onValueChange = { shutterSpeedText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Focal length",
                    value = focalLengthText,
                    onValueChange = { focalLengthText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
                LabeledField(
                    label = "Captured at",
                    value = capturedAtText,
                    onValueChange = { capturedAtText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Latitude",
                    value = latitudeText,
                    onValueChange = { latitudeText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal,
                )
                LabeledField(
                    label = "Longitude",
                    value = longitudeText,
                    onValueChange = { longitudeText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal,
                )
            }
            LabeledField(
                label = "Frame notes",
                value = notes,
                onValueChange = { notes = it },
                singleLine = false,
                keyboardType = KeyboardType.Text,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            initial,
                            parsedFrameNumber,
                            parsedExposureSequence,
                            apertureText,
                            shutterSpeedText,
                            focalLengthText,
                            capturedAtText,
                            parsedLatitude,
                            parsedLongitude,
                            notes,
                        )
                    },
                    enabled = saveEnabled,
                ) {
                    Text(if (initial == null) "Save frame" else "Update frame")
                }
            }
        }
    }
}

@Composable
private fun FilmFrameCard(
    frame: FilmFrameEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = frame.displayLabel,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${frame.apertureText} · ${frame.shutterSpeedText} · ${frame.focalLengthText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = frame.capturedAtText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit frame")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete frame")
                    }
                }
            }
            if (frame.latitude != null || frame.longitude != null) {
                Text(
                    text = "GPS: ${frame.latitude?.toString().orEmpty()} ${frame.longitude?.toString().orEmpty()}".trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (frame.notes.isNotBlank()) {
                Text(
                    text = frame.notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun buildFilmRollDetailProgressText(
    roll: FilmRollEntity,
    uniqueFrameCount: Int,
    exposureCount: Int,
): String {
    val frameText = roll.totalFrames?.let { capacity ->
        "$uniqueFrameCount / $capacity frames"
    } ?: "$uniqueFrameCount frames"
    return if (exposureCount > uniqueFrameCount) {
        "$frameText · $exposureCount exposures"
    } else {
        frameText
    }
}

private fun buildReciprocitySnapshot(roll: FilmRollEntity): String? {
    val exponent = roll.stockReciprocityExponent
    val startsAt = roll.stockReciprocityStartsAtSeconds
    return if (exponent != null && startsAt != null) {
        "Approx. t^${formatCompactExponent(exponent)} after ${formatExposureTime(startsAt)}"
    } else {
        null
    }
}

private fun formatCompactExponent(value: Double): String {
    val formatted = String.format(Locale.ROOT, "%.2f", value)
    return formatted.trimEnd('0').trimEnd('.')
}
