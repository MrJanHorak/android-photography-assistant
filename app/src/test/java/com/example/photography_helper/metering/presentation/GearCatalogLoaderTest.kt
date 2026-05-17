package com.example.photography_helper.metering.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GearCatalogLoaderTest {
    @Test
    fun mergeCatalogs_replacesMatchingIdsAndAppendsNewEntries() {
        val baseBody = cameraBodyProfiles.first()
        val baseLens = lensProfiles.first()
        val bundledCatalog = GearCatalog(
            cameraBodyProfiles = listOf(baseBody),
            lensProfiles = listOf(baseLens),
            source = GearCatalogSource.BUNDLED,
        )
        val importedCatalog = GearCatalog(
            cameraBodyProfiles = listOf(
                baseBody.copy(label = "Updated generic digital body"),
                baseBody.copy(id = "custom_rangefinder", label = "Custom rangefinder"),
            ),
            lensProfiles = listOf(
                baseLens.copy(label = "Updated generic zoom"),
                baseLens.copy(id = "custom_prime", label = "Custom prime"),
            ),
            source = GearCatalogSource.IMPORTED,
        )

        val mergedCatalog = GearCatalogLoader.mergeCatalogs(bundledCatalog, importedCatalog)

        assertEquals(GearCatalogSource.IMPORTED, mergedCatalog.source)
        assertEquals(listOf("generic_digital", "custom_rangefinder"), mergedCatalog.cameraBodyProfiles.map { profile -> profile.id })
        assertEquals("Updated generic digital body", mergedCatalog.cameraBodyProfiles.first().label)
        assertEquals(listOf("generic_24_70_28", "custom_prime"), mergedCatalog.lensProfiles.map { profile -> profile.id })
        assertEquals("Updated generic zoom", mergedCatalog.lensProfiles.first().label)
    }

    @Test
    fun buildImportPreview_summarizesAddedAndOverriddenEntries() {
        val baseBody = cameraBodyProfiles.first()
        val baseLens = lensProfiles.first()
        val bundledCatalog = GearCatalog(
            cameraBodyProfiles = listOf(baseBody),
            lensProfiles = listOf(baseLens),
            source = GearCatalogSource.BUNDLED,
        )
        val importedCatalog = GearCatalog(
            cameraBodyProfiles = listOf(
                baseBody.copy(label = "Updated generic digital body"),
                baseBody.copy(id = "custom_rangefinder", label = "Custom rangefinder"),
            ),
            lensProfiles = listOf(
                baseLens.copy(label = "Updated generic zoom"),
                baseLens.copy(id = "custom_prime", label = "Custom prime"),
            ),
            source = GearCatalogSource.IMPORTED,
        )

        val preview = GearCatalogLoader.buildImportPreview(
            bundledCatalog = bundledCatalog,
            importedCatalog = importedCatalog,
            importedJson = "{}",
        )

        assertTrue(preview.hasChanges)
        assertEquals(listOf("Custom rangefinder"), preview.cameraBodyChanges.addedLabels)
        assertEquals(listOf("Updated generic digital body"), preview.cameraBodyChanges.overriddenLabels)
        assertEquals(listOf("Custom prime"), preview.lensChanges.addedLabels)
        assertEquals(listOf("Updated generic zoom"), preview.lensChanges.overriddenLabels)
        assertEquals(2, preview.mergedCatalog.cameraBodyProfiles.size)
        assertEquals(2, preview.mergedCatalog.lensProfiles.size)
    }
}