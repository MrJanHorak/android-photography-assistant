package com.janhorak.shutterdeck.film.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.data.db.FilmStockEntity
import com.janhorak.shutterdeck.film.domain.ReciprocityGuidance
import com.janhorak.shutterdeck.film.domain.ReciprocityStatus
import com.janhorak.shutterdeck.film.domain.buildReciprocityNote
import com.janhorak.shutterdeck.film.domain.evaluateFilmReciprocity
import com.janhorak.shutterdeck.metering.domain.formatExposureTime
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.SectionHeader

private data class ReciprocityContext(
    val sourceLabel: String,
    val stockDisplayName: String,
    val rollDisplayTitle: String?,
    val processingType: String,
    val baseIso: Int?,
    val exposureIndex: Int?,
    val reciprocityExponent: Double?,
    val reciprocityStartsAtSeconds: Double?,
    val developerNotes: String,
    val availabilityMessage: String?,
    val cameraLabel: String?,
    val lensLabel: String?,
)

@Composable
fun FilmReciprocityScreen(
    modifier: Modifier = Modifier,
    viewModel: FilmReferenceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    var selectedRollId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedStockId by rememberSaveable { mutableStateOf<String?>(null) }
    var meteredTimeText by rememberSaveable { mutableStateOf("") }
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
        selectedRoll?.toReciprocityContext() ?: selectedStock?.toReciprocityContext()
    }
    val meteredSeconds = meteredTimeText.toDoubleOrNull()?.takeIf { value -> value > 0.0 }
    val guidance = remember(context, meteredSeconds) {
        if (meteredSeconds != null && context != null) {
            evaluateFilmReciprocity(
                meteredSeconds = meteredSeconds,
                exponent = context.reciprocityExponent,
                onsetSeconds = context.reciprocityStartsAtSeconds,
            )
        } else {
            null
        }
    }
    val validationMessage = buildReciprocityValidationMessage(
        context = context,
        meteredTimeText = meteredTimeText,
        meteredSeconds = meteredSeconds,
    )
    val noteBlock = remember(context, guidance, sessionNotes) {
        if (context != null && guidance != null) {
            buildReciprocityNote(
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
                title = "Reciprocity assistant",
                subtitle = "Use saved stock or active-roll reciprocity data to correct long exposures before you trip the shutter.",
            )
        }
        item {
            RollSelectorCard(
                rolls = uiState.activeRolls,
                selectedRollId = selectedRollId,
                onSelectRoll = { rollId ->
                    selectedRollId = rollId
                    copyStatus = null
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
                        copyStatus = null
                    },
                )
            }
        }
        item {
            ReciprocityGuidanceCard(
                context = context,
                meteredTimeText = meteredTimeText,
                onMeteredTimeChange = {
                    meteredTimeText = it
                    copyStatus = null
                },
                guidance = guidance,
                validationMessage = validationMessage,
            )
        }
        item {
            ReciprocityNotesCard(
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
private fun ReciprocityGuidanceCard(
    context: ReciprocityContext?,
    meteredTimeText: String,
    onMeteredTimeChange: (String) -> Unit,
    guidance: ReciprocityGuidance?,
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
                    text = "Select an active roll or a stock before calculating reciprocity correction.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilmDetailLine(label = "Source", value = context.sourceLabel)
                    FilmDetailLine(label = "Stock", value = context.stockDisplayName)
                    FilmDetailLine(label = "Processing", value = context.processingType.ifBlank { "-" })
                    FilmDetailLine(label = "Box ISO", value = context.baseIso?.toString() ?: "Unavailable")
                    FilmDetailLine(label = "EI", value = context.exposureIndex?.toString() ?: "—")
                    FilmDetailLine(
                        label = "Onset",
                        value = context.reciprocityStartsAtSeconds?.let(::formatExposureTime) ?: "Unavailable",
                    )
                    FilmDetailLine(
                        label = "Exponent",
                        value = context.reciprocityExponent?.toString() ?: "Unavailable",
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
                label = "Metered exposure",
                value = meteredTimeText,
                onValueChange = onMeteredTimeChange,
                suffix = "sec",
                keyboardType = KeyboardType.Decimal,
            )

            when {
                guidance != null -> {
                    FilmHighlightCard(
                        title = when (guidance.status) {
                            ReciprocityStatus.NO_CURVE_SAVED -> "No saved curve"
                            ReciprocityStatus.BELOW_ONSET -> "No correction needed"
                            ReciprocityStatus.CORRECTED -> "Corrected exposure"
                        },
                        body = buildList {
                            add(guidance.timingSummary)
                            add(guidance.curveSummary)
                            if (guidance.status == ReciprocityStatus.CORRECTED) {
                                add("Added time: ${formatExposureTime(guidance.addedSeconds)}")
                            }
                        },
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
private fun ReciprocityNotesCard(
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
                text = "Exposure note",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Capture any one-off bracket, safety, or lab notes for this long exposure and copy the final block into your roll notes or shot log.",
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
                    text = "The generated note will appear after you select a stock or roll and enter a valid metered time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun FilmReferenceRollOption.toReciprocityContext(): ReciprocityContext {
    val availabilityMessage = when {
        stockReciprocityExponent == null || stockReciprocityStartsAtSeconds == null ->
            "This roll does not have a saved reciprocity curve yet, so the tool can only echo the metered time."

        !hasLiveStockDetails && stockId != null ->
            "The linked stock is no longer in the library, so live stock notes are unavailable. Reciprocity still uses the roll's saved curve snapshot."

        !hasLiveStockDetails ->
            "This roll no longer links to a saved stock entry, but its snapshotted reciprocity curve is still available."

        else -> null
    }
    return ReciprocityContext(
        sourceLabel = "Active roll",
        stockDisplayName = stockDisplayName,
        rollDisplayTitle = displayTitle,
        processingType = processingType,
        baseIso = baseIso,
        exposureIndex = exposureIndex,
        reciprocityExponent = stockReciprocityExponent,
        reciprocityStartsAtSeconds = stockReciprocityStartsAtSeconds,
        developerNotes = developerNotes,
        availabilityMessage = availabilityMessage,
        cameraLabel = cameraLabel,
        lensLabel = lensLabel,
    )
}

private fun FilmStockEntity.toReciprocityContext(): ReciprocityContext = ReciprocityContext(
    sourceLabel = "Stock library",
    stockDisplayName = displayName,
    rollDisplayTitle = null,
    processingType = processingType,
    baseIso = iso,
    exposureIndex = iso,
    reciprocityExponent = reciprocityExponent,
    reciprocityStartsAtSeconds = reciprocityStartsAtSeconds,
    developerNotes = developerNotes,
    availabilityMessage = if (reciprocityExponent == null || reciprocityStartsAtSeconds == null) {
        "This stock does not have a saved reciprocity curve yet."
    } else {
        null
    },
    cameraLabel = null,
    lensLabel = null,
)

private fun buildReciprocityValidationMessage(
    context: ReciprocityContext?,
    meteredTimeText: String,
    meteredSeconds: Double?,
): String? = when {
    context == null -> "Select an active roll or a stock before calculating reciprocity correction."
    meteredTimeText.isBlank() -> "Enter the metered long-exposure time in seconds."
    meteredSeconds == null -> "Metered time must be greater than 0."
    else -> null
}
