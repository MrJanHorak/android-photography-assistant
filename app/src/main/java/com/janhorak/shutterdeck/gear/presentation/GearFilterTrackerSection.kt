package com.janhorak.shutterdeck.gear.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.ui.components.LabeledField

@Composable
internal fun FilterEditorCard(
    initial: GearFilterSummary?,
    onSave: (Long, String, String, String, String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var label by remember(initial?.filter?.id) { mutableStateOf(initial?.filter?.label ?: "") }
    var filterType by remember(initial?.filter?.id) {
        mutableStateOf(initial?.filter?.filterType ?: filterTypeOptions.first())
    }
    var threadSizeText by remember(initial?.filter?.id) {
        mutableStateOf(initial?.filter?.threadSizeText ?: "")
    }
    var strengthText by remember(initial?.filter?.id) {
        mutableStateOf(initial?.filter?.strengthText ?: "")
    }
    var notes by remember(initial?.filter?.id) { mutableStateOf(initial?.filter?.notes ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (initial == null) "Add filter" else "Edit filter",
                style = MaterialTheme.typography.titleMedium,
            )
            LabeledField(
                label = "Filter name",
                value = label,
                onValueChange = { label = it },
                keyboardType = KeyboardType.Text,
            )
            FilterTypeChips(
                selectedType = filterType,
                onSelectedTypeChange = { filterType = it },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Thread size",
                    value = threadSizeText,
                    onValueChange = { threadSizeText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
                LabeledField(
                    label = "Strength / details",
                    value = strengthText,
                    onValueChange = { strengthText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
            }
            Text(
                text = "Examples: 67, 67mm, 82 mm, 100mm system",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LabeledField(
                label = "Notes",
                value = notes,
                onValueChange = { notes = it },
                keyboardType = KeyboardType.Text,
                singleLine = false,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
                OutlinedButton(
                    onClick = {
                        onSave(
                            initial?.filter?.id ?: 0L,
                            label,
                            filterType,
                            threadSizeText,
                            strengthText,
                            notes,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = label.isNotBlank(),
                ) {
                    Text(if (initial == null) "Save" else "Update")
                }
            }
        }
    }
}

@Composable
internal fun FilterCard(
    summary: GearFilterSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.filter.label,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = buildList {
                            add(summary.filter.filterType)
                            formatThreadSizeText(summary.filter.threadSizeText)
                                .takeIf { it.isNotBlank() }
                                ?.let { add(it) }
                        }.joinToString(" · ").ifBlank { "Thread size not saved" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
            if (summary.filter.strengthText.isNotBlank()) {
                Text(
                    text = summary.filter.strengthText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = when {
                    summary.normalizedThreadKey == null ->
                        "Add a thread size to compare this filter against saved lenses."
                    summary.compatibleLensLabels.isEmpty() ->
                        "No saved lenses match this size yet."
                    else ->
                        "Fits ${summary.compatibleLensLabels.joinToString()}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary.filter.notes.isNotBlank()) {
                Text(
                    text = summary.filter.notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
internal fun LensThreadCompatibilityCard(
    summary: LensThreadCompatibilitySummary,
    onEdit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = gearDisplayName(summary.lens),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = formatThreadSizeText(summary.lens.filterThreadSizeText)
                            .takeIf { it.isNotBlank() }
                            ?.let { "Thread $it" }
                            ?: "Thread size not saved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
            }
            Text(
                text = when {
                    summary.normalizedThreadKey == null ->
                        "Add this lens's filter thread size to see which saved filters fit."
                    summary.compatibleFilterLabels.isEmpty() ->
                        "No saved filters match this size yet."
                    else ->
                        "Matching filters: ${summary.compatibleFilterLabels.joinToString()}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilterTypeChips(
    selectedType: String,
    onSelectedTypeChange: (String) -> Unit,
) {
    Text(
        text = "Filter type",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        filterTypeOptions.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    FilterChip(
                        selected = selectedType == option,
                        onClick = { onSelectedTypeChange(option) },
                        label = { Text(option) },
                    )
                }
            }
        }
    }
}
