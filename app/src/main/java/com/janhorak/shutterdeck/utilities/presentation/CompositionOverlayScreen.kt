package com.janhorak.shutterdeck.utilities.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.janhorak.shutterdeck.ui.camera.BackCameraPreview
import com.janhorak.shutterdeck.ui.camera.isCameraPermissionGranted
import com.janhorak.shutterdeck.ui.effects.ImmersiveScreenMode
import com.janhorak.shutterdeck.utilities.domain.CompositionOverlayMode
import com.janhorak.shutterdeck.utilities.domain.compositionOverlayGuides

@Composable
fun CompositionOverlayScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraAvailable = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(isCameraPermissionGranted(context))
    }
    var selectedOverlayName by rememberSaveable {
        mutableStateOf(CompositionOverlayMode.RULE_OF_THIRDS.name)
    }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    val selectedOverlay = remember(selectedOverlayName) {
        CompositionOverlayMode.valueOf(selectedOverlayName)
    }
    val guideLines = remember(selectedOverlay) {
        compositionOverlayGuides(selectedOverlay)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted },
    )

    ImmersiveScreenMode(useDarkSystemBarIcons = false)

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = isCameraPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val previewReady = cameraAvailable && hasCameraPermission
    val showControls = !previewReady || controlsVisible

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (previewReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(controlsVisible) {
                        detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                    },
            ) {
                BackCameraPreview(modifier = Modifier.fillMaxSize())
                CompositionOverlayCanvas(
                    guideLines = guideLines,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }

                if (!previewReady) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CameraAccessCard(
                            cameraAvailable = cameraAvailable,
                            onRequestPermission = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                        )
                    }
                } else {
                    OverlayControlsCard(
                        selectedOverlay = selectedOverlay,
                        onOverlaySelected = { selectedOverlayName = it.name },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompositionOverlayCanvas(
    guideLines: List<com.janhorak.shutterdeck.utilities.domain.CompositionGuideLine>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val outlineStroke = 4.dp.toPx()
        val guideStroke = 1.8.dp.toPx()
        val outlineColor = Color.Black.copy(alpha = 0.42f)
        val guideColor = Color.White.copy(alpha = 0.88f)

        guideLines.forEach { line ->
            val start = Offset(
                x = line.startX * size.width,
                y = line.startY * size.height,
            )
            val end = Offset(
                x = line.endX * size.width,
                y = line.endY * size.height,
            )

            drawLine(
                color = outlineColor,
                start = start,
                end = end,
                strokeWidth = outlineStroke,
            )
            drawLine(
                color = guideColor,
                start = start,
                end = end,
                strokeWidth = guideStroke,
            )
        }
    }
}

@Composable
private fun OverlayControlsCard(
    selectedOverlay: CompositionOverlayMode,
    onOverlaySelected: (CompositionOverlayMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Composition overlays",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = selectedOverlay.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompositionOverlayMode.entries.forEach { overlayMode ->
                    FilterChip(
                        selected = overlayMode == selectedOverlay,
                        onClick = { onOverlaySelected(overlayMode) },
                        label = { Text(overlayMode.label) },
                    )
                }
            }
            Text(
                text = "Tap the preview to hide or show controls. The overlay stays aligned to the live cropped preview that you see on screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CameraAccessCard(
    cameraAvailable: Boolean,
    onRequestPermission: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (cameraAvailable) "Allow camera access" else "Camera unavailable",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (cameraAvailable) {
                    "Use the back camera with rule-of-thirds, golden-ratio, or corner-to-corner diagonal guides. Camera access is required first."
                } else {
                    "This device does not expose a usable back camera for the composition overlay tool."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (cameraAvailable) {
                FilledTonalButton(onClick = onRequestPermission) {
                    Text("Allow camera access")
                }
            }
        }
    }
}
