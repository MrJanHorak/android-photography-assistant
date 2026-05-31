package com.janhorak.shutterdeck.utilities.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

enum class LightingSetupItemType(val defaultLabel: String) {
    CAMERA("Camera"),
    SUBJECT("Subject"),
    LIGHT("Light"),
    ;

    companion object {
        fun fromStoredValue(value: String): LightingSetupItemType =
            entries.firstOrNull { it.name == value } ?: LIGHT
    }
}

data class LightingSetupDraftItem(
    val localId: Long,
    val type: LightingSetupItemType,
    val label: String,
    val xFraction: Float,
    val yFraction: Float,
)

data class LightingSetupDiagram(
    val id: Long,
    val name: String,
    val notes: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val items: List<LightingSetupDraftItem>,
)

fun defaultLightingSetupDraftItems(): List<LightingSetupDraftItem> = listOf(
    LightingSetupDraftItem(
        localId = 1,
        type = LightingSetupItemType.CAMERA,
        label = "Camera",
        xFraction = 0.5f,
        yFraction = 0.86f,
    ),
    LightingSetupDraftItem(
        localId = 2,
        type = LightingSetupItemType.SUBJECT,
        label = "Subject",
        xFraction = 0.5f,
        yFraction = 0.5f,
    ),
    LightingSetupDraftItem(
        localId = 3,
        type = LightingSetupItemType.LIGHT,
        label = "Key",
        xFraction = 0.25f,
        yFraction = 0.24f,
    ),
    LightingSetupDraftItem(
        localId = 4,
        type = LightingSetupItemType.LIGHT,
        label = "Fill",
        xFraction = 0.76f,
        yFraction = 0.3f,
    ),
    LightingSetupDraftItem(
        localId = 5,
        type = LightingSetupItemType.LIGHT,
        label = "Back",
        xFraction = 0.5f,
        yFraction = 0.14f,
    ),
)

fun nextLightingSetupLightPosition(existingLightCount: Int): Pair<Float, Float> {
    val positions = listOf(
        0.15f to 0.7f,
        0.85f to 0.7f,
        0.16f to 0.12f,
        0.84f to 0.12f,
        0.5f to 0.74f,
    )
    val extraLightIndex = (existingLightCount - 3).coerceAtLeast(0)
    return positions[extraLightIndex % positions.size]
}

fun clampLightingSetupFraction(value: Float): Float = value.coerceIn(0f, 1f)

fun buildLightingSetupShareText(
    name: String,
    notes: String,
    items: List<LightingSetupDraftItem>,
    updatedAtMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val subject = items.firstOrNull { it.type == LightingSetupItemType.SUBJECT }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val updatedAt = Instant.ofEpochMilli(updatedAtMillis)
        .atZone(zoneId)
        .toLocalDateTime()
        .format(formatter)
    return buildString {
        appendLine("ShutterDeck Lighting Setup")
        appendLine(name.ifBlank { "Untitled setup" })
        appendLine("Updated $updatedAt")
        appendLine()
        items.forEach { item ->
            append("- ")
            append(item.label.ifBlank { item.type.defaultLabel })
            append(": ")
            append(describeLightingSetupItem(item, subject))
            appendLine()
        }
        if (notes.isNotBlank()) {
            appendLine()
            appendLine("Notes")
            append(notes.trim())
        }
    }
}

private fun describeLightingSetupItem(
    item: LightingSetupDraftItem,
    subject: LightingSetupDraftItem?,
): String {
    val positionDescription = when (item.type) {
        LightingSetupItemType.SUBJECT -> describeLightingSetupStagePosition(item)
        LightingSetupItemType.CAMERA -> describeLightingSetupStagePosition(item)
        LightingSetupItemType.LIGHT -> describeLightingSetupRelativePosition(item, subject)
    }
    val coordinates = String.format(
        Locale.US,
        "(%.0f%%, %.0f%%)",
        item.xFraction * 100f,
        item.yFraction * 100f,
    )
    return "$positionDescription $coordinates"
}

private fun describeLightingSetupStagePosition(item: LightingSetupDraftItem): String {
    val horizontal = axisDescriptor(
        value = item.xFraction,
        lower = "left edge",
        middle = "center",
        upper = "right edge",
    )
    val vertical = axisDescriptor(
        value = item.yFraction,
        lower = "top",
        middle = "middle",
        upper = "bottom",
    )
    return "$vertical, $horizontal"
}

private fun describeLightingSetupRelativePosition(
    item: LightingSetupDraftItem,
    subject: LightingSetupDraftItem?,
): String {
    if (subject == null) {
        return describeLightingSetupStagePosition(item)
    }
    val xDelta = item.xFraction - subject.xFraction
    val yDelta = item.yFraction - subject.yFraction
    val horizontal = when {
        abs(xDelta) < 0.08f -> "centered with subject"
        xDelta < 0f -> "left of subject"
        else -> "right of subject"
    }
    val vertical = when {
        abs(yDelta) < 0.08f -> "level with subject"
        yDelta < 0f -> "above subject"
        else -> "below subject"
    }
    return if (horizontal == "centered with subject" && vertical == "level with subject") {
        "stacked on subject"
    } else {
        "$vertical, $horizontal"
    }
}

private fun axisDescriptor(
    value: Float,
    lower: String,
    middle: String,
    upper: String,
): String = when {
    value < 0.33f -> lower
    value > 0.67f -> upper
    else -> middle
}
