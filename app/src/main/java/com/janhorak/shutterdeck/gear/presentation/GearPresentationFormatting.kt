package com.janhorak.shutterdeck.gear.presentation

import com.janhorak.shutterdeck.core.data.db.GearItemEntity
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

internal const val UNASSIGNED_GEAR_LABEL = "Unassigned"

internal val gearConditionOptions = listOf(
    "Excellent",
    "Good",
    "Fair",
    "Needs service",
    "Retired",
)
internal val filterTypeOptions = listOf(
    "ND",
    "CPL",
    "UV / protection",
    "Mist / diffusion",
    "Color / creative",
    "IR / specialty",
    "Other",
)
internal val batteryStatusOptions = listOf("Ready", "Charging", "Needs charge", "Retired")
internal val memoryCardStatusOptions = listOf("Empty", "In use", "Full", "Backed up", "Needs format")
internal val memoryCardTypeOptions = listOf("SD", "microSD", "CFexpress A", "CFexpress B", "CFast", "XQD", "Other")

internal fun gearDisplayName(item: GearItemEntity): String =
    listOf(item.brand.trim(), item.model.trim())
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { item.model }

internal fun formatMoney(value: Double): String = String.format(Locale.US, "$%.2f", value)

internal fun formatWeight(grams: Double): String = when {
    grams >= 1000.0 -> String.format(Locale.US, "%.2f kg", grams / 1000.0)
    else -> String.format(Locale.US, "%.0f g", grams)
}

internal fun formatPercent(value: Int): String = String.format(Locale.US, "%d%%", value)

internal fun formatBatteryCapacityMah(value: Long): String = String.format(Locale.US, "%,d mAh", value)

internal fun formatStorageCapacityGb(value: Long): String = when {
    value >= 1024L && value % 1024L == 0L -> String.format(Locale.US, "%,d TB", value / 1024L)
    value >= 1024L -> String.format(Locale.US, "%.1f TB", value / 1024.0)
    else -> String.format(Locale.US, "%,d GB", value)
}

internal fun normalizeThreadSize(text: String): String? {
    val trimmed = text.trim().lowercase(Locale.US)
    if (trimmed.isBlank()) return null

    val compact = trimmed.replace(" ", "")
    val numericKey = Regex("""\d+""").find(compact)?.value
    return numericKey ?: compact
}

internal fun formatThreadSizeText(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return ""

    val normalized = normalizeThreadSize(trimmed)
    return when {
        normalized == null -> ""
        trimmed.any { it.isLetter() } -> trimmed
        normalized.all { it.isDigit() } -> "$normalized mm"
        else -> trimmed
    }
}

internal fun referencePhotoLabel(uriString: String): String {
    val trimmed = uriString.trim()
    if (trimmed.isBlank()) return ""

    val encodedLabel = trimmed
        .substringBefore('?')
        .substringAfterLast('/')
        .ifBlank { trimmed }

    return URLDecoder.decode(encodedLabel, StandardCharsets.UTF_8)
}
