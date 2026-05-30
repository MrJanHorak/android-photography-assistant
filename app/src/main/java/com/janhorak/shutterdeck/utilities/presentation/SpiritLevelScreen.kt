package com.janhorak.shutterdeck.utilities.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.align
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janhorak.shutterdeck.ui.effects.KeepScreenOn
import com.janhorak.shutterdeck.ui.effects.LockPortraitOrientation
import com.janhorak.shutterdeck.utilities.domain.SPIRIT_LEVEL_DISPLAY_RANGE_DEGREES
import com.janhorak.shutterdeck.utilities.domain.SPIRIT_LEVEL_THRESHOLD_DEGREES
import com.janhorak.shutterdeck.utilities.domain.SpiritLevelReading
import java.util.Locale

@Composable
fun SpiritLevelScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpiritLevelViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onBack)

    val state by viewModel.state.collectAsStateWithLifecycle()
    val inspectionMode = LocalInspectionMode.current

    KeepScreenOn()
    LockPortraitOrientation()

    DisposableEffect(viewModel, inspectionMode) {
        if (!inspectionMode) {
            viewModel.startLeveling()
        }
        onDispose {
            if (!inspectionMode) {
                viewModel.stopLeveling()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                StatusPill(reading = state.reading)
            }

            Text(
                text = "Spirit Level",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Use the bubble plus pitch and roll readouts to level the camera. This tool stays awake while open.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.weight(1f))

            when {
                !state.sensorAvailable -> SensorStatusCard(
                    title = "Sensor unavailable",
                    message = "This device does not report gravity or accelerometer data for the spirit level.",
                )

                state.reading == null -> SensorStatusCard(
                    title = "Hold steady",
                    message = "Reading the gravity sensor and stabilizing the level bubble.",
                )

                else -> {
                    SpiritLevelDial(
                        reading = state.reading!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(maxWidth = 360.dp)
                            .aspectRatio(1f)
                            .align(Alignment.CenterHorizontally),
                    )

                    if (state.reading!!.tooSteep) {
                        Text(
                            text = "Tilt back toward flat to resume leveling.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }

                    SpiritLevelReadout(reading = state.reading!!)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusPill(
    reading: SpiritLevelReading?,
) {
    val (label, containerColor, contentColor) = when {
        reading == null -> Triple(
            "Starting",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        reading.tooSteep -> Triple(
            "Recenter",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        reading.isLevel -> Triple(
            "Level",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        else -> Triple(
            "Adjust",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SensorStatusCard(
    title: String,
    message: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SpiritLevelDial(
    reading: SpiritLevelReading,
    modifier: Modifier = Modifier,
) {
    val ringColor = MaterialTheme.colorScheme.outline
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val bubbleColor = if (reading.isLevel) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val levelGlowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val ringStroke = 6.dp.toPx()
        val guideStroke = 2.dp.toPx()
        val bubbleRadius = radius * 0.14f
        val bubbleTravel = radius - bubbleRadius - 18.dp.toPx()
        val thresholdRatio =
            (SPIRIT_LEVEL_THRESHOLD_DEGREES / SPIRIT_LEVEL_DISPLAY_RANGE_DEGREES).toFloat()
        val targetRadius = bubbleTravel * thresholdRatio

        drawCircle(
            color = guideColor,
            radius = radius,
            style = Stroke(width = ringStroke),
        )
        drawCircle(
            color = guideColor,
            radius = targetRadius + bubbleRadius,
            style = Stroke(width = guideStroke),
        )
        drawLine(
            color = guideColor,
            start = Offset(center.x - bubbleTravel, center.y),
            end = Offset(center.x + bubbleTravel, center.y),
            strokeWidth = guideStroke,
        )
        drawLine(
            color = guideColor,
            start = Offset(center.x, center.y - bubbleTravel),
            end = Offset(center.x, center.y + bubbleTravel),
            strokeWidth = guideStroke,
        )

        val bubbleCenter = Offset(
            x = center.x + (reading.rollOffset * bubbleTravel),
            y = center.y - (reading.pitchOffset * bubbleTravel),
        )

        drawCircle(
            color = bubbleColor.copy(alpha = 0.24f),
            radius = bubbleRadius * 1.8f,
            center = bubbleCenter,
        )
        drawCircle(
            color = bubbleColor,
            radius = bubbleRadius,
            center = bubbleCenter,
        )
        if (reading.isLevel) {
            drawCircle(
                color = levelGlowColor,
                radius = radius * 0.82f,
            )
        }
    }
}

@Composable
private fun SpiritLevelReadout(
    reading: SpiritLevelReading,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    label = "Pitch",
                    value = formatDegrees(reading.pitchDegrees),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Roll",
                    value = formatDegrees(reading.rollDegrees),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = if (reading.isLevel) {
                    "Within ${SPIRIT_LEVEL_THRESHOLD_DEGREES.toInt()}° of level."
                } else {
                    "Center the bubble to bring both axes within ${SPIRIT_LEVEL_THRESHOLD_DEGREES.toInt()}°."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

private fun formatDegrees(value: Double): String = String.format(Locale.US, "%.1f°", value)
