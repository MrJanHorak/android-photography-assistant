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
import androidx.compose.material3.RadioButton
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
import com.janhorak.shutterdeck.core.data.db.GearItemEntity
import com.janhorak.shutterdeck.ui.components.LabeledField

@Composable
internal fun BatteryEditorCard(
    initial: GearBatterySummary?,
    availableItems: List<GearItemEntity>,
    onSave: (Long, Long?, String, Int?, Int?, Int?, String, String, String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var linkedGearItemId by remember(initial?.battery?.id) {
        mutableStateOf(initial?.battery?.linkedGearItemId)
    }
    var label by remember(initial?.battery?.id) { mutableStateOf(initial?.battery?.label ?: "") }
    var capacityMah by remember(initial?.battery?.id) {
        mutableStateOf(initial?.battery?.capacityMah?.toString() ?: "")
    }
    var healthPercent by remember(initial?.battery?.id) {
        mutableStateOf(initial?.battery?.healthPercent?.toString() ?: "")
    }
    var chargePercent by remember(initial?.battery?.id) {
        mutableStateOf(initial?.battery?.chargePercent?.toString() ?: "")
    }
    var status by remember(initial?.battery?.id) {
        mutableStateOf(initial?.battery?.status ?: batteryStatusOptions.first())
    }
    var lastChargedText by remember(initial?.battery?.id) {
        mutableStateOf(initial?.battery?.lastChargedText ?: "")
    }
    var lastCheckedText by remember(initial?.battery?.id) {
        mutableStateOf(initial?.battery?.lastCheckedText ?: "")
    }
    var notes by remember(initial?.battery?.id) { mutableStateOf(initial?.battery?.notes ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (initial == null) "Add battery" else "Edit battery",
                style = MaterialTheme.typography.titleMedium,
            )
            LabeledField(
                label = "Label",
                value = label,
                onValueChange = { label = it },
                keyboardType = KeyboardType.Text,
            )
            GearAssignmentPicker(
                title = "Linked gear",
                description = "Leave this unassigned if it is a loose spare or shared pack.",
                availableItems = availableItems,
                selectedItemId = linkedGearItemId,
                onSelectedItemIdChange = { linkedGearItemId = it },
                unassignedLabel = "Unassigned / spare",
            )
            SelectableChipRows(
                title = "Battery state",
                options = batteryStatusOptions,
                selectedOption = status,
                onSelectedOptionChange = { status = it },
                itemsPerRow = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Capacity",
                    value = capacityMah,
                    onValueChange = { capacityMah = it },
                    modifier = Modifier.weight(1f),
                    suffix = "mAh",
                    keyboardType = KeyboardType.Number,
                )
                LabeledField(
                    label = "Health",
                    value = healthPercent,
                    onValueChange = { healthPercent = it },
                    modifier = Modifier.weight(1f),
                    suffix = "%",
                    keyboardType = KeyboardType.Number,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Charge level",
                    value = chargePercent,
                    onValueChange = { chargePercent = it },
                    modifier = Modifier.weight(1f),
                    suffix = "%",
                    keyboardType = KeyboardType.Number,
                )
                LabeledField(
                    label = "Last charged",
                    value = lastChargedText,
                    onValueChange = { lastChargedText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
            }
            LabeledField(
                label = "Charge checked",
                value = lastCheckedText,
                onValueChange = { lastCheckedText = it },
                keyboardType = KeyboardType.Text,
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
                            initial?.battery?.id ?: 0L,
                            linkedGearItemId,
                            label,
                            capacityMah.toIntOrNull(),
                            healthPercent.toIntOrNull(),
                            chargePercent.toIntOrNull(),
                            status,
                            lastChargedText,
                            lastCheckedText,
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
internal fun BatteryCard(
    summary: GearBatterySummary,
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
                        text = summary.battery.label,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${summary.battery.status} · ${summary.linkedItemLabel}",
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
            val detailLine = buildList {
                summary.battery.capacityMah?.let { add(formatBatteryCapacityMah(it.toLong())) }
                summary.battery.healthPercent?.let { add("Health ${formatPercent(it)}") }
                summary.battery.chargePercent?.let { add("Charge ${formatPercent(it)}") }
                if (summary.battery.lastChargedText.isNotBlank()) add("Charged ${summary.battery.lastChargedText}")
                if (summary.battery.lastCheckedText.isNotBlank()) add("Checked ${summary.battery.lastCheckedText}")
            }.joinToString(" · ").ifBlank { "No battery stats recorded yet" }
            Text(
                text = detailLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary.battery.notes.isNotBlank()) {
                Text(
                    text = summary.battery.notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
internal fun MemoryCardEditorCard(
    initial: MemoryCardSummary?,
    availableItems: List<GearItemEntity>,
    onSave: (Long, Long?, String, String, Int?, String, String, String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var linkedGearItemId by remember(initial?.card?.id) {
        mutableStateOf(initial?.card?.linkedGearItemId)
    }
    var label by remember(initial?.card?.id) { mutableStateOf(initial?.card?.label ?: "") }
    var cardType by remember(initial?.card?.id) {
        mutableStateOf(initial?.card?.cardType ?: memoryCardTypeOptions.first())
    }
    var capacityGb by remember(initial?.card?.id) {
        mutableStateOf(initial?.card?.capacityGb?.toString() ?: "")
    }
    var speedLabel by remember(initial?.card?.id) {
        mutableStateOf(initial?.card?.speedLabel ?: "")
    }
    var status by remember(initial?.card?.id) {
        mutableStateOf(initial?.card?.status ?: memoryCardStatusOptions.first())
    }
    var lastFormattedText by remember(initial?.card?.id) {
        mutableStateOf(initial?.card?.lastFormattedText ?: "")
    }
    var notes by remember(initial?.card?.id) { mutableStateOf(initial?.card?.notes ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (initial == null) "Add memory card" else "Edit memory card",
                style = MaterialTheme.typography.titleMedium,
            )
            LabeledField(
                label = "Label",
                value = label,
                onValueChange = { label = it },
                keyboardType = KeyboardType.Text,
            )
            SelectableChipRows(
                title = "Card type",
                options = memoryCardTypeOptions,
                selectedOption = cardType,
                onSelectedOptionChange = { cardType = it },
                itemsPerRow = 2,
            )
            GearAssignmentPicker(
                title = "Linked gear",
                description = "Leave this unassigned if the card moves between bodies or readers.",
                availableItems = availableItems,
                selectedItemId = linkedGearItemId,
                onSelectedItemIdChange = { linkedGearItemId = it },
                unassignedLabel = "Unassigned / shared",
            )
            SelectableChipRows(
                title = "Card state",
                options = memoryCardStatusOptions,
                selectedOption = status,
                onSelectedOptionChange = { status = it },
                itemsPerRow = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Capacity",
                    value = capacityGb,
                    onValueChange = { capacityGb = it },
                    modifier = Modifier.weight(1f),
                    suffix = "GB",
                    keyboardType = KeyboardType.Number,
                )
                LabeledField(
                    label = "Speed / label",
                    value = speedLabel,
                    onValueChange = { speedLabel = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
            }
            LabeledField(
                label = "Last formatted",
                value = lastFormattedText,
                onValueChange = { lastFormattedText = it },
                keyboardType = KeyboardType.Text,
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
                            initial?.card?.id ?: 0L,
                            linkedGearItemId,
                            label,
                            cardType,
                            capacityGb.toIntOrNull(),
                            speedLabel,
                            status,
                            lastFormattedText,
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
internal fun MemoryCardCard(
    summary: MemoryCardSummary,
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
                        text = summary.card.label,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${summary.card.status} · ${summary.linkedItemLabel}",
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
            val detailLine = buildList {
                if (summary.card.cardType.isNotBlank()) add(summary.card.cardType)
                summary.card.capacityGb?.let { add(formatStorageCapacityGb(it.toLong())) }
                if (summary.card.speedLabel.isNotBlank()) add(summary.card.speedLabel)
                if (summary.card.lastFormattedText.isNotBlank()) add("Formatted ${summary.card.lastFormattedText}")
            }.joinToString(" · ").ifBlank { "No memory-card details recorded yet" }
            Text(
                text = detailLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary.card.notes.isNotBlank()) {
                Text(
                    text = summary.card.notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
internal fun GearAssignmentPicker(
    title: String,
    description: String,
    availableItems: List<GearItemEntity>,
    selectedItemId: Long?,
    onSelectedItemIdChange: (Long?) -> Unit,
    unassignedLabel: String,
) {
    val sortedItems = availableItems.sortedBy { gearDisplayName(it).lowercase() }

    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selectedItemId == null,
            onClick = { onSelectedItemIdChange(null) },
        )
        Text(
            text = unassignedLabel,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    sortedItems.forEach { item ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selectedItemId == item.id,
                onClick = { onSelectedItemIdChange(item.id) },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gearDisplayName(item),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (sortedItems.isEmpty()) {
        Text(
            text = "No saved gear yet. Save this item now and link it later if you want.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SelectableChipRows(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelectedOptionChange: (String) -> Unit,
    itemsPerRow: Int = 3,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(itemsPerRow).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    FilterChip(
                        selected = selectedOption == option,
                        onClick = { onSelectedOptionChange(option) },
                        label = { Text(option) },
                    )
                }
            }
        }
    }
}
