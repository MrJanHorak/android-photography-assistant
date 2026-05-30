package com.janhorak.shutterdeck.film.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.FilmRollEntity
import com.janhorak.shutterdeck.core.data.db.FilmStockEntity
import com.janhorak.shutterdeck.film.domain.FILM_ROLL_STATUS_ACTIVE
import com.janhorak.shutterdeck.film.domain.FILM_ROLL_STATUS_FINISHED
import com.janhorak.shutterdeck.film.domain.defaultFilmRollStartedOnText
import com.janhorak.shutterdeck.ui.components.DatePickerField
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.SectionHeader
import java.util.Locale

private enum class FilmRollFilter(val label: String) {
    ALL("All"),
    ACTIVE(FILM_ROLL_STATUS_ACTIVE),
    FINISHED(FILM_ROLL_STATUS_FINISHED),
}

private data class FilmRollLibrarySummary(
    val totalRolls: Int,
    val activeRolls: Int,
    val finishedRolls: Int,
    val loggedFrames: Int,
    val loggedExposures: Int,
)

@Composable
fun FilmRollsScreen(
    onOpenRoll: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilmRollsViewModel = hiltViewModel(),
) {
    val stocks by viewModel.stocks.collectAsStateWithLifecycle()
    val rollSummaries by viewModel.rollSummaries.collectAsStateWithLifecycle()
    val rollStatus by viewModel.rollStatus.collectAsStateWithLifecycle()

    var showingNewEditor by remember { mutableStateOf(false) }
    var editingRoll by remember { mutableStateOf<FilmRollEntity?>(null) }
    var newEditorSession by remember { mutableStateOf(0) }
    var filterName by rememberSaveable { mutableStateOf(FilmRollFilter.ALL.name) }

    val filter = remember(filterName) { FilmRollFilter.valueOf(filterName) }
    val summary = remember(rollSummaries) { buildFilmRollLibrarySummary(rollSummaries) }
    val filteredRollSummaries = remember(rollSummaries, filter) {
        rollSummaries.filter { rollSummary ->
            when (filter) {
                FilmRollFilter.ALL -> true
                FilmRollFilter.ACTIVE -> rollSummary.roll.status == FILM_ROLL_STATUS_ACTIVE
                FilmRollFilter.FINISHED -> rollSummary.roll.status == FILM_ROLL_STATUS_FINISHED
            }
        }
    }
    val showEditor = showingNewEditor || editingRoll != null
    val editorKey = remember(showingNewEditor, newEditorSession, editingRoll?.id) {
        editingRoll?.id?.let { id -> "edit_$id" } ?: "new_$newEditorSession"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "Roll logger",
                subtitle = "Start rolls from your stock library, keep per-frame shooting notes, and export a clean log when the roll is done.",
            )
        }
        item {
            FilmRollLibrarySummaryCard(summary = summary)
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionHeader(
                        title = "Manage your active and finished rolls",
                        subtitle = "Create a new roll here, then open its detail screen to log frames and export the final CSV record.",
                    )
                    FilmRollFilterChipRow(
                        label = "Show",
                        options = FilmRollFilter.entries.map { option -> option.label },
                        selectedOption = filter.label,
                        onSelected = { selected ->
                            filterName = FilmRollFilter.entries.first { option -> option.label == selected }.name
                        },
                    )
                    OutlinedButton(
                        onClick = {
                            if (showEditor && editingRoll == null) {
                                showingNewEditor = false
                            } else {
                                editingRoll = null
                                newEditorSession += 1
                                showingNewEditor = true
                            }
                            viewModel.clearStatus()
                        },
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Text(
                            text = if (showEditor && editingRoll == null) "Hide roll editor" else "Start new roll",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    rollStatus?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (showEditor) {
            item {
                FilmRollEditorCard(
                    editorKey = editorKey,
                    stocks = stocks,
                    initial = editingRoll,
                    onCancel = {
                        showingNewEditor = false
                        editingRoll = null
                    },
                    onSave = { initial, selectedStockId, title, exposureIndex, totalFrames, cameraLabel, lensLabel, startedOnText, notes ->
                        val saveAccepted = viewModel.saveRoll(
                            existing = initial,
                            selectedStockId = selectedStockId,
                            title = title,
                            exposureIndex = exposureIndex,
                            totalFrames = totalFrames,
                            cameraLabel = cameraLabel,
                            lensLabel = lensLabel,
                            startedOnText = startedOnText,
                            notes = notes,
                        )
                        if (saveAccepted) {
                            showingNewEditor = false
                            editingRoll = null
                        }
                    },
                )
            }
        }
        if (filteredRollSummaries.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "No rolls match the current filter.",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Start a new roll above, or switch the filter to review finished logs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        items(filteredRollSummaries, key = { rollSummary -> rollSummary.roll.id }) { rollSummary ->
            FilmRollCard(
                rollSummary = rollSummary,
                onOpen = { onOpenRoll(rollSummary.roll.id) },
                onEdit = {
                    showingNewEditor = false
                    editingRoll = rollSummary.roll
                    viewModel.clearStatus()
                },
                onDelete = {
                    if (editingRoll?.id == rollSummary.roll.id) {
                        editingRoll = null
                        showingNewEditor = false
                    }
                    viewModel.deleteRoll(rollSummary.roll)
                },
            )
        }
    }
}

@Composable
private fun FilmRollLibrarySummaryCard(summary: FilmRollLibrarySummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(
                title = "Roll summary",
                subtitle = "Frame count tracks unique frame numbers. Exposure count includes bracketed and double-exposed entries.",
            )
            FilmRollSummaryLine("Rolls", summary.totalRolls.toString())
            FilmRollSummaryLine("Active", summary.activeRolls.toString())
            FilmRollSummaryLine("Finished", summary.finishedRolls.toString())
            FilmRollSummaryLine("Frames logged", summary.loggedFrames.toString())
            FilmRollSummaryLine("Exposures logged", summary.loggedExposures.toString())
        }
    }
}

