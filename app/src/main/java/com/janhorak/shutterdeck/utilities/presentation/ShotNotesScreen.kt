package com.janhorak.shutterdeck.utilities.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.ShotNoteEntity
import com.janhorak.shutterdeck.core.location.CurrentLocationLookupResult
import com.janhorak.shutterdeck.core.location.DeviceLocationProvider
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.SectionHeader
import com.janhorak.shutterdeck.ui.location.CurrentLocationAction
import com.janhorak.shutterdeck.ui.location.CurrentLocationRequestState
import com.janhorak.shutterdeck.ui.location.formatCoordinateInput
import com.janhorak.shutterdeck.ui.location.rememberCurrentLocationRequestState
import com.janhorak.shutterdeck.utilities.domain.appendShotNoteTranscript
import com.janhorak.shutterdeck.utilities.domain.formatShotNoteTimestamp

private data class ShotNoteEditorState(
    val shotLabel: String = "",
    val noteText: String = "",
    val latitude: String = "",
    val longitude: String = "",
)

@Composable
fun ShotNotesScreen(
    modifier: Modifier = Modifier,
    viewModel: ShotNotesViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val locationProvider = remember(context.applicationContext) {
        DeviceLocationProvider(context.applicationContext)
    }

    var shotLabelDraft by rememberSaveable { mutableStateOf("") }
    var noteDraft by rememberSaveable { mutableStateOf("") }
    var latitudeDraft by rememberSaveable { mutableStateOf("") }
    var longitudeDraft by rememberSaveable { mutableStateOf("") }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingNoteId by rememberSaveable { mutableStateOf(0L) }
    var editShotLabelDraft by rememberSaveable { mutableStateOf("") }
    var editNoteDraft by rememberSaveable { mutableStateOf("") }
    var editLatitudeDraft by rememberSaveable { mutableStateOf("") }
    var editLongitudeDraft by rememberSaveable { mutableStateOf("") }
    var attemptedInitialLocation by rememberSaveable { mutableStateOf(false) }

    val editingNote = notes.firstOrNull { note -> note.id == editingNoteId }
    val draftHasLocation = latitudeDraft.isNotBlank() && longitudeDraft.isNotBlank()
    val editHasLocation = editLatitudeDraft.isNotBlank() && editLongitudeDraft.isNotBlank()
    val speechRecognitionAvailable = remember(context) {
        buildDictationIntent().resolveActivity(context.packageManager) != null
    }
    val dictationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val transcript = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull { spokenText -> spokenText.isNotBlank() }
            ?.trim()

        when {
            result.resultCode == Activity.RESULT_OK && transcript != null -> {
                noteDraft = appendShotNoteTranscript(noteDraft, transcript)
                viewModel.clearStatus()
            }

            result.resultCode == Activity.RESULT_OK -> {
                viewModel.setStatus("No dictation result returned. If you're offline, type the note instead.")
            }

            else -> {
                viewModel.setStatus("Dictation cancelled or not recognised — type the note instead.")
            }
        }
    }

    val draftLocationState = rememberCurrentLocationRequestState { coordinates ->
        latitudeDraft = formatCoordinateInput(coordinates.latitude)
        longitudeDraft = formatCoordinateInput(coordinates.longitude)
        viewModel.clearStatus()
    }
    val editLocationState = rememberCurrentLocationRequestState { coordinates ->
        editLatitudeDraft = formatCoordinateInput(coordinates.latitude)
        editLongitudeDraft = formatCoordinateInput(coordinates.longitude)
        viewModel.clearStatus()
    }

    LaunchedEffect(attemptedInitialLocation, draftHasLocation, context, locationProvider) {
        if (!attemptedInitialLocation) {
            attemptedInitialLocation = true
            if (!draftHasLocation && context.hasLocationPermission()) {
                when (val result = locationProvider.getCurrentLocation()) {
                    is CurrentLocationLookupResult.Success -> {
                        latitudeDraft = formatCoordinateInput(result.coordinates.latitude)
                        longitudeDraft = formatCoordinateInput(result.coordinates.longitude)
                    }

                    is CurrentLocationLookupResult.Failure -> Unit
                }
            }
        }
    }

    fun openEditor(note: ShotNoteEntity) {
        editingNoteId = note.id
        editShotLabelDraft = note.shotLabel
        editNoteDraft = note.noteText
        editLatitudeDraft = note.latitude?.let(::formatCoordinateInput).orEmpty()
        editLongitudeDraft = note.longitude?.let(::formatCoordinateInput).orEmpty()
        showEditor = true
        viewModel.clearStatus()
    }

    fun closeEditor() {
        showEditor = false
        editingNoteId = 0L
        editShotLabelDraft = ""
        editNoteDraft = ""
        editLatitudeDraft = ""
        editLongitudeDraft = ""
        viewModel.clearStatus()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = "Shot notes",
                subtitle = "Capture fast field notes with automatic timestamps and optional current-location snapshots.",
            )
        }
        item {
            ShotNoteComposerCard(
                shotLabel = shotLabelDraft,
                onShotLabelChange = {
                    shotLabelDraft = it
                    viewModel.clearStatus()
                },
                noteText = noteDraft,
                onNoteTextChange = {
                    noteDraft = it
                    viewModel.clearStatus()
                },
                locationState = draftLocationState,
                hasLocation = draftHasLocation,
                locationSummary = formatLocationSummary(latitudeDraft, longitudeDraft),
                onClearLocation = {
                    latitudeDraft = ""
                    longitudeDraft = ""
                    viewModel.clearStatus()
                },
                speechRecognitionAvailable = speechRecognitionAvailable,
                statusMessage = status,
                onDictate = {
                    viewModel.clearStatus()
                    dictationLauncher.launch(buildDictationIntent())
                },
                onClearDraft = {
                    shotLabelDraft = ""
                    noteDraft = ""
                    viewModel.clearStatus()
                },
                onSave = {
                    val coordinates = parseCoordinates(latitudeDraft, longitudeDraft)
                    if (
                        viewModel.saveNote(
                            existing = null,
                            shotLabel = shotLabelDraft,
                            noteText = noteDraft,
                            latitude = coordinates?.first,
                            longitude = coordinates?.second,
                        )
                    ) {
                        shotLabelDraft = ""
                        noteDraft = ""
                    }
                },
            )
        }
        if (notes.isEmpty()) {
            item {
                EmptyShotNotesCard()
            }
        } else {
            items(notes, key = { note -> note.id }) { note ->
                ShotNoteCard(
                    note = note,
                    onEdit = { openEditor(note) },
                    onDelete = { viewModel.deleteNote(note) },
                )
            }
        }
    }

    if (showEditor && editingNote != null) {
        ShotNoteDialog(
            state = ShotNoteEditorState(
                shotLabel = editShotLabelDraft,
                noteText = editNoteDraft,
                latitude = editLatitudeDraft,
                longitude = editLongitudeDraft,
            ),
            currentLocationState = editLocationState,
            statusMessage = status,
            onDismiss = { closeEditor() },
            onShotLabelChange = {
                editShotLabelDraft = it
                viewModel.clearStatus()
            },
            onNoteTextChange = {
                editNoteDraft = it
                viewModel.clearStatus()
            },
            onClearLocation = {
                editLatitudeDraft = ""
                editLongitudeDraft = ""
                viewModel.clearStatus()
            },
            onSave = {
                val coordinates = parseCoordinates(editLatitudeDraft, editLongitudeDraft)
                if (
                    viewModel.saveNote(
                        existing = editingNote,
                        shotLabel = editShotLabelDraft,
                        noteText = editNoteDraft,
                        latitude = coordinates?.first,
                        longitude = coordinates?.second,
                    )
                ) {
                    closeEditor()
                }
            },
        )
    }
}

