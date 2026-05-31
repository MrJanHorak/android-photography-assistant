package com.janhorak.shutterdeck.utilities.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.calculators.presentation.CalculatorHint
import com.janhorak.shutterdeck.calculators.presentation.CalculatorScaffold
import com.janhorak.shutterdeck.calculators.presentation.ResultCard
import com.janhorak.shutterdeck.calculators.presentation.formatOneDecimal
import com.janhorak.shutterdeck.ui.components.LabeledField
import com.janhorak.shutterdeck.ui.components.ResultRow
import com.janhorak.shutterdeck.ui.components.SectionHeader
import com.janhorak.shutterdeck.utilities.domain.IntervalometerPlan
import com.janhorak.shutterdeck.utilities.domain.buildIntervalometerPlan
import java.util.Locale
import kotlin.math.abs

@Composable
fun IntervalometerPlannerScreen(modifier: Modifier = Modifier) {
    var intervalSecondsText by rememberSaveable { mutableStateOf("5") }
    var frameCountText by rememberSaveable { mutableStateOf("240") }
    var playbackFramesPerSecondText by rememberSaveable { mutableStateOf("24") }
    var averageFrameSizeMegabytesText by rememberSaveable { mutableStateOf("30") }
    var cardCapacityGigabytesText by rememberSaveable { mutableStateOf("64") }
    var shotsPerBatteryText by rememberSaveable { mutableStateOf("400") }
    var exposureSecondsText by rememberSaveable { mutableStateOf("") }

    val intervalSeconds = intervalSecondsText.toIntOrNull()
    val frameCount = frameCountText.toIntOrNull()
    val playbackFramesPerSecond = playbackFramesPerSecondText.toIntOrNull()
    val averageFrameSizeMegabytes = averageFrameSizeMegabytesText.toDoubleOrNull()
    val cardCapacityGigabytes = cardCapacityGigabytesText.toDoubleOrNull()
    val shotsPerBattery = shotsPerBatteryText.toIntOrNull()
    val exposureSeconds = exposureSecondsText.toDoubleOrNull()

    val validationMessage = remember(
        intervalSecondsText,
        frameCountText,
        playbackFramesPerSecondText,
        averageFrameSizeMegabytesText,
        cardCapacityGigabytesText,
        shotsPerBatteryText,
        exposureSecondsText,
        intervalSeconds,
        frameCount,
        playbackFramesPerSecond,
        averageFrameSizeMegabytes,
        cardCapacityGigabytes,
        shotsPerBattery,
        exposureSeconds,
    ) {
        buildValidationMessage(
            intervalSecondsText = intervalSecondsText,
            intervalSeconds = intervalSeconds,
            frameCountText = frameCountText,
            frameCount = frameCount,
            playbackFramesPerSecondText = playbackFramesPerSecondText,
            playbackFramesPerSecond = playbackFramesPerSecond,
            averageFrameSizeMegabytesText = averageFrameSizeMegabytesText,
            averageFrameSizeMegabytes = averageFrameSizeMegabytes,
            cardCapacityGigabytesText = cardCapacityGigabytesText,
            cardCapacityGigabytes = cardCapacityGigabytes,
            shotsPerBatteryText = shotsPerBatteryText,
            shotsPerBattery = shotsPerBattery,
            exposureSecondsText = exposureSecondsText,
            exposureSeconds = exposureSeconds,
        )
    }

    val plan = remember(
        intervalSeconds,
        frameCount,
        playbackFramesPerSecond,
        averageFrameSizeMegabytesText,
        averageFrameSizeMegabytes,
        cardCapacityGigabytesText,
        cardCapacityGigabytes,
        shotsPerBatteryText,
        shotsPerBattery,
        exposureSecondsText,
        exposureSeconds,
    ) {
        if (validationMessage != null) {
            null
        } else {
            buildIntervalometerPlan(
                intervalSeconds = intervalSeconds ?: 0,
                frameCount = frameCount ?: 0,
                playbackFramesPerSecond = playbackFramesPerSecond ?: 0,
                averageFrameSizeMegabytes = averageFrameSizeMegabytesText.takeIf { it.isNotBlank() }?.let { averageFrameSizeMegabytes },
                cardCapacityGigabytes = cardCapacityGigabytesText.takeIf { it.isNotBlank() }?.let { cardCapacityGigabytes },
                shotsPerBattery = shotsPerBatteryText.takeIf { it.isNotBlank() }?.let { shotsPerBattery },
                exposureSeconds = exposureSecondsText.takeIf { it.isNotBlank() }?.let { exposureSeconds },
            )
        }
    }

    CalculatorScaffold(modifier = modifier) {
        SectionHeader(
            title = "Intervalometer / time-lapse planner",
            subtitle = "Plan interval, frame count, final clip length, storage and battery coverage. Assumes frame 1 is taken at the start of the sequence.",
        )
        CalculatorHint("Camera triggering still depends on external hardware; this screen focuses on planning the sequence.")
        IntervalometerInputsCard(
            intervalSecondsText = intervalSecondsText,
            onIntervalSecondsChange = { intervalSecondsText = it },
            frameCountText = frameCountText,
            onFrameCountChange = { frameCountText = it },
            playbackFramesPerSecondText = playbackFramesPerSecondText,
            onPlaybackFramesPerSecondChange = { playbackFramesPerSecondText = it },
            exposureSecondsText = exposureSecondsText,
            onExposureSecondsChange = { exposureSecondsText = it },
        )
        SupportPlanningCard(
            averageFrameSizeMegabytesText = averageFrameSizeMegabytesText,
            onAverageFrameSizeMegabytesChange = { averageFrameSizeMegabytesText = it },
            cardCapacityGigabytesText = cardCapacityGigabytesText,
            onCardCapacityGigabytesChange = { cardCapacityGigabytesText = it },
            shotsPerBatteryText = shotsPerBatteryText,
            onShotsPerBatteryChange = { shotsPerBatteryText = it },
        )
        if (plan == null) {
            CalculatorHint(validationMessage ?: "Enter a positive interval, frame count and playback rate to build a plan.")
        } else {
            SequenceSummaryCard(
                plan = plan,
                playbackFramesPerSecond = playbackFramesPerSecond ?: 0,
            )
            CoverageSummaryCard(
                plan = plan,
                cardCapacityGigabytes = cardCapacityGigabytesText.takeIf { it.isNotBlank() }?.let { cardCapacityGigabytes },
                shotsPerBattery = shotsPerBatteryText.takeIf { it.isNotBlank() }?.let { shotsPerBattery },
            )
        }
    }
}

