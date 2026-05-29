package com.janhorak.shutterdeck.gear.presentation

import com.janhorak.shutterdeck.core.data.db.GearItemEntity
import java.util.Locale

internal const val UNASSIGNED_GEAR_LABEL = "Unassigned"

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
