package com.janhorak.shutterdeck.planner.presentation

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.LocationEntity
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
)

@Composable
fun LocationsScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationsViewModel = hiltViewModel(),
) {
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<LocationEditorState?>(null) }
    val currentLocationState = rememberCurrentLocationRequestState { coordinates ->
        editing = editing?.copy(
            latitude = formatCoordinateInput(coordinates.latitude),
            longitude = formatCoordinateInput(coordinates.longitude),
        )
    }

    if (editing != null) {
        LocationDialog(
            state = editing!!,
            currentLocationState = currentLocationState,
            onDismiss = { editing = null },
            onNameChange = { editing = editing?.copy(name = it) },
            onLatitudeChange = { editing = editing?.copy(latitude = it) },
            onLongitudeChange = { editing = editing?.copy(longitude = it) },
            onBestTimeChange = { editing = editing?.copy(bestTime = it) },
            onNotesChange = { editing = editing?.copy(notes = it) },
            onSave = {
                val current = editing ?: return@LocationDialog
                viewModel.save(
                    id = current.id,
                    name = current.name,
                    latitude = current.latitude.toDoubleOrNull(),
                    longitude = current.longitude.toDoubleOrNull(),
                    notes = current.notes,
                    bestTime = current.bestTime,
                )
                editing = null
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedButton(
                onClick = { editing = LocationEditorState() },
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
                onEdit = { editing = location.toEditorState() },
                onDelete = { viewModel.delete(location) },
            )
        }
    }
}

@Composable
private fun LocationCard(
    location: LocationEntity,
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
            }
            if (location.bestTime.isNotBlank()) {
                Text(
                    text = "Best: ${location.bestTime}",
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
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onBestTimeChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
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
                LabeledField("Best time", state.bestTime, onBestTimeChange, keyboardType = KeyboardType.Text)
                LabeledField("Notes", state.notes, onNotesChange, keyboardType = KeyboardType.Text, singleLine = false)
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

private fun LocationEntity.toEditorState(): LocationEditorState =
    LocationEditorState(
        id = id,
        name = name,
        latitude = latitude?.let(::formatCoordinateInput) ?: "",
        longitude = longitude?.let(::formatCoordinateInput) ?: "",
        bestTime = bestTime,
        notes = notes,
    )
