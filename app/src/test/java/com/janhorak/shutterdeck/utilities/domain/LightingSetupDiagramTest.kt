package com.janhorak.shutterdeck.utilities.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class LightingSetupDiagramTest {
    @Test
    fun `default draft includes camera subject and three lights`() {
        val items = defaultLightingSetupDraftItems()

        assertEquals(5, items.size)
        assertEquals(
            listOf(
                LightingSetupItemType.CAMERA,
                LightingSetupItemType.SUBJECT,
                LightingSetupItemType.LIGHT,
                LightingSetupItemType.LIGHT,
                LightingSetupItemType.LIGHT,
            ),
            items.map { it.type },
        )
        assertEquals(listOf("Camera", "Subject", "Key", "Fill", "Back"), items.map { it.label })
    }

    @Test
    fun `share text describes lights relative to the subject`() {
        val zoneId = ZoneId.of("Europe/Prague")
        val updatedAtMillis = LocalDateTime.of(2026, 5, 31, 12, 30)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val shareText = buildLightingSetupShareText(
            name = "Two-light portrait",
            notes = "Bounce fill into the ceiling.",
            items = defaultLightingSetupDraftItems(),
            updatedAtMillis = updatedAtMillis,
            zoneId = zoneId,
        )

        assertTrue(shareText.contains("ShutterDeck Lighting Setup"))
        assertTrue(shareText.contains("Two-light portrait"))
        assertTrue(shareText.contains("Updated 2026-05-31 12:30"))
        assertTrue(shareText.contains("Key: above subject, left of subject (25%, 24%)"))
        assertTrue(shareText.contains("Fill: above subject, right of subject (76%, 30%)"))
        assertTrue(shareText.contains("Notes\nBounce fill into the ceiling."))
    }

    @Test
    fun `next extra light positions cycle through stable slots`() {
        assertEquals(0.15f to 0.7f, nextLightingSetupLightPosition(existingLightCount = 3))
        assertEquals(0.85f to 0.7f, nextLightingSetupLightPosition(existingLightCount = 4))
        assertEquals(0.16f to 0.12f, nextLightingSetupLightPosition(existingLightCount = 5))
        assertEquals(0.15f to 0.7f, nextLightingSetupLightPosition(existingLightCount = 8))
    }
}
