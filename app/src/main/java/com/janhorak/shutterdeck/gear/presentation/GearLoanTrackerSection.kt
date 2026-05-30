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
import com.janhorak.shutterdeck.core.data.db.GearItemEntity
import com.janhorak.shutterdeck.gear.domain.GearLoanReminderLevel
import com.janhorak.shutterdeck.ui.components.LabeledField

@Composable
internal fun GearLoanEditorCard(
    initial: GearLoanSummary?,
    availableItems: List<GearItemEntity>,
    onSave: (Long, Long?, String, String, String, String, String, String, String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var linkedGearItemId by remember(initial?.loan?.id) {
        mutableStateOf(initial?.loan?.linkedGearItemId)
    }
    var customItemLabel by remember(initial?.loan?.id) {
        mutableStateOf(initial?.loan?.customItemLabel ?: "")
    }
    var direction by remember(initial?.loan?.id) {
        mutableStateOf(initial?.loan?.direction ?: loanDirectionOptions.first())
    }
    var counterpartName by remember(initial?.loan?.id) {
        mutableStateOf(initial?.loan?.counterpartName ?: "")
    }
    var status by remember(initial?.loan?.id) {
        mutableStateOf(initial?.loan?.status ?: loanStatusOptions.first())
    }
    var startDateText by remember(initial?.loan?.id) {
        mutableStateOf(initial?.loan?.startDateText ?: "")
    }
    var dueDateText by remember(initial?.loan?.id) {
        mutableStateOf(initial?.loan?.dueDateText ?: "")
    }
    var returnedDateText by remember(initial?.loan?.id) {
        mutableStateOf(initial?.loan?.returnedDateText ?: "")
    }
    var notes by remember(initial?.loan?.id) { mutableStateOf(initial?.loan?.notes ?: "") }
    val canSave = counterpartName.isNotBlank() && (linkedGearItemId != null || customItemLabel.isNotBlank())

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (initial == null) "Add loan / rental" else "Edit loan / rental",
                style = MaterialTheme.typography.titleMedium,
            )
            SelectableChipRows(
                title = "Direction",
                options = loanDirectionOptions,
                selectedOption = direction,
                onSelectedOptionChange = { direction = it },
                itemsPerRow = 2,
            )
            SelectableChipRows(
                title = "Status",
                options = loanStatusOptions,
                selectedOption = status,
                onSelectedOptionChange = { status = it },
                itemsPerRow = 3,
            )
            GearAssignmentPicker(
                title = "Linked gear",
                description = "Link a saved item when this record belongs to your inventory, or leave it unassigned for borrowed/rented gear.",
                availableItems = availableItems,
                selectedItemId = linkedGearItemId,
                onSelectedItemIdChange = { linkedGearItemId = it },
                unassignedLabel = "Unassigned / external gear",
            )
            LabeledField(
                label = "Custom item label",
                value = customItemLabel,
                onValueChange = { customItemLabel = it },
                keyboardType = KeyboardType.Text,
            )
            Text(
                text = "Optional if the linked gear name is enough. Use this for borrowed or rented gear that is not in your inventory.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LabeledField(
                label = loanCounterpartFieldLabel(direction),
                value = counterpartName,
                onValueChange = { counterpartName = it },
                keyboardType = KeyboardType.Text,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = loanStartedLabel(direction),
                    value = startDateText,
                    onValueChange = { startDateText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
                LabeledField(
                    label = "Return due",
                    value = dueDateText,
                    onValueChange = { dueDateText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
            }
            Text(
                text = "Use YYYY-MM-DD for due-date reminders (example: 2026-05-30).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (status == "Returned" || returnedDateText.isNotBlank()) {
                LabeledField(
                    label = "Returned on",
                    value = returnedDateText,
                    onValueChange = { returnedDateText = it },
                    keyboardType = KeyboardType.Text,
                )
            }
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
                            initial?.loan?.id ?: 0L,
                            linkedGearItemId,
                            customItemLabel,
                            direction,
                            counterpartName,
                            status,
                            startDateText,
                            dueDateText,
                            returnedDateText,
                            notes,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canSave,
                ) {
                    Text(if (initial == null) "Save" else "Update")
                }
            }
        }
    }
}

@Composable
internal fun GearLoanCard(
    summary: GearLoanSummary,
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
                        text = summary.itemLabel,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${summary.loan.direction} · ${summary.loan.status}",
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
                loanCounterpartSummary(summary.loan.direction, summary.loan.counterpartName)
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
                if (summary.loan.startDateText.isNotBlank()) {
                    add("${loanStartedLabel(summary.loan.direction)} ${summary.loan.startDateText}")
                }
                if (summary.loan.dueDateText.isNotBlank()) {
                    add("Due ${summary.loan.dueDateText}")
                }
                if (summary.loan.returnedDateText.isNotBlank()) {
                    add("Returned ${summary.loan.returnedDateText}")
                }
            }.joinToString(" · ").ifBlank {
                when (summary.loan.status) {
                    "Returned" -> "Marked returned"
                    "Lost" -> "Marked lost"
                    else -> "No timing details saved yet"
                }
            }
            Text(
                text = detailLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                summary.reminder != null -> {
                    val reminderColor = when (summary.reminder.level) {
                        GearLoanReminderLevel.OVERDUE -> MaterialTheme.colorScheme.error
                        GearLoanReminderLevel.DUE_TODAY -> MaterialTheme.colorScheme.primary
                        GearLoanReminderLevel.UPCOMING -> MaterialTheme.colorScheme.tertiary
                    }
                    Text(
                        text = summary.reminder.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = reminderColor,
                    )
                }

                summary.loan.status == "Active" && summary.loan.dueDateText.isBlank() -> {
                    Text(
                        text = "No due date saved yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (summary.loan.notes.isNotBlank()) {
                Text(
                    text = summary.loan.notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
