package com.janhorak.shutterdeck.planner.presentation

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

@Composable
fun LocationsScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationsViewModel = hiltViewModel(),
) {
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<LocationEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        LocationDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { id, name, lat, lon, notes, bestTime ->
                viewModel.save(id, name, lat, lon, notes, bestTime)
                showDialog = false
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
                onClick = { editing = null; showDialog = true },
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
                onEdit = { editing = location; showDialog = true },
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
    initial: LocationEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, String, Double?, Double?, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var latitude by remember { mutableStateOf(initial?.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(initial?.longitude?.toString() ?: "") }
    var bestTime by remember { mutableStateOf(initial?.bestTime ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add location" else "Edit location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("Name", name, { name = it }, keyboardType = KeyboardType.Text)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("Latitude", latitude, { latitude = it }, modifier = Modifier.weight(1f), suffix = "°")
                    LabeledField("Longitude", longitude, { longitude = it }, modifier = Modifier.weight(1f), suffix = "°")
                }
                LabeledField("Best time", bestTime, { bestTime = it }, keyboardType = KeyboardType.Text)
                LabeledField("Notes", notes, { notes = it }, keyboardType = KeyboardType.Text, singleLine = false)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial?.id ?: 0L,
                        name,
                        latitude.toDoubleOrNull(),
                        longitude.toDoubleOrNull(),
                        notes,
                        bestTime,
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
