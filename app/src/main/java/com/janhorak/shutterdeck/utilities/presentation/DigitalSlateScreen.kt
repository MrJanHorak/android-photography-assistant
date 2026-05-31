package com.janhorak.shutterdeck.utilities.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.core.time.formatStructuredDateTime
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.effects.ImmersiveScreenMode
import com.janhorak.shutterdeck.ui.effects.LockLandscapeOrientation
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlinx.coroutines.delay

private const val SLATE_FLASH_DURATION_MILLIS = 260L

private data class SlateMarkSnapshot(
    val productionTitle: String,
    val scene: String,
    val shot: String,
    val take: Int,
    val timestampText: String,
)

@Composable
fun DigitalSlateScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    ImmersiveScreenMode(useDarkSystemBarIcons = false)
    LockLandscapeOrientation()

    val haptics = LocalHapticFeedback.current
    val slateDateText = remember { LocalDate.now().toString() }

    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var productionTitleText by rememberSaveable { mutableStateOf("") }
    var rollCardText by rememberSaveable { mutableStateOf("A001") }
    var sceneText by rememberSaveable { mutableStateOf("1") }
    var shotText by rememberSaveable { mutableStateOf("A") }
    var takeText by rememberSaveable { mutableStateOf("1") }
    var fpsText by rememberSaveable { mutableStateOf("23.976") }
    var directorText by rememberSaveable { mutableStateOf("") }
    var cameraText by rememberSaveable { mutableStateOf("") }
    var notesText by rememberSaveable { mutableStateOf("") }
    var lastMarkedLabel by rememberSaveable { mutableStateOf("") }
    var lastMarkedAtText by rememberSaveable { mutableStateOf("") }
    var markSnapshot by remember { mutableStateOf<SlateMarkSnapshot?>(null) }

    val takeValue = takeText.toIntOrNull()?.takeIf { it > 0 }
    val takeValidationMessage = when {
        takeText.isBlank() -> "Enter a take number before marking the slate."
        takeValue == null -> "Take must be a whole number greater than 0."
        else -> null
    }

    LaunchedEffect(markSnapshot) {
        val snapshot = markSnapshot ?: return@LaunchedEffect
        delay(SLATE_FLASH_DURATION_MILLIS)
        lastMarkedLabel = buildLastMarkedLabel(
            scene = snapshot.scene,
            shot = snapshot.shot,
            take = snapshot.take,
        )
        lastMarkedAtText = snapshot.timestampText
        takeText = (snapshot.take + 1).toString()
        markSnapshot = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                StatusPill(
                    text = buildStatusText(
                        currentTake = takeValue,
                        markSnapshot = markSnapshot,
                        lastMarkedLabel = lastMarkedLabel,
                        lastMarkedAtText = lastMarkedAtText,
                    ),
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(onClick = { controlsVisible = !controlsVisible }) {
                    Text(if (controlsVisible) "Hide details" else "Show details")
                }
            }

            SlateBoard(
                productionTitle = productionTitleText,
                rollCard = rollCardText,
                scene = sceneText,
                shot = shotText,
                take = takeValue,
                fps = fpsText,
                director = directorText,
                camera = cameraText,
                notes = notesText,
                dateText = slateDateText,
                lastMarkedAtText = lastMarkedAtText,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(controlsVisible) {
                        detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                    },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = {
                        val nextTake = (takeValue ?: 2) - 1
                        takeText = nextTake.coerceAtLeast(1).toString()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Take -")
                }
                Button(
                    onClick = {
                        if (markSnapshot != null || takeValue == null) return@Button
                        val snapshot = SlateMarkSnapshot(
                            productionTitle = productionTitleText.trim(),
                            scene = sceneText.trim(),
                            shot = shotText.trim(),
                            take = takeValue,
                            timestampText = formatStructuredDateTime(LocalDateTime.now()),
                        )
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        markSnapshot = snapshot
                    },
                    enabled = takeValidationMessage == null && markSnapshot == null,
                    modifier = Modifier.weight(1.35f),
                ) {
                    Text(if (markSnapshot == null) "Mark" else "Marking")
                }
                FilledTonalButton(
                    onClick = {
                        val nextTake = (takeValue ?: 0) + 1
                        takeText = nextTake.coerceAtLeast(1).toString()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Take +")
                }
            }

            if (takeValidationMessage != null) {
                Text(
                    text = takeValidationMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (controlsVisible) {
                SlateDetailsCard(
                    productionTitleText = productionTitleText,
                    onProductionTitleChange = { productionTitleText = it },
                    rollCardText = rollCardText,
                    onRollCardChange = { rollCardText = it },
                    sceneText = sceneText,
                    onSceneChange = { sceneText = it },
                    shotText = shotText,
                    onShotChange = { shotText = it },
                    takeText = takeText,
                    onTakeChange = { takeText = it },
                    fpsText = fpsText,
                    onFpsChange = { fpsText = it },
                    directorText = directorText,
                    onDirectorChange = { directorText = it },
                    cameraText = cameraText,
                    onCameraChange = { cameraText = it },
                    notesText = notesText,
                    onNotesChange = { notesText = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        markSnapshot?.let { snapshot ->
            SlateFlashOverlay(
                snapshot = snapshot,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SlateBoard(
    productionTitle: String,
    rollCard: String,
    scene: String,
    shot: String,
    take: Int?,
    fps: String,
    director: String,
    camera: String,
    notes: String,
    dateText: String,
    lastMarkedAtText: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)

    Surface(
        modifier = modifier
            .border(
                border = BorderStroke(2.dp, Color(0xFFF2F2F2)),
                shape = shape,
            ),
        shape = shape,
        color = Color(0xFF111111),
        contentColor = Color(0xFFF7F7F7),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SlateStripeBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "DIGITAL SLATE",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFC857),
                    )
                    Text(
                        text = displayValueOrFallback(productionTitle, "UNTITLED PRODUCTION"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SlateInfoCell(label = "ROLL / CARD", value = displayValueOrFallback(rollCard, "-"))
                    SlateInfoCell(label = "FPS", value = displayValueOrFallback(fps, "-"))
                    SlateInfoCell(label = "DATE", value = dateText)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SlateFieldCell(
                    label = "SCENE",
                    value = displayValueOrFallback(scene, "-"),
                    modifier = Modifier.weight(1.15f),
                )
                SlateFieldCell(
                    label = "SHOT",
                    value = displayValueOrFallback(shot, "-"),
                    modifier = Modifier.weight(1f),
                )
                SlateFieldCell(
                    label = "TAKE",
                    value = take?.toString() ?: "-",
                    modifier = Modifier.weight(0.9f),
                    emphasize = true,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SlateFieldCell(
                    label = "DIRECTOR",
                    value = displayValueOrFallback(director, "-"),
                    modifier = Modifier.weight(1f),
                    compact = true,
                )
                SlateFieldCell(
                    label = "CAMERA",
                    value = displayValueOrFallback(camera, "-"),
                    modifier = Modifier.weight(1f),
                    compact = true,
                )
            }

            SlateFieldCell(
                label = "NOTES",
                value = displayValueOrFallback(notes, "Tap Show details to add notes or client-facing labels."),
                modifier = Modifier.fillMaxWidth(),
                compact = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = if (lastMarkedAtText.isBlank()) {
                        "Ready to mark. Mark auto-advances to the next take."
                    } else {
                        "Last mark: $lastMarkedAtText"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD9D9D9),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Tap the slate to show or hide details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBDBDBD),
                )
            }
        }
    }
}

@Composable
private fun SlateStripeBar(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(color = Color(0xFF050505))

        val stripeWidth = size.height * 0.9f
        var startX = -size.height
        var brightStripe = true

        while (startX < size.width + size.height) {
            val path = Path().apply {
                moveTo(startX, size.height)
                lineTo(startX + stripeWidth, size.height)
                lineTo(startX + stripeWidth + size.height, 0f)
                lineTo(startX + size.height, 0f)
                close()
            }
            drawPath(
                path = path,
                color = if (brightStripe) Color(0xFFF5F5F5) else Color(0xFFFFC857),
            )
            startX += stripeWidth
            brightStripe = !brightStripe
        }
    }
}

@Composable
private fun SlateInfoCell(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFBDBDBD),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SlateFieldCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier.sizeIn(minHeight = if (compact) 80.dp else 112.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1D1D1D),
        border = BorderStroke(1.dp, Color(0xFF5A5A5A)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFBDBDBD),
            )
            Text(
                text = value,
                style = if (emphasize) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                fontWeight = if (emphasize) FontWeight.Black else FontWeight.Bold,
                fontFamily = if (emphasize) FontFamily.Monospace else FontFamily.Default,
                maxLines = if (compact) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SlateDetailsCard(
    productionTitleText: String,
    onProductionTitleChange: (String) -> Unit,
    rollCardText: String,
    onRollCardChange: (String) -> Unit,
    sceneText: String,
    onSceneChange: (String) -> Unit,
    shotText: String,
    onShotChange: (String) -> Unit,
    takeText: String,
    onTakeChange: (String) -> Unit,
    fpsText: String,
    onFpsChange: (String) -> Unit,
    directorText: String,
    onDirectorChange: (String) -> Unit,
    cameraText: String,
    onCameraChange: (String) -> Unit,
    notesText: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Slate details",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Keep Mark visible for the camera, and hide this panel once the slate is framed. Mark uses the visible scene / shot / take and then advances to the next take.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LabeledField(
                label = "Production / project title",
                value = productionTitleText,
                onValueChange = onProductionTitleChange,
                keyboardType = KeyboardType.Text,
            )
            LabeledField(
                label = "Roll / card / clip ID",
                value = rollCardText,
                onValueChange = onRollCardChange,
                keyboardType = KeyboardType.Text,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabeledField(
                    label = "Scene",
                    value = sceneText,
                    onValueChange = onSceneChange,
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.weight(1f),
                )
                LabeledField(
                    label = "Shot",
                    value = shotText,
                    onValueChange = onShotChange,
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabeledField(
                    label = "Take",
                    value = takeText,
                    onValueChange = onTakeChange,
                    modifier = Modifier.weight(1f),
                )
                LabeledField(
                    label = "Frame rate / timebase",
                    value = fpsText,
                    onValueChange = onFpsChange,
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabeledField(
                    label = "Director",
                    value = directorText,
                    onValueChange = onDirectorChange,
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.weight(1f),
                )
                LabeledField(
                    label = "Camera",
                    value = cameraText,
                    onValueChange = onCameraChange,
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = notesText,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
        }
    }
}

@Composable
private fun SlateFlashOverlay(
    snapshot: SlateMarkSnapshot,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        contentColor = Color.Black,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = displayValueOrFallback(snapshot.productionTitle, "DIGITAL SLATE"),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildSceneShotLine(snapshot.scene, snapshot.shot),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "TAKE ${snapshot.take}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            Text(
                text = snapshot.timestampText,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun buildStatusText(
    currentTake: Int?,
    markSnapshot: SlateMarkSnapshot?,
    lastMarkedLabel: String,
    lastMarkedAtText: String,
): String = when {
    markSnapshot != null -> "Marking ${buildLastMarkedLabel(markSnapshot.scene, markSnapshot.shot, markSnapshot.take)}"
    lastMarkedLabel.isNotBlank() && lastMarkedAtText.isNotBlank() -> "Last mark: $lastMarkedLabel at $lastMarkedAtText"
    currentTake != null -> "Ready for take $currentTake"
    else -> "Ready to mark"
}

private fun buildLastMarkedLabel(
    scene: String,
    shot: String,
    take: Int,
): String {
    val sceneDisplay = displayValueOrFallback(scene, "-")
    val shotDisplay = displayValueOrFallback(shot, "-")
    return "S$sceneDisplay / $shotDisplay / T$take"
}

private fun buildSceneShotLine(
    scene: String,
    shot: String,
): String = "SCENE ${displayValueOrFallback(scene, "-")} / SHOT ${displayValueOrFallback(shot, "-")}"

private fun displayValueOrFallback(
    value: String,
    fallback: String,
): String = value.trim().takeIf { it.isNotBlank() }?.uppercase(Locale.getDefault()) ?: fallback