@Composable
private fun ShotNoteComposerCard(
    shotLabel: String,
    onShotLabelChange: (String) -> Unit,
    noteText: String,
    onNoteTextChange: (String) -> Unit,
    locationState: CurrentLocationRequestState,
    hasLocation: Boolean,
    locationSummary: String?,
    onClearLocation: () -> Unit,
    speechRecognitionAvailable: Boolean,
    statusMessage: String?,
    onDictate: () -> Unit,
    onClearDraft: () -> Unit,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "New note",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Save a quick note for the current shot, subject, or setup. Dictation uses the device speech recognizer when it is available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LabeledField(
                label = "Shot / subject label",
                value = shotLabel,
                onValueChange = onShotLabelChange,
                keyboardType = KeyboardType.Text,
            )
            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteTextChange,
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
            )
            if (!speechRecognitionAvailable) {
                Text(
                    text = "Speech recognition is not available on this device. You can still type notes normally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CurrentLocationAction(
                state = locationState,
                buttonLabel = if (hasLocation) "Refresh current location" else "Use current location",
            )
            if (locationSummary != null) {
                Text(
                    text = "Attached location: $locationSummary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onClearLocation,
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Text("Clear location")
                }
            }
            statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onDictate,
                    enabled = speechRecognitionAvailable,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Dictate note")
                }
                OutlinedButton(
                    onClick = onClearDraft,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Clear draft")
                }
                Button(
                    onClick = onSave,
                    enabled = noteText.trim().isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save note")
                }
            }
        }
    }
}

