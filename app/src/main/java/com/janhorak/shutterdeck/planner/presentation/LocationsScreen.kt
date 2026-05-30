package com.janhorak.shutterdeck.planner.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.LocationEntity
import com.janhorak.shutterdeck.core.storage.documentAttachmentLabel
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.location.CurrentLocationAction
import com.janhorak.shutterdeck.ui.location.CurrentLocationRequestState
import com.janhorak.shutterdeck.ui.location.formatCoordinateInput
import com.janhorak.shutterdeck.ui.location.rememberCurrentLocationRequestState

private data class LocationEditorState(
    val id: Long = 0L,
    val name: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val bestTime: String = "",
    val notes: String = "",
    val referencePhotoUri: String = "",
)

@Composable
fun LocationsScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationsViewModel = hiltViewModel(),
) {
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingLocationId by rememberSaveable { mutableStateOf(0L) }
    var nameDraft by rememberSaveable { mutableStateOf("") }
    var latitudeDraft by rememberSaveable { mutableStateOf("") }
    var longitudeDraft by rememberSaveable { mutableStateOf("") }
    var bestTimeDraft by rememberSaveable { mutableStateOf("") }
    var notesDraft by rememberSaveable { mutableStateOf("") }
    var referencePhotoUriDraft by rememberSaveable { mutableStateOf("") }
    var previewLocationId by rememberSaveable { mutableStateOf(0L) }
    val editing = locations.firstOrNull { it.id == editingLocationId }
    val previewLocation = locations.firstOrNull { it.id == previewLocationId }
    val referencePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            referencePhotoUriDraft = uri.toString()
        }
    }
    val currentLocationState = rememberCurrentLocationRequestState { coordinates ->
        latitudeDraft = formatCoordinateInput(coordinates.latitude)
        longitudeDraft = formatCoordinateInput(coordinates.longitude)
    }

    fun clearEditor() {
        showEditor = false
        editingLocationId = 0L
        nameDraft = ""
        latitudeDraft = ""
        longitudeDraft = ""
        bestTimeDraft = ""
        notesDraft = ""
        referencePhotoUriDraft = ""
        viewModel.clearStatus()
    }

    fun openEditor(location: LocationEntity?) {
        editingLocationId = location?.id ?: 0L
        nameDraft = location?.name.orEmpty()
        latitudeDraft = location?.latitude?.let(::formatCoordinateInput) ?: ""
        longitudeDraft = location?.longitude?.let(::formatCoordinateInput) ?: ""
        bestTimeDraft = location?.bestTime.orEmpty()
        notesDraft = location?.notes.orEmpty()
        referencePhotoUriDraft = location?.referencePhotoUri.orEmpty()
        showEditor = true
        viewModel.clearStatus()
    }

    if (showEditor && (editingLocationId == 0L || editing != null)) {
        LocationDialog(
            state = LocationEditorState(
                id = editingLocationId,
                name = nameDraft,
                latitude = latitudeDraft,
                longitude = longitudeDraft,
                bestTime = bestTimeDraft,
                notes = notesDraft,
                referencePhotoUri = referencePhotoUriDraft,
            ),
            currentLocationState = currentLocationState,
            statusMessage = status,
            onDismiss = ::clearEditor,
            onNameChange = { nameDraft = it },
            onLatitudeChange = { latitudeDraft = it },
            onLongitudeChange = { longitudeDraft = it },
            onBestTimeChange = { bestTimeDraft = it },
            onNotesChange = { notesDraft = it },
            onPickReferencePhoto = {
                referencePhotoPickerLauncher.launch(arrayOf("image/*"))
            },
            onClearReferencePhoto = { referencePhotoUriDraft = "" },
            onSave = {
                viewModel.save(
                    id = editingLocationId,
                    name = nameDraft,
                    latitude = latitudeDraft.toDoubleOrNull(),
                    longitude = longitudeDraft.toDoubleOrNull(),
                    notes = notesDraft,
                    bestTime = bestTimeDraft,
                    referencePhotoUri = referencePhotoUriDraft,
                    onSuccess = { clearEditor() },
                )
            },
        )
    }

    if (previewLocation != null && previewLocation.latitude != null && previewLocation.longitude != null) {
        LocationMapDialog(
            location = previewLocation,
            onDismiss = { previewLocationId = 0L },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedButton(
                onClick = { openEditor(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add location")
            }
        }
        if (locations.isEmpty()) {
            item {
                Text(
                    "No saved locations yet. Add a spot you want to shoot, with its coordinates and best time of day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(locations, key = { it.id }) { location ->
            LocationCard(
                location = location,
                onViewMap = { previewLocationId = location.id },
                onEdit = { openEditor(location) },
                onDelete = { viewModel.delete(location) },
            )
        }
    }
}

@Composable
private fun LocationCard(
    location: LocationEntity,
    onViewMap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
            val lat = location.latitude
            val lon = location.longitude
            if (lat != null && lon != null) {
                Text(
                    text = "%.4f, %.4f".format(lat, lon),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onViewMap) {
                    Text("View map")
                }
            }
            if (location.bestTime.isNotBlank()) {
                Text(
                    text = "Best: ${location.bestTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (location.referencePhotoUri.isNotBlank()) {
                Text(
                    text = "Reference photo: ${documentAttachmentLabel(location.referencePhotoUri)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (location.notes.isNotBlank()) {
                Text(
                    text = location.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun LocationDialog(
    state: LocationEditorState,
    currentLocationState: CurrentLocationRequestState,
    statusMessage: String?,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onBestTimeChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPickReferencePhoto: () -> Unit,
    onClearReferencePhoto: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == 0L) "Add location" else "Edit location") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                LabeledField("Name", state.name, onNameChange, keyboardType = KeyboardType.Text)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("Latitude", state.latitude, onLatitudeChange, modifier = Modifier.weight(1f), suffix = "°")
                    LabeledField("Longitude", state.longitude, onLongitudeChange, modifier = Modifier.weight(1f), suffix = "°")
                }
                CurrentLocationAction(state = currentLocationState)
                Text(
                    text = "Reference photo",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (state.referencePhotoUri.isBlank()) {
                        "No reference photo attached yet."
                    } else {
                        "Attached: ${documentAttachmentLabel(state.referencePhotoUri)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPickReferencePhoto,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.referencePhotoUri.isBlank()) "Choose photo" else "Replace photo")
                    }
                    if (state.referencePhotoUri.isNotBlank()) {
                        OutlinedButton(
                            onClick = onClearReferencePhoto,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Clear photo")
                        }
                    }
                }
                LabeledField("Best time", state.bestTime, onBestTimeChange, keyboardType = KeyboardType.Text)
                LabeledField("Notes", state.notes, onNotesChange, keyboardType = KeyboardType.Text, singleLine = false)
                statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = state.name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