@Composable
private fun IntervalometerInputsCard(
    intervalSecondsText: String,
    onIntervalSecondsChange: (String) -> Unit,
    frameCountText: String,
    onFrameCountChange: (String) -> Unit,
    playbackFramesPerSecondText: String,
    onPlaybackFramesPerSecondChange: (String) -> Unit,
    exposureSecondsText: String,
    onExposureSecondsChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = "Sequence",
                subtitle = "The capture window runs from the first frame to the last frame in the sequence.",
            )
            LabeledField(
                label = "Interval between frames",
                value = intervalSecondsText,
                onValueChange = onIntervalSecondsChange,
                suffix = "s",
            )
            LabeledField(
                label = "Planned frame count",
                value = frameCountText,
                onValueChange = onFrameCountChange,
            )
            LabeledField(
                label = "Playback frame rate",
                value = playbackFramesPerSecondText,
                onValueChange = onPlaybackFramesPerSecondChange,
                suffix = "fps",
            )
            LabeledField(
                label = "Exposure time (optional)",
                value = exposureSecondsText,
                onValueChange = onExposureSecondsChange,
                suffix = "s",
                keyboardType = KeyboardType.Decimal,
            )
        }
    }
}

@Composable
private fun SupportPlanningCard(
    averageFrameSizeMegabytesText: String,
    onAverageFrameSizeMegabytesChange: (String) -> Unit,
    cardCapacityGigabytesText: String,
    onCardCapacityGigabytesChange: (String) -> Unit,
    shotsPerBatteryText: String,
    onShotsPerBatteryChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = "Coverage",
                subtitle = "Leave any of these blank if you only need timing math.",
            )
            LabeledField(
                label = "Average frame size",
                value = averageFrameSizeMegabytesText,
                onValueChange = onAverageFrameSizeMegabytesChange,
                suffix = "MB",
                keyboardType = KeyboardType.Decimal,
            )
            LabeledField(
                label = "Card capacity",
                value = cardCapacityGigabytesText,
                onValueChange = onCardCapacityGigabytesChange,
                suffix = "GB",
                keyboardType = KeyboardType.Decimal,
            )
            LabeledField(
                label = "Shots per battery",
                value = shotsPerBatteryText,
                onValueChange = onShotsPerBatteryChange,
            )
        }
    }
}

