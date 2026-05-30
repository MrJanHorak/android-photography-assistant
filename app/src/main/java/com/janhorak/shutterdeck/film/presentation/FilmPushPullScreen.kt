package com.janhorak.shutterdeck.film.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.FilmStockEntity
import com.janhorak.shutterdeck.film.domain.PushPullLatitudeStatus
import com.janhorak.shutterdeck.film.domain.buildPushPullNote
import com.janhorak.shutterdeck.film.domain.evaluatePushPull
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.SectionHeader

private data class PushPullContext(
    val sourceLabel: String,
    val stockDisplayName: String,
    val rollDisplayTitle: String?,
    val processingType: String,
    val baseIso: Int?,
    val defaultTargetEi: Int,
    val maxPushStops: Int?,
    val maxPullStops: Int?,
    val developerNotes: String,
    val availabilityMessage: String?,
    val cameraLabel: String?,
    val lensLabel: String?,
)

@Composable
fun FilmPushPullScreen(
    modifier: Modifier = Modifier,
    viewModel: FilmReferenceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    var selectedRollId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedStockId by rememberSaveable { mutableStateOf<String?>(null) }
    var targetEiText by rememberSaveable { mutableStateOf("") }
    var sessionNotes by rememberSaveable { mutableStateOf("") }
    var copyStatus by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedRollId, uiState.activeRolls) {
        if (selectedRollId != null && uiState.activeRolls.none { roll -> roll.id == selectedRollId }) {
            selectedRollId = null
        }
    }
    LaunchedEffect(selectedStockId, selectedRollId, uiState.stocks) {
        if (
            selectedRollId == null &&
            selectedStockId != null &&
            uiState.stocks.none { stock -> stock.id == selectedStockId }
        ) {
            selectedStockId = null
        }
    }

    val selectedRoll = remember(uiState.activeRolls, selectedRollId) {
        uiState.activeRolls.firstOrNull { roll -> roll.id == selectedRollId }
    }
    val selectedStock = remember(uiState.stocks, selectedStockId) {
        uiState.stocks.firstOrNull { stock -> stock.id == selectedStockId }
    }
    val context = remember(selectedRoll, selectedStock) {
        selectedRoll?.toContext() ?: selectedStock?.toContext()
    }
    val targetExposureIndex = targetEiText.toIntOrNull()?.takeIf { value -> value > 0 }
    val guidance = remember(context, targetExposureIndex) {
        val baseIso = context?.baseIso
        if (baseIso != null && targetExposureIndex != null) {
            evaluatePushPull(
                baseIso = baseIso,
                targetExposureIndex = targetExposureIndex,
                maxPushStops = context.maxPushStops,
                maxPullStops = context.maxPullStops,
            )
        } else {
            null
        }
    }
    val validationMessage = buildValidationMessage(
        context = context,
        targetEiText = targetEiText,
        targetExposureIndex = targetExposureIndex,
    )
    val noteBlock = remember(context, guidance, sessionNotes) {
        if (context != null && guidance != null) {
            buildPushPullNote(
                stockDisplayName = context.stockDisplayName,
                rollDisplayTitle = context.rollDisplayTitle,
                processingType = context.processingType,
                guidance = guidance,
                developerNotes = context.developerNotes,
                sessionNotes = sessionNotes,
            )
        } else {
            null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "Push / pull workflow",
                subtitle = "Compare box speed to your chosen EI, check the saved stock latitude, and generate notes you can keep with the roll.",
            )
        }
        item {
            RollSelectorCard(
                rolls = uiState.activeRolls,
                selectedRollId = selectedRollId,
                onSelectRoll = { rollId ->
                    selectedRollId = rollId
                    if (rollId == null) {
                        copyStatus = null
                    } else {
                        uiState.activeRolls.firstOrNull { roll -> roll.id == rollId }?.let { roll ->
                            targetEiText = roll.exposureIndex.toString()
                            copyStatus = null
                        }
                    }
                },
            )
        }
        if (selectedRollId == null) {
            item {
                StockSelectorCard(
                    stocks = uiState.stocks,
                    selectedStockId = selectedStockId,
                    onSelectStock = { stockId ->
                        selectedStockId = stockId
                        uiState.stocks.firstOrNull { stock -> stock.id == stockId }?.let { stock ->
                            targetEiText = stock.iso.toString()
                            copyStatus = null
                        }
                    },
                )
            }
        }
        item {
            PushPullGuidanceCard(
                context = context,
                targetEiText = targetEiText,
                onTargetEiChange = {
                    targetEiText = it
                    copyStatus = null
                },
                guidance = guidance,
                validationMessage = validationMessage,
            )
        }
        item {
            PushPullNotesCard(
                sessionNotes = sessionNotes,
                onSessionNotesChange = {
                    sessionNotes = it
                    copyStatus = null
                },
                noteBlock = noteBlock,
                copyStatus = copyStatus,
                onCopy = {
                    if (noteBlock != null) {
                        clipboardManager.setText(AnnotatedString(noteBlock))
                        copyStatus = "Copied note block."
                    }
                },
            )
        }
    }
}