@Composable
private fun FilmRollSummaryLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FilmRollEditorCard(
    editorKey: String,
    stocks: List<FilmStockEntity>,
    initial: FilmRollEntity?,
    onSave: (
        FilmRollEntity?,
        String?,
        String,
        Int?,
        Int?,
        String,
        String,
        String,
        String,
    ) -> Unit,
    onCancel: () -> Unit,
) {
    val sortedStocks = remember(stocks) {
        stocks.sortedBy { stock -> stock.displayName.lowercase(Locale.ROOT) }
    }
    var selectedStockId by remember(editorKey) { mutableStateOf(initial?.stockId) }
    var title by remember(editorKey) { mutableStateOf(initial?.title.orEmpty()) }
    var exposureIndexText by remember(editorKey) { mutableStateOf(initial?.exposureIndex?.toString().orEmpty()) }
    var totalFramesText by remember(editorKey) { mutableStateOf(initial?.totalFrames?.toString().orEmpty()) }
    var cameraLabel by remember(editorKey) { mutableStateOf(initial?.cameraLabel.orEmpty()) }
    var lensLabel by remember(editorKey) { mutableStateOf(initial?.lensLabel.orEmpty()) }
    var startedOnText by remember(editorKey) {
        mutableStateOf(initial?.startedOnText ?: defaultFilmRollStartedOnText())
    }
    var notes by remember(editorKey) { mutableStateOf(initial?.notes.orEmpty()) }

    val parsedExposureIndex = exposureIndexText.toIntOrNull()
    val parsedTotalFrames = totalFramesText.takeIf { text -> text.isNotBlank() }?.toIntOrNull()
    val saveEnabled = selectedStockId != null &&
        parsedExposureIndex != null &&
        parsedExposureIndex > 0 &&
        (totalFramesText.isBlank() || (parsedTotalFrames != null && parsedTotalFrames > 0)) &&
        cameraLabel.isNotBlank() &&
        lensLabel.isNotBlank() &&
        startedOnText.isNotBlank()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = if (initial == null) "Start a new roll" else "Edit roll",
                subtitle = "Choose the film stock first. Roll capacity is optional for sheet or instant film, but useful for 35mm and 120 progress tracking.",
            )
            if (initial?.stockId == null && initial != null) {
                Text(
                    text = "This roll's original stock is no longer linked. Choose a current stock to save changes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Film stock",
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sortedStocks.forEach { stock ->
                        FilterChip(
                            selected = selectedStockId == stock.id,
                            onClick = { selectedStockId = stock.id },
                            label = { Text(stock.displayName) },
                            modifier = Modifier.widthIn(min = 90.dp),
                        )
                    }
                }
            }
            LabeledField(
                label = "Roll title (optional)",
                value = title,
                onValueChange = { title = it },
                keyboardType = KeyboardType.Text,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Exposure index",
                    value = exposureIndexText,
                    onValueChange = { exposureIndexText = it },
                    modifier = Modifier.weight(1f),
                )
                LabeledField(
                    label = "Roll capacity",
                    value = totalFramesText,
                    onValueChange = { totalFramesText = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(
                    label = "Camera",
                    value = cameraLabel,
                    onValueChange = { cameraLabel = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
                LabeledField(
                    label = "Lens",
                    value = lensLabel,
                    onValueChange = { lensLabel = it },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Text,
                )
            }
            DatePickerField(
                label = "Started on",
                value = startedOnText,
                onValueChange = { startedOnText = it },
            )
            LabeledField(
                label = "Roll notes",
                value = notes,
                onValueChange = { notes = it },
                keyboardType = KeyboardType.Text,
                singleLine = false,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            initial,
                            selectedStockId,
                            title,
                            parsedExposureIndex,
                            parsedTotalFrames,
                            cameraLabel,
                            lensLabel,
                            startedOnText,
                            notes,
                        )
                    },
                    enabled = saveEnabled,
                ) {
                    Text(if (initial == null) "Start roll" else "Save changes")
                }
            }
        }
    }
}

