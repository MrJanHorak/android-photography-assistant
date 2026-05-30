package com.janhorak.shutterdeck.planner.presentation

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.LocationEntity
import com.janhorak.shutterdeck.core.data.db.ShotItemEntity
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.SectionHeader

@Composable
fun ShootDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: ShootDetailViewModel = hiltViewModel(),
) {
    val headerState by viewModel.headerState.collectAsStateWithLifecycle()
    val shots by viewModel.shots.collectAsStateWithLifecycle()
    var showShotDialog by rememberSaveable { mutableStateOf(false) }
    var editingShotId by rememberSaveable { mutableStateOf(0L) }
    var shotDescriptionDraft by rememberSaveable { mutableStateOf("") }
    var shotGearDraft by rememberSaveable { mutableStateOf("") }
    var shotNotesDraft by rememberSaveable { mutableStateOf("") }
    val shoot = headerState.shoot
    val editingShot = remember(editingShotId, shots) {
        shots.firstOrNull { it.id == editingShotId }
    }

    val doneCount = shots.count { it.done }

    fun clearShotDraft() {
        showShotDialog = false
        editingShotId = 0L
        shotDescriptionDraft = ""
        shotGearDraft = ""
        shotNotesDraft = ""
    }

    fun editShot(shot: ShotItemEntity) {
        editingShotId = shot.id
        shotDescriptionDraft = shot.description
        shotGearDraft = shot.gearNotes
        shotNotesDraft = shot.notes
        showShotDialog = true
    }

    if (showShotDialog && (editingShotId == 0L || editingShot != null)) {
        ShotDialog(
            isEditing = editingShotId != 0L,
            description = shotDescriptionDraft,
            gearNotes = shotGearDraft,
            notes = shotNotesDraft,
            onDescriptionChange = { shotDescriptionDraft = it },
            onGearNotesChange = { shotGearDraft = it },
            onNotesChange = { shotNotesDraft = it },
            onDismiss = ::clearShotDraft,
            onSave = {
                viewModel.saveShot(
                    existing = editingShot,
                    description = shotDescriptionDraft,
                    gearNotes = shotGearDraft,
                    notes = shotNotesDraft,
                )
                clearShotDraft()
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SectionHeader(
                title = shoot?.title ?: "Shoot",
                subtitle = buildString {
                    shoot?.dateText?.takeIf { it.isNotBlank() }?.let { append(it).append(" · ") }
                    append("$doneCount / ${shots.size} shots done")
                },
            )
            shoot?.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (headerState.location != null || headerState.locationMissing) {
            item {
                LinkedLocationCard(
                    location = headerState.location,
                    locationMissing = headerState.locationMissing,
                )
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    editingShotId = 0L
                    shotDescriptionDraft = ""
                    shotGearDraft = ""
                    shotNotesDraft = ""
                    showShotDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add shot")
            }
            HorizontalDivider()
        }
        if (shots.isEmpty()) {
            item {
                Text(
                    "No shots yet. Add the shots you want to capture on this shoot.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(shots, key = { it.id }) { shot ->
            ShotRow(
                shot = shot,
                onToggle = { viewModel.toggleDone(shot) },
                onEdit = { editShot(shot) },
                onDelete = { viewModel.delete(shot) },
            )
        }
    }
}

@Composable
private fun LinkedLocationCard(
    location: LocationEntity?,
    locationMissing: Boolean,
) {
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Linked location",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (location != null) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                val latitude = location.latitude
                val longitude = location.longitude
                if (latitude != null && longitude != null) {
                    Text(
                        text = "%.4f, %.4f".format(latitude, longitude),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (location.bestTime.isNotBlank()) {
                    Text(
                        text = "Best time: ${location.bestTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (location.notes.isNotBlank()) {
                    Text(
                        text = location.notes,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Text(
                    text = if (locationMissing) {
                        "Linked location removed. Edit the shoot from the shoot list to choose another spot."
                    } else {
                        "No linked location."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ShotRow(
    shot: ShotItemEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = shot.done, onCheckedChange = { onToggle() })
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = shot.description,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (shot.done) TextDecoration.LineThrough else null,
                color = if (shot.done) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            shot.gearNotes.takeIf { it.isNotBlank() }?.let { gearNotes ->
                Text(
                    text = "Gear: $gearNotes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            shot.notes.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
    }
}

@Composable
private fun ShotDialog(
    isEditing: Boolean,
    description: String,
    gearNotes: String,
    notes: String,
    onDescriptionChange: (String) -> Unit,
    onGearNotesChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit shot" else "Add shot") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabeledField(
                    label = "Shot",
                    value = description,
                    onValueChange = onDescriptionChange,
                    keyboardType = KeyboardType.Text,
                )
                LabeledField(
                    label = "Gear",
                    value = gearNotes,
                    onValueChange = onGearNotesChange,
                    keyboardType = KeyboardType.Text,
                    singleLine = false,
                )
                LabeledField(
                    label = "Notes",
                    value = notes,
                    onValueChange = onNotesChange,
                    keyboardType = KeyboardType.Text,
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = description.trim().isNotBlank(),
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