@Composable
private fun EmptyShotNotesCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "No shot notes yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Saved notes stay on the device so you can capture quick observations, dictated reminders, and GPS-linked context while shooting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShotNoteCard(
    note: ShotNoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                        text = note.shotLabel.ifBlank { "Shot note" },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = formatShotNoteTimestamp(note.createdAtMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (note.updatedAtMillis > note.createdAtMillis) {
                        Text(
                            text = "Edited ${formatShotNoteTimestamp(note.updatedAtMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit note")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete note")
                    }
                }
            }
            Text(
                text = note.noteText,
                style = MaterialTheme.typography.bodyLarge,
            )
            formatLocationSummary(
                latitude = note.latitude?.let(::formatCoordinateInput).orEmpty(),
                longitude = note.longitude?.let(::formatCoordinateInput).orEmpty(),
            )?.let { locationSummary ->
                Text(
                    text = "Location: $locationSummary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ShotNoteDialog(
    state: ShotNoteEditorState,
    currentLocationState: CurrentLocationRequestState,
    statusMessage: String?,
    onDismiss: () -> Unit,
    onShotLabelChange: (String) -> Unit,
    onNoteTextChange: (String) -> Unit,
    onClearLocation: () -> Unit,
    onSave: () -> Unit,
) {
    val hasLocation = state.latitude.isNotBlank() && state.longitude.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit note") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabeledField(
                    label = "Shot / subject label",
                    value = state.shotLabel,
                    onValueChange = onShotLabelChange,
                    keyboardType = KeyboardType.Text,
                )
                OutlinedTextField(
                    value = state.noteText,
                    onValueChange = onNoteTextChange,
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
                CurrentLocationAction(
                    state = currentLocationState,
                    buttonLabel = if (hasLocation) "Refresh current location" else "Use current location",
                )
                formatLocationSummary(state.latitude, state.longitude)?.let { locationSummary ->
                    Text(
                        text = "Attached location: $locationSummary",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onClearLocation) {
                        Text("Clear location")
                    }
                }
                statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = state.noteText.trim().isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun buildDictationIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your shot note")
    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
}

private fun formatLocationSummary(
    latitude: String,
    longitude: String,
): String? = if (latitude.isNotBlank() && longitude.isNotBlank()) {
    "$latitude, $longitude"
} else {
    null
}

private fun parseCoordinates(
    latitude: String,
    longitude: String,
): Pair<Double, Double>? {
    val latitudeValue = latitude.toDoubleOrNull()
    val longitudeValue = longitude.toDoubleOrNull()
    return if (latitudeValue != null && longitudeValue != null) {
        latitudeValue to longitudeValue
    } else {
        null
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
