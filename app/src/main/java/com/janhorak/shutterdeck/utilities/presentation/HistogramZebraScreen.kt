package com.janhorak.shutterdeck.utilities.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.janhorak.shutterdeck.ui.camera.BackCameraPreview
import com.janhorak.shutterdeck.ui.camera.isCameraPermissionGranted
import com.janhorak.shutterdeck.ui.effects.ImmersiveScreenMode
import com.janhorak.shutterdeck.utilities.data.LiveLuminanceAnalyzer
import com.janhorak.shutterdeck.utilities.data.LiveLuminanceAnalyzerConfig
import com.janhorak.shutterdeck.utilities.domain.HistogramZebraAnalysis
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun HistogramZebraScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val cameraAvailable = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(isCameraPermissionGranted(context))
    }
    var showHistogram by rememberSaveable { mutableStateOf(true) }
    var showZebra by rememberSaveable { mutableStateOf(true) }
    var zebraThresholdPercent by rememberSaveable { mutableStateOf(98) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var lastAnalysis by remember { mutableStateOf<HistogramZebraAnalysis?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val analyzer = remember(mainExecutor) {
        LiveLuminanceAnalyzer(
            callbackExecutor = mainExecutor,
            onAnalysis = { analysis ->
                cameraError = null
                lastAnalysis = analysis
            },
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted },
    )

    SideEffect {
        analyzer.updateConfig(
            LiveLuminanceAnalyzerConfig(
                enabled = showHistogram || showZebra,
                histogramBinCount = 32,
                zebraThreshold = (zebraThresholdPercent / 100f * 255f).roundToInt().coerceIn(0, 255),
                zebraColumns = 20,
                zebraActivationFraction = 0.35f,
                minAnalysisIntervalMillis = 100L,
            ),
        )
    }

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

    val previewReady = cameraAvailable && hasCameraPermission && cameraError == null
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
                BackCameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    imageAnalyzer = analyzer,
                    analysisTargetResolution = Size(640, 480),
                    onBindingError = { cameraError = "Unable to start the live camera preview." },
                )
                if (showZebra) {
                    HistogramZebraOverlay(
                        analysis = lastAnalysis,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
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
                        HistogramCameraAccessCard(
                            cameraAvailable = cameraAvailable,
                            cameraError = cameraError,
                            onRequestPermission = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                        )
                    }
                } else {
                    HistogramControlsCard(
                        analysis = lastAnalysis,
                        showHistogram = showHistogram,
                        onShowHistogramChange = { showHistogram = it },
                        showZebra = showZebra,
                        onShowZebraChange = { showZebra = it },
                        zebraThresholdPercent = zebraThresholdPercent,
                        onZebraThresholdPercentChange = { zebraThresholdPercent = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistogramZebraOverlay(
    analysis: HistogramZebraAnalysis?,
    modifier: Modifier = Modifier,
) {
    if (analysis == null) return

    Canvas(modifier = modifier) {
        val cellWidth = size.width / analysis.zebraColumns.toFloat()
        val cellHeight = size.height / analysis.zebraRows.toFloat()
        val stripeSpacing = max(cellWidth, cellHeight) / 3f
        val stripeWidth = 2.dp.toPx()

        analysis.activeZebraCells.forEach { cell ->
            val left = cell.column * cellWidth
            val top = cell.row * cellHeight
            val right = left + cellWidth
            val bottom = top + cellHeight
            val fillAlpha = 0.10f + (cell.highlightedFraction - 0.35f).coerceAtLeast(0f) * 0.45f

            drawRect(
                color = Color.White.copy(alpha = fillAlpha.coerceAtMost(0.35f)),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight),
            )

            clipRect(left = left, top = top, right = right, bottom = bottom) {
                var x = left - cellHeight
                while (x < right + cellHeight) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.55f),
                        start = Offset(x, bottom),
                        end = Offset(x + cellHeight, top),
                        strokeWidth = stripeWidth,
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.82f),
                        start = Offset(x + stripeWidth, bottom),
                        end = Offset(x + cellHeight + stripeWidth, top),
                        strokeWidth = stripeWidth,
                    )
                    x += stripeSpacing
                }
            }
        }
    }
}

@Composable
private fun HistogramControlsCard(
    analysis: HistogramZebraAnalysis?,
    showHistogram: Boolean,
    onShowHistogramChange: (Boolean) -> Unit,
    showZebra: Boolean,
    onShowZebraChange: (Boolean) -> Unit,
    zebraThresholdPercent: Int,
    onZebraThresholdPercentChange: (Int) -> Unit,
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
                text = "Live Histogram & Zebra",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Monitor the live luminance distribution and highlight clipping from the same shared back-camera preview path used by the other on-shoot tools.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HistogramToggle(
                    label = "Histogram",
                    checked = showHistogram,
                    onCheckedChange = onShowHistogramChange,
                )
                HistogramToggle(
                    label = "Zebra",
                    checked = showZebra,
                    onCheckedChange = onShowZebraChange,
                )
            }
            Text(
                text = "Zebra threshold",
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(95, 98, 100).forEach { percent ->
                    FilterChip(
                        selected = zebraThresholdPercent == percent,
                        onClick = { onZebraThresholdPercentChange(percent) },
                        label = { Text("$percent%") },
                    )
                }
            }
            if (analysis == null) {
                Text(
                    text = if (showHistogram || showZebra) {
                        "Waiting for live preview analysis..."
                    } else {
                        "Histogram and zebra are both paused."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HistogramMetric(
                        label = "Average",
                        value = "${(analysis.averageLuminanceRatio * 100f).roundToInt()}%",
                        modifier = Modifier.weight(1f),
                    )
                    HistogramMetric(
                        label = "Highlights",
                        value = "${(analysis.highlightRatio * 100f).roundToInt()}%",
                        modifier = Modifier.weight(1f),
                    )
                    HistogramMetric(
                        label = "Zebra cells",
                        value = analysis.activeZebraCells.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (showHistogram) {
                    HistogramChart(
                        histogramCounts = analysis.histogramCounts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp),
                    )
                }
            }
            Text(
                text = "Tap the preview to hide or show controls. Zebra uses a coarse clipped-highlight grid so the live view stays responsive.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistogramMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun HistogramToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun HistogramChart(
    histogramCounts: List<Int>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (histogramCounts.isEmpty()) return@Canvas

        val maxCount = histogramCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
        val barWidth = size.width / histogramCounts.size.toFloat()

        histogramCounts.forEachIndexed { index, count ->
            val normalizedHeight = count.toFloat() / maxCount.toFloat()
            val height = normalizedHeight * size.height
            val left = index * barWidth

            drawRoundRect(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = Offset(left + barWidth * 0.1f, size.height - height),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, height),
                cornerRadius = CornerRadius(x = 6.dp.toPx(), y = 6.dp.toPx()),
            )
        }
    }
}

@Composable
private fun HistogramCameraAccessCard(
    cameraAvailable: Boolean,
    cameraError: String?,
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
                text = when {
                    cameraError != null -> "Camera preview unavailable"
                    cameraAvailable -> "Allow camera access"
                    else -> "Camera unavailable"
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = cameraError ?: if (cameraAvailable) {
                    "Use the back camera to monitor a live luminance histogram and clipped-highlight zebra overlay. Camera access is required first."
                } else {
                    "This device does not expose a usable back camera for the live histogram & zebra tool."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (cameraAvailable && cameraError == null) {
                FilledTonalButton(onClick = onRequestPermission) {
                    Text("Allow camera access")
                }
            }
        }
    }
}
