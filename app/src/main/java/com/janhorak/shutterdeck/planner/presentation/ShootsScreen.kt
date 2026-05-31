package com.janhorak.shutterdeck.planner.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.LocationEntity
import com.janhorak.shutterdeck.core.data.db.ShootEntity
import com.janhorak.shutterdeck.ui.components.DatePickerField
import com.janhorak.shutterdeck.ui.components.LabeledField

@Composable
fun ShootsScreen(
    onOpenShoot: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShootsViewModel = hiltViewModel(),
) {
    val shoots by viewModel.shoots.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    var editingShootId by rememberSaveable { mutableStateOf(0L) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var titleDraft by rememberSaveable { mutableStateOf("") }
    var dateDraft by rememberSaveable { mutableStateOf("") }
    var notesDraft by rememberSaveable { mutableStateOf("") }
    var locationIdDraft by rememberSaveable { mutableStateOf<Long?>(null) }
    fun clearDraft() {
        editingShootId = 0L
        titleDraft = ""
        dateDraft = ""
        notesDraft = ""
        locationIdDraft = null
        showDialog = false
    }

    fun startNewShoot() {
        editingShootId = 0L
        titleDraft = ""
        dateDraft = ""
        notesDraft = ""
        locationIdDraft = null
        showDialog = true
    }

    fun startEditingShoot(shoot: ShootEntity) {
        editingShootId = shoot.id
        titleDraft = shoot.title
        dateDraft = shoot.dateText
        notesDraft = shoot.notes
        locationIdDraft = shoot.locationId?.takeIf { selectedId ->
            locations.any { it.id == selectedId }
        }
        showDialog = true
    }

    if (showDialog) {
        ShootDialog(
            isEditing = editingShootId != 0L,
            title = titleDraft,
            date = dateDraft,
            notes = notesDraft,
            selectedLocationId = locationIdDraft?.takeIf { selectedId ->
                locations.any { it.id == selectedId }
            },
            locations = locations,
            onTitleChange = { titleDraft = it },
            onDateChange = { dateDraft = it },
            onNotesChange = { notesDraft = it },
            onSelectedLocationIdChange = { locationIdDraft = it },
            onDismiss = ::clearDraft,
            onSave = {
                viewModel.save(
                    editingShootId,
                    titleDraft,
                    dateDraft,
                    notesDraft,
                    locationIdDraft?.takeIf { selectedId ->
                        locations.any { it.id == selectedId }
                    },
                )
                clearDraft()
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
                onClick = ::startNewShoot,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  New shoot")
            }
        }
        if (shoots.isEmpty()) {
            item {
                Text(
                    "No shoots planned yet. Create a shoot, then build its shot list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(shoots, key = { it.shoot.id }) { shoot ->
            ShootCard(
                shoot = shoot,
                onOpen = { onOpenShoot(shoot.shoot.id) },
                onEdit = { startEditingShoot(shoot.shoot) },
                onDelete = { viewModel.delete(shoot.shoot) },
            )
        }
    }
}

@Composable
private fun ShootCard(
    shoot: ShootListItemUiState,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = shoot.shoot.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
            if (shoot.shoot.dateText.isNotBlank()) {
                Text(
                    text = shoot.shoot.dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (shoot.locationName != null) {
                Text(
                    text = "Location: ${shoot.locationName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (shoot.locationMissing) {
                Text(
                    text = "Linked location removed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (shoot.shoot.notes.isNotBlank()) {
                Text(
                    text = shoot.shoot.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ShootDialog(
    isEditing: Boolean,
    title: String,
    date: String,
    notes: String,
    selectedLocationId: Long?,
    locations: List<LocationEntity>,
    onTitleChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSelectedLocationIdChange: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit shoot" else "New shoot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("Title", title, onTitleChange, keyboardType = KeyboardType.Text)
                DatePickerField("Date", date, onDateChange)
                LocationPickerField(
                    locations = locations,
                    selectedLocationId = selectedLocationId,
                    onSelectedLocationIdChange = onSelectedLocationIdChange,
                )
                LabeledField("Notes", notes, onNotesChange, keyboardType = KeyboardType.Text, singleLine = false)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = title.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LocationPickerField(
    locations: List<LocationEntity>,
    selectedLocationId: Long?,
    onSelectedLocationIdChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLocationName = remember(selectedLocationId, locations) {
        locations.firstOrNull { it.id == selectedLocationId }?.name ?: "No location"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Location",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selectedLocationName,
                    modifier = Modifier.weight(1f),
                )
                Text("Change")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("No location") },
                    onClick = {
                        onSelectedLocationIdChange(null)
                        expanded = false
                    },
                )
                locations.forEach { location ->
                    DropdownMenuItem(
                        text = { Text(location.name) },
                        onClick = {
                            onSelectedLocationIdChange(location.id)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (locations.isEmpty()) {
            Text(
                text = "Save a scouting location first if you want to link one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
