package com.janhorak.shutterdeck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.core.time.formatStructuredDate
import com.janhorak.shutterdeck.core.time.formatStructuredDateTime
import com.janhorak.shutterdeck.core.time.formatStructuredTime
import com.janhorak.shutterdeck.core.time.parseStructuredDateOrNull
import com.janhorak.shutterdeck.core.time.parseStructuredDateTimeOrNull
import com.janhorak.shutterdeck.core.time.parseStructuredTimeOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true,
) {
    val parsedDate = remember(value) { parseStructuredDateOrNull(value) }
    var showPicker by remember { mutableStateOf(false) }

    PickerButtonField(
        label = label,
        value = value,
        placeholder = "Pick a date",
        formatHint = "YYYY-MM-DD",
        isLegacyValue = value.isNotBlank() && parsedDate == null,
        modifier = modifier,
        allowClear = allowClear,
        onClick = { showPicker = true },
        onClear = { onValueChange("") },
    )

    if (showPicker) {
        DatePickerModal(
            initialDate = parsedDate ?: LocalDate.now(),
            onDismiss = { showPicker = false },
            onConfirm = { selectedDate ->
                onValueChange(formatStructuredDate(selectedDate))
                showPicker = false
            },
        )
    }
}

@Composable
fun TimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true,
) {
    val parsedTime = remember(value) { parseStructuredTimeOrNull(value) }
    var showPicker by remember { mutableStateOf(false) }

    PickerButtonField(
        label = label,
        value = value,
        placeholder = "Pick a time",
        formatHint = "HH:mm",
        isLegacyValue = value.isNotBlank() && parsedTime == null,
        modifier = modifier,
        allowClear = allowClear,
        onClick = { showPicker = true },
        onClear = { onValueChange("") },
    )

    if (showPicker) {
        TimePickerModal(
            initialTime = parsedTime ?: LocalTime.now().withSecond(0).withNano(0),
            onDismiss = { showPicker = false },
            onConfirm = { selectedTime ->
                onValueChange(formatStructuredTime(selectedTime))
                showPicker = false
            },
        )
    }
}

@Composable
fun DateTimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true,
) {
    val parsedDateTime = remember(value) { parseStructuredDateTimeOrNull(value) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    PickerButtonField(
        label = label,
        value = value,
        placeholder = "Pick a date & time",
        formatHint = "YYYY-MM-DD HH:mm",
        isLegacyValue = value.isNotBlank() && parsedDateTime == null,
        modifier = modifier,
        allowClear = allowClear,
        onClick = {
            pendingDate = null
            showDatePicker = true
        },
        onClear = { onValueChange("") },
    )

    if (showDatePicker) {
        DatePickerModal(
            initialDate = parsedDateTime?.toLocalDate() ?: LocalDate.now(),
            onDismiss = {
                pendingDate = null
                showDatePicker = false
            },
            onConfirm = { selectedDate ->
                pendingDate = selectedDate
                showDatePicker = false
                showTimePicker = true
            },
        )
    }

    if (showTimePicker) {
        TimePickerModal(
            initialTime = parsedDateTime?.toLocalTime()?.withSecond(0)?.withNano(0)
                ?: LocalTime.now().withSecond(0).withNano(0),
            onDismiss = {
                pendingDate = null
                showTimePicker = false
            },
            onConfirm = { selectedTime ->
                val selectedDate = pendingDate ?: parsedDateTime?.toLocalDate() ?: LocalDate.now()
                onValueChange(formatStructuredDateTime(LocalDateTime.of(selectedDate, selectedTime)))
                pendingDate = null
                showTimePicker = false
            },
        )
    }
}

@Composable
private fun PickerButtonField(
    label: String,
    value: String,
    placeholder: String,
    formatHint: String,
    isLegacyValue: Boolean,
    modifier: Modifier = Modifier,
    allowClear: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = value.ifBlank { placeholder },
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current,
            )
            Text(if (value.isBlank()) "Pick" else "Change")
        }
        if (isLegacyValue) {
            Text(
                text = "Saved value is not in $formatHint yet. Picking a new value will replace it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (allowClear && value.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toPickerDateMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { selectedDateMillis ->
                        onConfirm(selectedDateMillis.toPickerLocalDate())
                    }
                },
                enabled = pickerState.selectedDateMillis != null,
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(
            state = pickerState,
            showModeToggle = false,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerModal(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose time") },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime.of(pickerState.hour, pickerState.minute))
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun LocalDate.toPickerDateMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toPickerLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}