@Composable
fun RollSelectorCard(
    rolls: List<FilmReferenceRollOption>,
    selectedRollId: Long?,
    onSelectRoll: (Long?) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Roll context",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (rolls.isEmpty()) {
                    "No active rolls are available yet. You can still plan directly from the stock library below."
                } else {
                    "Choose an active roll to compare its saved EI against the stock's box speed and latitude."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (rolls.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedRollId == null,
                        onClick = { onSelectRoll(null) },
                        label = { Text("No roll") },
                    )
                    rolls.forEach { roll ->
                        FilterChip(
                            selected = selectedRollId == roll.id,
                            onClick = { onSelectRoll(roll.id) },
                            label = { Text(roll.displayTitle) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StockSelectorCard(
    stocks: List<FilmStockEntity>,
    selectedStockId: String?,
    onSelectStock: (String?) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Stock context",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (stocks.isEmpty()) {
                    "No film stocks are saved yet."
                } else {
                    "If you are planning before loading a roll, choose a stock here and then set the EI you want to shoot."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (stocks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedStockId == null,
                        onClick = { onSelectStock(null) },
                        label = { Text("No stock") },
                    )
                    stocks.forEach { stock ->
                        FilterChip(
                            selected = selectedStockId == stock.id,
                            onClick = { onSelectStock(stock.id) },
                            label = { Text(stock.displayName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PushPullGuidanceCard(
    context: PushPullContext?,
    targetEiText: String,
    onTargetEiChange: (String) -> Unit,
    guidance: com.janhorak.shutterdeck.film.domain.PushPullGuidance?,
    validationMessage: String?,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Guidance",
                style = MaterialTheme.typography.titleMedium,
            )
            if (context == null) {
                Text(
                    text = "Select an active roll or a stock before calculating push/pull guidance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilmDetailLine(label = "Source", value = context.sourceLabel)
                    FilmDetailLine(label = "Stock", value = context.stockDisplayName)
                    FilmDetailLine(label = "Processing", value = context.processingType.ifBlank { "-" })
                    FilmDetailLine(label = "Box ISO", value = context.baseIso?.toString() ?: "Unavailable")
                    FilmDetailLine(
                        label = "Saved latitude",
                        value = formatLatitudeRange(context.maxPushStops, context.maxPullStops),
                    )
                    context.rollDisplayTitle?.let { title ->
                        FilmDetailLine(label = "Roll", value = title)
                    }
                    context.cameraLabel?.takeIf { value -> value.isNotBlank() }?.let { camera ->
                        FilmDetailLine(label = "Camera", value = camera)
                    }
                    context.lensLabel?.takeIf { value -> value.isNotBlank() }?.let { lens ->
                        FilmDetailLine(label = "Lens", value = lens)
                    }
                }
            }

            LabeledField(
                label = "Target EI",
                value = targetEiText,
                onValueChange = onTargetEiChange,
                suffix = "EI",
                keyboardType = KeyboardType.Number,
            )

            when {
                guidance != null -> {
                    FilmHighlightCard(
                        title = guidance.adjustmentLabel,
                        body = listOf(
                            guidance.exposureSummary,
                            guidance.processingSummary,
                            guidance.latitudeSummary,
                        ),
                    )
                    context?.availabilityMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    context?.developerNotes?.takeIf { notes -> notes.isNotBlank() }?.let { notes ->
                        FilmHighlightCard(
                            title = "Stock notes",
                            body = listOf(notes),
                        )
                    }
                    if (guidance.latitudeStatus == PushPullLatitudeStatus.OUTSIDE_SAVED_RANGE) {
                        Text(
                            text = "Consider testing this stock/developer combination before committing an important roll at that EI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                validationMessage != null -> {
                    Text(
                        text = validationMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PushPullNotesCard(
    sessionNotes: String,
    onSessionNotesChange: (String) -> Unit,
    noteBlock: String?,
    copyStatus: String?,
    onCopy: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Workflow note",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Add any one-off notes for this roll or stock, then copy the generated block into your roll notes, lab notebook, or export workflow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LabeledField(
                label = "Session notes",
                value = sessionNotes,
                onValueChange = onSessionNotesChange,
                singleLine = false,
                keyboardType = KeyboardType.Text,
            )
            if (noteBlock != null) {
                FilmHighlightCard(
                    title = "Generated note",
                    body = noteBlock.split('\n'),
                )
                OutlinedButton(onClick = onCopy) {
                    Text("Copy note")
                }
                copyStatus?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = "The generated note will appear after you select a stock or roll and enter a valid EI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun FilmHighlightCard(
    title: String,
    body: List<String>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            body.filter { line -> line.isNotBlank() }.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun FilmDetailLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private fun FilmReferenceRollOption.toContext(): PushPullContext {
    val availabilityMessage = when {
        !hasLiveStockDetails && stockId != null ->
            "Live stock latitude and developer notes are unavailable because this roll's stock is no longer in the library."

        !hasLiveStockDetails ->
            "This roll no longer links to a saved stock entry, so only its snapshotted ISO and EI can be compared."

        maxPushStops == null && maxPullStops == null ->
            "This stock does not have saved push/pull latitude yet."

        else -> null
    }
    return PushPullContext(
        sourceLabel = "Active roll",
        stockDisplayName = stockDisplayName,
        rollDisplayTitle = displayTitle,
        processingType = processingType,
        baseIso = baseIso,
        defaultTargetEi = exposureIndex,
        maxPushStops = maxPushStops,
        maxPullStops = maxPullStops,
        developerNotes = developerNotes,
        availabilityMessage = availabilityMessage,
        cameraLabel = cameraLabel,
        lensLabel = lensLabel,
    )
}

private fun FilmStockEntity.toContext(): PushPullContext = PushPullContext(
    sourceLabel = "Stock library",
    stockDisplayName = displayName,
    rollDisplayTitle = null,
    processingType = processingType,
    baseIso = iso,
    defaultTargetEi = iso,
    maxPushStops = maxPushStops,
    maxPullStops = maxPullStops,
    developerNotes = developerNotes,
    availabilityMessage = if (maxPushStops == null && maxPullStops == null) {
        "This stock does not have saved push/pull latitude yet."
    } else {
        null
    },
    cameraLabel = null,
    lensLabel = null,
)

private fun buildValidationMessage(
    context: PushPullContext?,
    targetEiText: String,
    targetExposureIndex: Int?,
): String? = when {
    context == null -> "Select an active roll or a stock before calculating guidance."
    context.baseIso == null -> "This context does not have a saved box ISO, so push/pull stops cannot be calculated."
    targetEiText.isBlank() -> "Enter the EI you want to shoot."
    targetExposureIndex == null -> "EI must be a whole number greater than 0."
    else -> null
}

private fun formatLatitudeRange(
    maxPushStops: Int?,
    maxPullStops: Int?,
): String = when {
    maxPushStops == null && maxPullStops == null -> "Unavailable"
    else -> buildList {
        maxPushStops?.let { pushStops ->
            add("+$pushStops")
        }
        maxPullStops?.let { pullStops ->
            add("-$pullStops")
        }
    }.joinToString(" / ").ifBlank { "Unavailable" }
}
