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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.ShootEntity
import com.janhorak.shutterdeck.ui.components.LabeledField

@Composable
fun ShootsScreen(
    onOpenShoot: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShootsViewModel = hiltViewModel(),
) {
    val shoots by viewModel.shoots.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ShootEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        ShootDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { id, title, date, notes ->
                viewModel.save(id, title, date, notes)
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
        items(shoots, key = { it.id }) { shoot ->
            ShootCard(
                shoot = shoot,
                onOpen = { onOpenShoot(shoot.id) },
                onDelete = { viewModel.delete(shoot) },
            )
        }
    }
}

@Composable
private fun ShootCard(
    shoot: ShootEntity,
    onOpen: () -> Unit,
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
                    text = shoot.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
            if (shoot.dateText.isNotBlank()) {
                Text(
                    text = shoot.dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (shoot.notes.isNotBlank()) {
                Text(
                    text = shoot.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ShootDialog(
    initial: ShootEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var date by remember { mutableStateOf(initial?.dateText ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New shoot" else "Edit shoot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("Title", title, { title = it }, keyboardType = KeyboardType.Text)
                LabeledField("Date", date, { date = it }, keyboardType = KeyboardType.Text)
                LabeledField("Notes", notes, { notes = it }, keyboardType = KeyboardType.Text, singleLine = false)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(initial?.id ?: 0L, title, date, notes) },
                enabled = title.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