@Composable
private fun FilmRollCard(
    rollSummary: FilmRollSummary,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = rollSummary.roll.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${rollSummary.roll.stockDisplayName} · EI ${rollSummary.roll.exposureIndex}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${rollSummary.roll.cameraLabel} · ${rollSummary.roll.lensLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = rollSummary.roll.status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = buildFilmRollProgressText(rollSummary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Started ${rollSummary.roll.startedOnText}" +
                    rollSummary.roll.finishedOnText.takeIf(String::isNotBlank)?.let { finished -> " · Finished $finished" }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (rollSummary.roll.notes.isNotBlank()) {
                Text(
                    text = rollSummary.roll.notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onOpen) {
                    Text("Open log")
                }
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit roll")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete roll")
                }
            }
        }
    }
}

@Composable
private fun FilmRollFilterChipRow(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selectedOption,
                    onClick = { onSelected(option) },
                    label = { Text(option) },
                )
            }
        }
    }
}

private fun buildFilmRollLibrarySummary(rollSummaries: List<FilmRollSummary>): FilmRollLibrarySummary {
    return FilmRollLibrarySummary(
        totalRolls = rollSummaries.size,
        activeRolls = rollSummaries.count { rollSummary -> rollSummary.roll.status == FILM_ROLL_STATUS_ACTIVE },
        finishedRolls = rollSummaries.count { rollSummary -> rollSummary.roll.status == FILM_ROLL_STATUS_FINISHED },
        loggedFrames = rollSummaries.sumOf { rollSummary -> rollSummary.loggedFrameCount },
        loggedExposures = rollSummaries.sumOf { rollSummary -> rollSummary.loggedExposureCount },
    )
}

private fun buildFilmRollProgressText(rollSummary: FilmRollSummary): String {
    val frameText = rollSummary.roll.totalFrames?.let { capacity ->
        "${rollSummary.loggedFrameCount} / $capacity frames"
    } ?: "${rollSummary.loggedFrameCount} frames"
    val exposureSuffix = if (rollSummary.loggedExposureCount > rollSummary.loggedFrameCount) {
        " · ${rollSummary.loggedExposureCount} exposures"
    } else {
        ""
    }
    val capacitySuffix = if (rollSummary.atCapacity) " · capacity reached" else ""
    return "$frameText$exposureSuffix$capacitySuffix"
}
