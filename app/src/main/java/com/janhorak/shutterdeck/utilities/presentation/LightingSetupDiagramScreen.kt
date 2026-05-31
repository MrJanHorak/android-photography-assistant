package com.janhorak.shutterdeck.utilities.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.core.time.formatStructuredDateTime
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.SectionHeader
import com.janhorak.shutterdeck.utilities.domain.LightingSetupDiagram
import com.janhorak.shutterdeck.utilities.domain.LightingSetupDraftItem
import com.janhorak.shutterdeck.utilities.domain.LightingSetupItemType
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LightingSetupDiagramScreen(
    modifier: Modifier = Modifier,
    viewModel: LightingSetupDiagramViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val savedSetups by viewModel.savedSetups.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    var shareInProgress by rememberSaveable { mutableStateOf(false) }

    val selectedItem = remember(editorState) {
        editorState.items.firstOrNull { item -> item.localId == editorState.selectedItemId }
    }

    fun shareDiagram(
        name: String,
        notes: String,
        items: List<LightingSetupDraftItem>,
        updatedAtMillis: Long,
    ) {
        if (shareInProgress) {
            return
        }
        shareInProgress = true
        viewModel.clearStatus()
        coroutineScope.launch {
            try {
                val payload = prepareLightingSetupDiagramShare(
                    context = context,
                    name = name,
                    notes = notes,
                    items = items.map(::normalizeLightingSetupShareItem),
                    updatedAtMillis = updatedAtMillis,
                )
                launchLightingSetupShareIntent(context, payload)
            } catch (_: IOException) {
                viewModel.setStatus("Unable to prepare the share image right now.")
            } catch (_: IllegalArgumentException) {
                viewModel.setStatus("Unable to share the diagram from this build.")
            } catch (_: ActivityNotFoundException) {
                viewModel.setStatus("No compatible share target is available on this device.")
            } finally {
                shareInProgress = false
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = "Lighting setup diagrammer",
                subtitle = "Place camera, subject, and lights on a simple stage. Save reusable diagrams and share a clean setup image.",
            )
        }
        item {
            LightingSetupEditorCard(
                state = editorState,
                selectedItem = selectedItem,
                status = status,
                shareInProgress = shareInProgress,
                onNameChange = viewModel::updateName,
                onNotesChange = viewModel::updateNotes,
                onSelectItem = viewModel::selectItem,
                onMoveItem = viewModel::moveItem,
                onSelectedLabelChange = viewModel::updateSelectedLabel,
                onSave = { viewModel.saveCurrentSetup() },
                onCreateNewDraft = viewModel::createNewDraft,
                onAddLight = viewModel::addLight,
                onRemoveSelectedLight = viewModel::removeSelectedLight,
                onNudge = viewModel::nudgeSelectedItem,
                onCenterSelected = viewModel::centerSelectedItem,
                onShare = {
                    shareDiagram(
                        name = editorState.name,
                        notes = editorState.notes,
                        items = editorState.items,
                        updatedAtMillis = System.currentTimeMillis(),
                    )
                },
            )
        }
        if (savedSetups.isEmpty()) {
            item {
                EmptyLightingSetupCard()
            }
        } else {
            item {
                SectionHeader(
                    title = "Saved setups",
                    subtitle = "Reload, update, delete, or share diagrams you want to reuse on future shoots.",
                )
            }
            items(savedSetups, key = { setup -> setup.id }) { setup ->
                SavedLightingSetupCard(
                    setup = setup,
                    isLoaded = setup.id == editorState.currentSetupId,
                    shareInProgress = shareInProgress,
                    onLoad = { viewModel.loadSetup(setup) },
                    onDelete = { viewModel.deleteSetup(setup) },
                    onShare = {
                        shareDiagram(
                            name = setup.name,
                            notes = setup.notes,
                            items = setup.items,
                            updatedAtMillis = setup.updatedAtMillis,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun LightingSetupEditorCard(
    state: LightingSetupEditorState,
    selectedItem: LightingSetupDraftItem?,
    status: String?,
    shareInProgress: Boolean,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSelectItem: (Long) -> Unit,
    onMoveItem: (Long, Float, Float) -> Unit,
    onSelectedLabelChange: (String) -> Unit,
    onSave: () -> Boolean,
    onCreateNewDraft: () -> Unit,
    onAddLight: () -> Unit,
    onRemoveSelectedLight: () -> Unit,
    onNudge: (Float, Float) -> Unit,
    onCenterSelected: () -> Unit,
    onShare: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (state.currentSetupId == null) "Current draft" else "Editing saved setup",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Drag markers on the stage to block the scene. Use the label field for renaming lights, then save or share the current layout.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LabeledField(
                label = "Setup name",
                value = state.name,
                onValueChange = onNameChange,
                keyboardType = KeyboardType.Text,
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            LightingSetupStageBoard(
                items = state.items,
                selectedItemId = state.selectedItemId,
                onSelectItem = onSelectItem,
                onMoveItem = onMoveItem,
            )
            SelectedLightingItemCard(
                selectedItem = selectedItem,
                onLabelChange = onSelectedLabelChange,
                onNudge = onNudge,
                onCenter = onCenterSelected,
                onRemove = onRemoveSelectedLight,
            )
            if (shareInProgress) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Preparing a share image...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (status != null) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { onSave() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.currentSetupId == null) "Save setup" else "Update setup")
                }
                OutlinedButton(
                    onClick = onCreateNewDraft,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("New draft")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = onAddLight,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Add light")
                }
                OutlinedButton(
                    onClick = onShare,
                    enabled = !shareInProgress,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Share image")
                }
            }
        }
    }
}

@Composable
private fun LightingSetupStageBoard(
    items: List<LightingSetupDraftItem>,
    selectedItemId: Long?,
    onSelectItem: (Long) -> Unit,
    onMoveItem: (Long, Float, Float) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Stage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Top = backdrop, bottom = camera side.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .background(
                        color = Color(0xFFF3F5F8),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(12.dp),
            ) {
                val boardWidth = maxWidth
                val boardHeight = maxHeight
                val markerWidth = 88.dp
                val markerHeight = 56.dp
                val draggableWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (boardWidth - markerWidth).toPx().coerceAtLeast(1f)
                }
                val draggableHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (boardHeight - markerHeight).toPx().coerceAtLeast(1f)
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineColor = Color(0xFFD0D7E2)
                    repeat(2) { index ->
                        val x = size.width * ((index + 1) / 3f)
                        val y = size.height * ((index + 1) / 3f)
                        drawLine(
                            color = lineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 2f,
                        )
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2f,
                        )
                    }
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 2f,
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 2f,
                    )
                }

                Text(
                    text = "Backdrop",
                    modifier = Modifier.align(Alignment.TopCenter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Camera side",
                    modifier = Modifier.align(Alignment.BottomCenter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                items.forEach { item ->
                    LightingSetupMarker(
                        item = item,
                        isSelected = item.localId == selectedItemId,
                        modifier = Modifier
                            .offset(x = (boardWidth - markerWidth) * item.xFraction, y = (boardHeight - markerHeight) * item.yFraction)
                            .pointerInput(item.localId, draggableWidthPx, draggableHeightPx) {
                                detectDragGestures(
                                    onDragStart = { onSelectItem(item.localId) },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onMoveItem(
                                            item.localId,
                                            dragAmount.x / draggableWidthPx,
                                            dragAmount.y / draggableHeightPx,
                                        )
                                    },
                                )
                            }
                            .clickable { onSelectItem(item.localId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LightingSetupMarker(
    item: LightingSetupDraftItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = lightingSetupMarkerColor(item),
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = if (isSelected) 6.dp else 2.dp,
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        modifier = modifier.size(width = 88.dp, height = 56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = item.label.ifBlank { item.type.defaultLabel },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun SelectedLightingItemCard(
    selectedItem: LightingSetupDraftItem?,
    onLabelChange: (String) -> Unit,
    onNudge: (Float, Float) -> Unit,
    onCenter: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Selected marker",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (selectedItem == null) {
                Text(
                    text = "Tap a marker on the stage to rename it or fine-tune its position.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = buildString {
                        append(
                            when (selectedItem.type) {
                                LightingSetupItemType.CAMERA -> "Camera"
                                LightingSetupItemType.SUBJECT -> "Subject"
                                LightingSetupItemType.LIGHT -> "Light"
                            },
                        )
                        append(" at ")
                        append((selectedItem.xFraction * 100f).roundToInt())
                        append("%, ")
                        append((selectedItem.yFraction * 100f).roundToInt())
                        append("%")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LabeledField(
                    label = "Marker label",
                    value = selectedItem.label,
                    onValueChange = onLabelChange,
                    keyboardType = KeyboardType.Text,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = { onNudge(0f, -0.03f) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Up")
                    }
                    OutlinedButton(
                        onClick = onCenter,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Center")
                    }
                    OutlinedButton(
                        onClick = { onRemove() },
                        enabled = selectedItem.type == LightingSetupItemType.LIGHT,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Remove")
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = { onNudge(-0.03f, 0f) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Left")
                    }
                    OutlinedButton(
                        onClick = { onNudge(0f, 0.03f) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Down")
                    }
                    OutlinedButton(
                        onClick = { onNudge(0.03f, 0f) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Right")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLightingSetupCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "No saved setups yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Use the editor above to block the scene, then save the layout so you can reload or share it later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SavedLightingSetupCard(
    setup: LightingSetupDiagram,
    isLoaded: Boolean,
    shareInProgress: Boolean,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = setup.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Updated ${formatLightingSetupUpdatedAt(setup.updatedAtMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isLoaded) {
                    Text(
                        text = "Loaded",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (setup.notes.isNotBlank()) {
                Text(
                    text = setup.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = setup.items.joinToString(separator = "  ") { item ->
                    item.label.ifBlank { item.type.defaultLabel }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onLoad,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isLoaded) "Reload" else "Load")
                }
                OutlinedButton(
                    onClick = onShare,
                    enabled = !shareInProgress,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Share")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

private fun lightingSetupMarkerColor(item: LightingSetupDraftItem): Color = when (item.type) {
    LightingSetupItemType.CAMERA -> Color(0xFF334155)
    LightingSetupItemType.SUBJECT -> Color(0xFF0F766E)
    LightingSetupItemType.LIGHT -> when (item.label.trim().lowercase()) {
        "key" -> Color(0xFFF59E0B)
        "fill" -> Color(0xFF3B82F6)
        "back" -> Color(0xFFA855F7)
        else -> Color(0xFFE11D48)
    }
}

private fun formatLightingSetupUpdatedAt(updatedAtMillis: Long): String =
    formatStructuredDateTime(
        Instant.ofEpochMilli(updatedAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime(),
    )

private fun normalizeLightingSetupShareItem(item: LightingSetupDraftItem): LightingSetupDraftItem =
    item.copy(label = item.label.trim().ifBlank { item.type.defaultLabel })

private fun launchLightingSetupShareIntent(
    context: Context,
    payload: LightingSetupSharePayload,
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, payload.imageUri)
        putExtra(Intent.EXTRA_TEXT, payload.summaryText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Share lighting setup")
    if (context !is android.app.Activity) {
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
