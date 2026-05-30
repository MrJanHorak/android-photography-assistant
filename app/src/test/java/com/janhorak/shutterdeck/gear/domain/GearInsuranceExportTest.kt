package com.janhorak.shutterdeck.gear.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GearInsuranceExportTest {

    private val sampleItems = listOf(
        GearInsuranceExportItem(
            category = "Lens",
            itemName = "RF 24-70mm F2.8",
            serialNumber = "L-002",
            purchaseDateText = "2024-04-10",
            purchaseSource = "Used market",
            storageLocation = "Dry cabinet",
            conditionLabel = "Good",
            purchasePrice = 1799.0,
            currentValue = 1500.0,
            weightGrams = 900.0,
            hasReferencePhoto = true,
            notes = "Main event zoom",
        ),
        GearInsuranceExportItem(
            category = "Body",
            itemName = "EOS R6",
            serialNumber = "",
            purchaseDateText = "2023-01-01",
            purchaseSource = "Local shop",
            storageLocation = "",
            conditionLabel = "Excellent",
            purchasePrice = 2499.0,
            currentValue = null,
            weightGrams = 680.0,
            hasReferencePhoto = false,
            notes = "Primary body",
        ),
    )

    @Test
    fun buildGearInsuranceExportReportSortsAndSummarizes() {
        val report = buildGearInsuranceExportReport(
            items = sampleItems,
            generatedAt = LocalDateTime.of(2026, 5, 30, 9, 15),
        )

        assertEquals(listOf("EOS R6", "RF 24-70mm F2.8"), report.items.map { it.itemName })
        assertEquals(2, report.summary.itemCount)
        assertEquals(1, report.summary.itemsMissingSerialNumber)
        assertEquals(1, report.summary.itemsMissingCurrentValue)
        assertEquals(4298.0, report.summary.totalPurchaseValue, 0.0001)
        assertEquals(1500.0, report.summary.totalCurrentValue, 0.0001)
    }

    @Test
    fun buildGearInsuranceCsvEscapesCommasQuotesAndNewlines() {
        val report = buildGearInsuranceExportReport(
            items = listOf(
                sampleItems.first().copy(
                    purchaseSource = "Shop, Inc.",
                    notes = "Quote \"safe\"\nLine 2",
                ),
            ),
            generatedAt = LocalDateTime.of(2026, 5, 30, 9, 15),
        )

        val csv = buildGearInsuranceCsv(report)

        assertTrue(csv.startsWith("Category,Item name,Serial number"))
        assertTrue(csv.contains("\"Shop, Inc.\""))
        assertTrue(csv.contains("\"Quote \"\"safe\"\"\nLine 2\""))
    }

    @Test
    fun buildGearInsurancePdfLinesIncludesSummaryAndNormalizedNotes() {
        val report = buildGearInsuranceExportReport(
            items = listOf(
                sampleItems.first().copy(notes = "First line\n\nSecond line")
            ),
            generatedAt = LocalDateTime.of(2026, 5, 30, 9, 15),
        )

        val lines = buildGearInsurancePdfLines(report)

        assertEquals("ShutterDeck insurance inventory report", lines.first())
        assertTrue(lines.contains("Generated 2026-05-30 09:15"))
        assertTrue(lines.contains("- Total current value: 1500.00"))
        assertTrue(lines.contains("  Notes: First line / Second line"))
        assertTrue(lines.contains("The app does not store a currency code yet."))
    }

    @Test
    fun defaultFileNamesUseCurrentDateShape() {
        val date = LocalDate.of(2026, 5, 30)

        assertEquals("shutterdeck-gear-insurance-2026-05-30.csv", defaultGearInsuranceCsvFileName(date))
        assertEquals("shutterdeck-gear-insurance-2026-05-30.pdf", defaultGearInsurancePdfFileName(date))
    }
}