@Composable
private fun SequenceSummaryCard(
    plan: IntervalometerPlan,
    playbackFramesPerSecond: Int,
) {
    ResultCard {
        Text(
            text = "Sequence summary",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ResultRow(
            label = "Capture window",
            value = formatCaptureWindow(plan.captureWindowSeconds),
        )
        ResultRow(
            label = "Clip length @ ${playbackFramesPerSecond} fps",
            value = formatClipLength(plan.clipLengthSeconds),
        )
        ResultRow(
            label = "Frames per minute",
            value = formatOneDecimal(plan.framesPerMinute),
        )
        plan.exposureCheck?.let { exposureCheck ->
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ResultRow(
                label = "Exposure headroom",
                value = formatExposureHeadroom(exposureCheck.slackSeconds),
            )
            if (exposureCheck.overrunsInterval) {
                Text(
                    text = "Exposure is longer than the interval, so the sequence will drift unless you shorten the exposure or lengthen the interval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CoverageSummaryCard(
    plan: IntervalometerPlan,
    cardCapacityGigabytes: Double?,
    shotsPerBattery: Int?,
) {
    ResultCard {
        Text(
            text = "Coverage summary",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        plan.storageRequiredGigabytes?.let { storageRequiredGigabytes ->
            ResultRow(
                label = "Storage required",
                value = formatStorage(
                    storageRequiredGigabytes = storageRequiredGigabytes,
                    storageRequiredMegabytes = plan.storageRequiredMegabytes ?: 0.0,
                ),
            )
        }
        if (plan.cardsNeeded != null) {
            ResultRow(
                label = if (cardCapacityGigabytes != null) {
                    "${formatOneDecimal(cardCapacityGigabytes)} GB cards"
                } else {
                    "Cards needed"
                },
                value = formatCardCount(plan.cardsNeeded),
            )
        }
        if (plan.batteriesNeeded != null) {
            ResultRow(
                label = if (shotsPerBattery != null) {
                    "$shotsPerBattery shots per battery"
                } else {
                    "Batteries needed"
                },
                value = formatBatteryCount(plan.batteriesNeeded),
            )
        }
        if (plan.storageRequiredGigabytes == null && plan.batteriesNeeded == null) {
            Text(
                text = "Add average frame size, card capacity or battery life above to estimate how much support gear the sequence needs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildValidationMessage(
    intervalSecondsText: String,
    intervalSeconds: Int?,
    frameCountText: String,
    frameCount: Int?,
    playbackFramesPerSecondText: String,
    playbackFramesPerSecond: Int?,
    averageFrameSizeMegabytesText: String,
    averageFrameSizeMegabytes: Double?,
    cardCapacityGigabytesText: String,
    cardCapacityGigabytes: Double?,
    shotsPerBatteryText: String,
    shotsPerBattery: Int?,
    exposureSecondsText: String,
    exposureSeconds: Double?,
): String? = when {
    intervalSecondsText.isBlank() -> "Enter the interval between frames."
    intervalSeconds == null || intervalSeconds <= 0 -> "Interval must be greater than 0 seconds."
    frameCountText.isBlank() -> "Enter the number of planned frames."
    frameCount == null || frameCount <= 0 -> "Frame count must be greater than 0."
    playbackFramesPerSecondText.isBlank() -> "Enter the playback frame rate."
    playbackFramesPerSecond == null || playbackFramesPerSecond <= 0 -> "Playback frame rate must be greater than 0."
    averageFrameSizeMegabytesText.isNotBlank() &&
        (averageFrameSizeMegabytes == null || averageFrameSizeMegabytes <= 0.0) ->
        "Average frame size must be greater than 0 MB."
    cardCapacityGigabytesText.isNotBlank() &&
        (cardCapacityGigabytes == null || cardCapacityGigabytes <= 0.0) ->
        "Card capacity must be greater than 0 GB."
    shotsPerBatteryText.isNotBlank() &&
        (shotsPerBattery == null || shotsPerBattery <= 0) ->
        "Shots per battery must be greater than 0."
    exposureSecondsText.isNotBlank() &&
        (exposureSeconds == null || exposureSeconds <= 0.0) ->
        "Exposure time must be greater than 0 seconds."
    else -> null
}

private fun formatCaptureWindow(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%dh %02dm %02ds", hours, minutes, seconds)
        minutes > 0 -> String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds)
        else -> "$seconds s"
    }
}

private fun formatClipLength(seconds: Double): String = when {
    seconds >= 3600.0 -> String.format(Locale.getDefault(), "%.1f hr", seconds / 3600.0)
    seconds >= 60.0 -> String.format(Locale.getDefault(), "%.1f min", seconds / 60.0)
    else -> "${formatOneDecimal(seconds)} s"
}

private fun formatStorage(
    storageRequiredGigabytes: Double,
    storageRequiredMegabytes: Double,
): String = when {
    storageRequiredGigabytes >= 1.0 -> "${formatOneDecimal(storageRequiredGigabytes)} GB"
    else -> "${formatOneDecimal(storageRequiredMegabytes)} MB"
}

private fun formatExposureHeadroom(slackSeconds: Double): String = when {
    slackSeconds >= 0.0 -> "${formatOneDecimal(slackSeconds)} s spare"
    else -> "Over by ${formatOneDecimal(abs(slackSeconds))} s"
}

private fun formatCardCount(cardsNeeded: Int): String =
    if (cardsNeeded == 1) "1 card" else "$cardsNeeded cards"

private fun formatBatteryCount(batteriesNeeded: Int): String =
    if (batteriesNeeded == 1) "1 battery" else "$batteriesNeeded batteries"
