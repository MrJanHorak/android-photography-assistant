package com.janhorak.shutterdeck.gear.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class GearInsuranceExportItem(
    val category: String,
    val itemName: String,
    val serialNumber: String,
    val purchaseDateText: String,
    val purchaseSource: String,
    val storageLocation: String,
    val conditionLabel: String,
    val purchasePrice: Double?,
    val currentValue: Double?,
    val weightGrams: Double?,
    val hasReferencePhoto: Boolean,
    val notes: String,
)

data class GearInsuranceSummary(
    val itemCount: Int,
    val itemsWithSerialNumber: Int,
    val itemsMissingSerialNumber: Int,
    val itemsWithCurrentValue: Int,
    val itemsMissingCurrentValue: Int,
    val itemsWithPurchasePrice: Int,
    val itemsWithReferencePhoto: Int,
    val totalPurchaseValue: Double,
    val totalCurrentValue: Double,
)

data class GearInsuranceExportReport(
    val generatedAt: LocalDateTime,
    val summary: GearInsuranceSummary,
    val items: List<GearInsuranceExportItem>,
)

private val exportTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun buildGearInsuranceExportReport(
    items: List<GearInsuranceExportItem>,
    generatedAt: LocalDateTime = LocalDateTime.now(),
): GearInsuranceExportReport {
    val sortedItems = items.sortedWith(
        compareBy<GearInsuranceExportItem> { it.category.lowercase(Locale.US) }
            .thenBy { it.itemName.lowercase(Locale.US) }
            .thenBy { it.serialNumber.lowercase(Locale.US) },
    )
    return GearInsuranceExportReport(
        generatedAt = generatedAt,
        summary = GearInsuranceSummary(
            itemCount = sortedItems.size,
            itemsWithSerialNumber = sortedItems.count { it.serialNumber.isNotBlank() },
            itemsMissingSerialNumber = sortedItems.count { it.serialNumber.isBlank() },
            itemsWithCurrentValue = sortedItems.count { it.currentValue != null },
            itemsMissingCurrentValue = sortedItems.count { it.currentValue == null },
            itemsWithPurchasePrice = sortedItems.count { it.purchasePrice != null },
            itemsWithReferencePhoto = sortedItems.count { it.hasReferencePhoto },
            totalPurchaseValue = sortedItems.sumOf { it.purchasePrice ?: 0.0 },
            totalCurrentValue = sortedItems.sumOf { it.currentValue ?: 0.0 },
        ),
        items = sortedItems,
    )
}

fun buildGearInsuranceCsv(report: GearInsuranceExportReport): String {
    val header = listOf(
        "Category",
        "Item name",
        "Serial number",
        "Purchase date",
        "Purchase source",
        "Storage location",
        "Condition",
        "Purchase price",
        "Current value",
        "Weight grams",
        "Reference photo saved",
        "Notes",
    )
    val rows = report.items.map { item ->
        listOf(
            item.category,
            item.itemName,
            item.serialNumber,
            item.purchaseDateText,
            item.purchaseSource,
            item.storageLocation,
            item.conditionLabel,
            formatCsvDecimal(item.purchasePrice),
            formatCsvDecimal(item.currentValue),
            formatCsvDecimal(item.weightGrams),
            yesNo(item.hasReferencePhoto),
            item.notes,
        )
    }
    return buildString {
        append(header.joinToString(",") { csvEscape(it) })
        rows.forEach { row ->
            append('\n')
            append(row.joinToString(",") { csvEscape(it) })
        }
    }
}

fun buildGearInsurancePdfLines(report: GearInsuranceExportReport): List<String> = buildList {
    add("ShutterDeck insurance inventory report")
    add("Generated ${report.generatedAt.format(exportTimestampFormatter)}")
    add("")
    add("Summary")
    add("- Items: ${report.summary.itemCount}")
    add("- Serial numbers saved: ${report.summary.itemsWithSerialNumber}")
    add("- Missing serial numbers: ${report.summary.itemsMissingSerialNumber}")
    add("- Purchase prices saved: ${report.summary.itemsWithPurchasePrice}")
    add("- Current values saved: ${report.summary.itemsWithCurrentValue}")
    add("- Missing current values: ${report.summary.itemsMissingCurrentValue}")
    add("- Reference photos saved: ${report.summary.itemsWithReferencePhoto}")
    add("- Total purchase value: ${formatPdfDecimal(report.summary.totalPurchaseValue)}")
    add("- Total current value: ${formatPdfDecimal(report.summary.totalCurrentValue)}")
    add("")
    add("Monetary values are exported exactly as saved in ShutterDeck.")
    add("The app does not store a currency code yet.")
    add("")
    add("Inventory items")
    if (report.items.isEmpty()) {
        add("- No gear items saved yet.")
    } else {
        report.items.forEach { item ->
            add("- ${item.category}: ${item.itemName}")
            add("  Serial: ${item.serialNumber.ifBlank { "Not saved" }}")
            add("  Purchase date: ${item.purchaseDateText.ifBlank { "Not saved" }}")
            add("  Purchase source: ${item.purchaseSource.ifBlank { "Not saved" }}")
            add("  Storage location: ${item.storageLocation.ifBlank { "Not saved" }}")
            add("  Condition: ${item.conditionLabel.ifBlank { "Not saved" }}")
            add("  Purchase price: ${formatPdfDecimal(item.purchasePrice)}")
            add("  Current value: ${formatPdfDecimal(item.currentValue)}")
            add("  Weight: ${formatPdfWeight(item.weightGrams)}")
            add("  Reference photo saved: ${yesNo(item.hasReferencePhoto)}")
            add("  Notes: ${item.notes.normalizeForPdf()}")
            add("")
        }
    }
}

fun defaultGearInsuranceCsvFileName(date: LocalDate = LocalDate.now()): String =
    "shutterdeck-gear-insurance-${date}.csv"

fun defaultGearInsurancePdfFileName(date: LocalDate = LocalDate.now()): String =
    "shutterdeck-gear-insurance-${date}.pdf"

private fun csvEscape(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"$escaped\""
    } else {
        escaped
    }
}

private fun formatCsvDecimal(value: Double?): String =
    value?.let { String.format(Locale.US, "%.2f", it) }.orEmpty()

private fun formatPdfDecimal(value: Double?): String =
    value?.let { String.format(Locale.US, "%.2f", it) } ?: "Not saved"

private fun formatPdfWeight(value: Double?): String =
    value?.let { String.format(Locale.US, "%.0f g", it) } ?: "Not saved"

private fun yesNo(value: Boolean): String = if (value) "Yes" else "No"

private fun String.normalizeForPdf(): String =
    trim()
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lineSequence()
        .map(String::trim)
        .filter { it.isNotBlank() }
        .joinToString(" / ")
        .ifBlank { "Not saved" }
